package com.fortytwotalents.agentic.orchestrator;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

/**
 * Stand-in for Loki / Prometheus / Argo CD. The incident data is canned so the demo is
 * reproducible: payment-gateway v2.14.0 shrank its DB connection pool from 20 to 5.
 * inventory-service has a harmless red herring deployment 50 minutes earlier.
 */
@Component
public class ObservabilityBackend {

	public record LogLine(Instant timestamp, String service, String level, String message) {
	}

	public record MetricSample(Instant timestamp, double value) {
	}

	public record Deployment(Instant timestamp, String service, String version, String author, String changelog) {
	}

	private static Instant t(String hhmm) {
		return Instant.parse("2026-10-27T" + hhmm + ":00Z");
	}

	private final List<Deployment> deployments = List.of(
			new Deployment(t("09:30"), "checkout-service", "5.0.3", "team-checkout", "Fix rounding of Swiss VAT"),
			new Deployment(t("13:15"), "inventory-service", "3.2.1", "team-stock", "Increase product cache TTL to 10m"),
			new Deployment(t("14:02"), "payment-gateway", "2.14.0", "team-payments",
					"Cost saving: spring.datasource.hikari.maximum-pool-size 20 -> 5 to reduce RDS connections"));

	private final List<LogLine> logs = List.of(
			new LogLine(t("13:16"), "inventory-service", "WARN", "Cache warm-up: miss ratio 0.41 after deployment"),
			new LogLine(t("13:20"), "inventory-service", "INFO", "Cache miss ratio back to 0.03"),
			new LogLine(t("14:02"), "payment-gateway", "INFO", "Started PaymentGatewayApplication v2.14.0"),
			new LogLine(t("14:04"), "payment-gateway", "WARN",
					"HikariPool-1 - Pool stats (total=5, active=5, idle=0, waiting=37)"),
			new LogLine(t("14:05"), "payment-gateway", "ERROR",
					"HikariPool-1 - Connection is not available, request timed out after 30000ms"),
			new LogLine(t("14:05"), "payment-gateway", "ERROR",
					"Authorisation failed for order 88123: CannotGetJdbcConnectionException"),
			new LogLine(t("14:05"), "checkout-service", "ERROR",
					"POST /checkout 502: upstream payment-gateway timed out after 30s"),
			new LogLine(t("14:07"), "checkout-service", "ERROR", "Circuit breaker 'payment' OPEN, failure rate 61%"),
			new LogLine(t("14:31"), "payment-gateway", "INFO", "Started PaymentGatewayApplication v2.13.4 (rollback)"),
			new LogLine(t("14:33"), "checkout-service", "INFO", "Circuit breaker 'payment' CLOSED"));

	public List<Deployment> deployments(String service) {
		return deployments.stream()
			.filter(d -> service == null || service.isBlank() || d.service().equalsIgnoreCase(service))
			.sorted(Comparator.comparing(Deployment::timestamp))
			.toList();
	}

	public List<LogLine> logs(String service, String minLevel) {
		int threshold = severity(minLevel);
		return logs.stream()
			.filter(l -> l.service().equalsIgnoreCase(service) && severity(l.level()) >= threshold)
			.toList();
	}

	public List<String> metricNames(String service) {
		return switch (service.toLowerCase(Locale.ROOT)) {
			case "payment-gateway" -> List.of("http.p99.ms", "db.pool.active", "db.pool.pending", "error.rate");
			case "checkout-service" -> List.of("http.p99.ms", "http.5xx.rate", "orders.per.minute");
			case "inventory-service" -> List.of("http.p99.ms", "cpu.utilisation");
			default -> List.of();
		};
	}

	public List<MetricSample> metric(String service, String metric) {
		double[] values = switch (service.toLowerCase(Locale.ROOT) + "/" + metric) {
			case "payment-gateway/http.p99.ms" -> new double[] { 180, 190, 175, 30000, 30000, 30000, 210 };
			case "payment-gateway/db.pool.active" -> new double[] { 6, 7, 5, 5, 5, 5, 8 };
			case "payment-gateway/db.pool.pending" -> new double[] { 0, 0, 0, 37, 52, 49, 0 };
			case "payment-gateway/error.rate" -> new double[] { 0.001, 0.001, 0.002, 0.58, 0.61, 0.6, 0.002 };
			case "checkout-service/http.p99.ms" -> new double[] { 420, 410, 430, 30100, 30050, 30000, 450 };
			case "checkout-service/http.5xx.rate" -> new double[] { 0.002, 0.002, 0.003, 0.38, 0.41, 0.4, 0.002 };
			case "checkout-service/orders.per.minute" -> new double[] { 310, 305, 298, 120, 95, 101, 302 };
			case "inventory-service/http.p99.ms" -> new double[] { 95, 140, 90, 92, 94, 91, 93 };
			case "inventory-service/cpu.utilisation" -> new double[] { 0.31, 0.44, 0.3, 0.29, 0.3, 0.31, 0.3 };
			default -> new double[0];
		};
		List<Instant> times = List.of(t("13:00"), t("13:20"), t("14:00"), t("14:05"), t("14:15"), t("14:25"),
				t("14:40"));
		return IntStream.range(0, values.length).mapToObj(i -> new MetricSample(times.get(i), values[i])).toList();
	}

	private static int severity(String level) {
		return switch (level == null ? "" : level.toUpperCase(Locale.ROOT)) {
			case "ERROR" -> 3;
			case "WARN" -> 2;
			case "INFO" -> 1;
			default -> 0;
		};
	}

}

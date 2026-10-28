package com.fortytwotalents.agentic.orchestrator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityBackendTests {

	private final ObservabilityBackend backend = new ObservabilityBackend();

	@Test
	void deploymentsAreChronologicalAndFilterable() {
		assertThat(backend.deployments(null)).extracting(ObservabilityBackend.Deployment::service)
			.containsExactly("checkout-service", "inventory-service", "payment-gateway");
		assertThat(backend.deployments("payment-gateway")).singleElement()
			.satisfies(d -> assertThat(d.changelog()).contains("20 -> 5"));
	}

	@Test
	void logsContainTheRootCauseOnlyAtErrorLevel() {
		assertThat(backend.logs("payment-gateway", "ERROR"))
			.anySatisfy(line -> assertThat(line.message()).contains("Connection is not available"));
		assertThat(backend.logs("inventory-service", "ERROR")).isEmpty();
	}

	@Test
	void metricsShowThePoolSaturation() {
		assertThat(backend.metricNames("payment-gateway")).contains("db.pool.pending");
		assertThat(backend.metric("payment-gateway", "db.pool.pending")).hasSize(7)
			.anySatisfy(sample -> assertThat(sample.value()).isGreaterThan(30));
		assertThat(backend.metric("payment-gateway", "does.not.exist")).isEmpty();
	}

}

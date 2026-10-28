package com.fortytwotalents.agentic.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LLM picks one branch; your code owns the branches, the threshold and the fallback.
 *
 * @param <I> request
 * @param <O> response of every branch
 */
public final class RoutingWorkflow<I, O> {

	private static final Logger log = LoggerFactory.getLogger(RoutingWorkflow.class);

	/** The router only sees name + description: that is the API of a route. */
	public record Route<I, O>(String name, String description, Function<I, O> handler) {
	}

	public record RoutingDecision(@JsonPropertyDescription("Name of the selected route") String route,
			@JsonPropertyDescription("One sentence: why this route. Free audit trail.") String reason,
			@JsonPropertyDescription("0.0 to 1.0") double confidence) {
	}

	public record Routed<O>(RoutingDecision decision, String handledBy, boolean fallback, O output) {
	}

	private final Map<String, Route<I, O>> routes = new LinkedHashMap<>();

	private final BiFunction<I, List<Route<I, O>>, RoutingDecision> selector;

	private final Route<I, O> fallback;

	private final double minConfidence;

	public RoutingWorkflow(List<Route<I, O>> routes, BiFunction<I, List<Route<I, O>>, RoutingDecision> selector,
			Route<I, O> fallback, double minConfidence) {
		routes.forEach(route -> this.routes.put(route.name(), route));
		this.selector = selector;
		this.fallback = fallback;
		this.minConfidence = minConfidence;
	}

	public Routed<O> route(I request) {
		RoutingDecision decision = selector.apply(request, List.copyOf(routes.values()));
		Route<I, O> selected = routes.get(decision.route());
		boolean useFallback = selected == null || decision.confidence() < minConfidence;
		Route<I, O> route = useFallback ? fallback : selected;
		log.info("[routing] {} (confidence {}) -> {}: {}", decision.route(), decision.confidence(), route.name(),
				decision.reason());
		return new Routed<>(decision, route.name(), useFallback, route.handler().apply(request));
	}

}

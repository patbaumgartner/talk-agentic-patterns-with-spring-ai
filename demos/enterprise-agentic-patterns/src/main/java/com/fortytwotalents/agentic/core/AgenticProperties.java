package com.fortytwotalents.agentic.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * "Right model per job" and "bounded everything" as configuration, not as magic numbers
 * scattered through the agents.
 *
 * @param temperatureSupported whether the provider's models accept a sampling temperature;
 * a provider profile sets {@code agentic.temperature-supported=false} to omit it
 */
@ConfigurationProperties("agentic")
public record AgenticProperties(Models models, Limits limits, @DefaultValue("true") boolean temperatureSupported) {

	/**
	 * @param fast cheap, non-reasoning model for Task Execution workers and routers
	 * @param smart stronger model for planning, orchestration and writing
	 * @param judge evaluator model, deliberately different from the generator
	 */
	public record Models(String fast, String smart, String judge) {
	}

	/**
	 * @param maxTokens output cap per call
	 * @param maxParallelCalls bounded fan-out against provider rate limits
	 * @param maxEvaluationRounds evaluator-optimizer loop cap
	 * @param maxHandoffs swarm handoff cap
	 */
	public record Limits(int maxTokens, int maxParallelCalls, int maxEvaluationRounds, int maxHandoffs) {
	}

}

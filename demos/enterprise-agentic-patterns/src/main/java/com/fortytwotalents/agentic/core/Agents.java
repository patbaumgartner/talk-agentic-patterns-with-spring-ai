package com.fortytwotalents.agentic.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Hands out pre-configured {@link ChatClient.Builder}s per model tier.
 *
 * Builders come from Spring AI's auto-configured prototype bean, so every agent gets
 * Micrometer observations (one span per call) for free.
 */
@Component
public class Agents {

	private final ObjectProvider<ChatClient.Builder> builders;

	private final AgenticProperties properties;

	public Agents(ObjectProvider<ChatClient.Builder> builders, AgenticProperties properties) {
		this.builders = builders;
		this.properties = properties;
	}

	/** Boring on purpose: fast model, temperature 0 where the provider supports it. */
	public ChatClient.Builder fast() {
		return builder(properties.models().fast(), 0.0);
	}

	/** Planning, orchestration and writing. */
	public ChatClient.Builder smart() {
		return builder(properties.models().smart(), 0.3);
	}

	/** A different model than the one being judged, deterministic verdicts where supported. */
	public ChatClient.Builder judge() {
		return builder(properties.models().judge(), 0.0);
	}

	/** The temperature is omitted when the provider profile says its models reject it. */
	public ChatClient.Builder builder(String model, double temperature) {
		var options = ToolCallingChatOptions.builder().model(model).maxTokens(properties.limits().maxTokens());
		if (properties.temperatureSupported()) {
			options.temperature(temperature);
		}
		return builders.getObject().defaultOptions(options).defaultAdvisors(new SimpleLoggerAdvisor());
	}

	public AgenticProperties.Limits limits() {
		return properties.limits();
	}

}

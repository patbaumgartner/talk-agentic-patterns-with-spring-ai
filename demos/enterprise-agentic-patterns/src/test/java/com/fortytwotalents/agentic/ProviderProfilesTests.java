package com.fortytwotalents.agentic;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.fortytwotalents.agentic.core.AgenticProperties;
import com.fortytwotalents.agentic.core.Agents;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderProfilesTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withInitializer(new ConfigDataApplicationContextInitializer())
		.withUserConfiguration(ProviderConfiguration.class);

	static Stream<Arguments> providers() {
		return Stream.of(
				Arguments.of("bedrock", "BedrockProxyChatModel", "eu.anthropic.claude-haiku-5-5",
						"eu.anthropic.claude-opus-5-5"),
				Arguments.of("openai", "OpenAiChatModel", "gpt-4.1-mini", "gpt-4.1"),
				Arguments.of("anthropic", "AnthropicChatModel", "claude-haiku-4-5-20251001", "claude-sonnet-4-6"),
				Arguments.of("ollama", "OllamaChatModel", "qwen3:8b", "qwen3:14b"));
	}

	@ParameterizedTest
	@MethodSource("providers")
	void selectsOnlyTheRequestedProvider(String profile, String modelClass, String fast, String smart) {
		providerRunner(profile).run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(ChatModel.class).hasSingleBean(Agents.class);
			ChatModel model = context.getBean(ChatModel.class);
			assertThat(model.getClass().getSimpleName()).isEqualTo(modelClass);
			assertThat(model.getDefaultOptions().getModel()).isEqualTo(smart);
			assertThat(context.getBean(AgenticProperties.class).models())
				.isEqualTo(new AgenticProperties.Models(fast, smart, fast));
			if (!"bedrock".equals(profile)) {
				assertThat(context).doesNotHaveBean("awsCredentialsProvider");
			}
		});
	}

	@Test
	void defaultsToBedrock() {
		runner
			.withPropertyValues("spring.ai.bedrock.aws.access-key=test-access-key",
					"spring.ai.bedrock.aws.secret-key=test-secret-key")
			.run(context -> {
				assertThat(context).hasNotFailed().hasSingleBean(ChatModel.class);
				assertThat(context.getBean(ChatModel.class)).isInstanceOf(BedrockProxyChatModel.class);
			});
	}

	@ParameterizedTest
	@MethodSource("providers")
	void allowsEveryModelTierToBeOverridden(String profile, String modelClass, String fast, String smart) {
		providerRunner(profile)
			.withPropertyValues("AGENTIC_MODEL_FAST=custom-fast", "AGENTIC_MODEL_SMART=custom-smart",
					"AGENTIC_MODEL_JUDGE=custom-judge")
			.run(context -> {
				assertThat(context).hasNotFailed().hasSingleBean(ChatModel.class);
				assertThat(context.getBean(AgenticProperties.class).models())
					.isEqualTo(new AgenticProperties.Models("custom-fast", "custom-smart", "custom-judge"));
				assertThat(context.getBean(ChatModel.class).getDefaultOptions().getModel()).isEqualTo("custom-smart");
			});
	}

	static Stream<Arguments> httpProviders() {
		return Stream.of(Arguments.of("openai", "/v1/chat/completions", """
				{"id":"chat-test","object":"chat.completion","created":0,"model":"test",
				 "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],
				 "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
				"""), Arguments.of("anthropic", "/v1/messages", """
				{"id":"msg-test","type":"message","role":"assistant","model":"test",
				 "content":[{"type":"text","text":"ok"}],"stop_reason":"end_turn","stop_sequence":null,
				 "usage":{"input_tokens":1,"output_tokens":1}}
				"""), Arguments.of("ollama", "/api/chat", """
				{"model":"test","created_at":"2026-10-27T06:00:00Z",
				 "message":{"role":"assistant","content":"ok"},"done":true,"done_reason":"stop",
				 "prompt_eval_count":1,"eval_count":1}
				"""));
	}

	@ParameterizedTest
	@MethodSource("httpProviders")
	void sendsModelTiersAndToolOptionsToTheProvider(String profile, String path, String response) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		List<JsonNode> requests = new ArrayList<>();
		JsonMapper json = JsonMapper.builder().build();
		server.createContext(path, exchange -> {
			try (exchange) {
				requests.add(json.readTree(exchange.getRequestBody()));
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				byte[] body = response.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		server.start();
		try {
			String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
			providerRunner(profile)
				.withPropertyValues(
						"openai".equals(profile) ? "spring.ai.openai.base-url=" + baseUrl + "/v1"
								: profile.toUpperCase() + "_BASE_URL=" + baseUrl,
						"AGENTIC_MODEL_FAST=custom-fast", "AGENTIC_MODEL_SMART=custom-smart",
						"AGENTIC_MODEL_JUDGE=custom-judge")
				.run(context -> {
					assertThat(context).hasNotFailed();
					Agents agents = context.getBean(Agents.class);
					assertThat(agents.fast().build().prompt().user("test").call().content()).isEqualTo("ok");
					assertThat(agents.smart().build().prompt().user("test").call().content()).isEqualTo("ok");
					assertThat(agents.judge().build().prompt().user("test").call().content()).isEqualTo("ok");
					assertThat(agents.fast().build().prompt().user("test").tools(new DemoTools()).call().content())
						.isEqualTo("ok");
				});
			assertThat(requests).hasSize(4);
			assertThat(requests.stream().map(request -> request.path("model").asText())).containsExactly("custom-fast",
					"custom-smart", "custom-judge", "custom-fast");
			String temperature = "ollama".equals(profile) ? "/options/temperature" : "/temperature";
			assertThat(requests.stream().map(request -> request.at(temperature)))
				.allSatisfy(node -> assertThat(node.isNumber()).isTrue());
			assertThat(requests.get(0).at(temperature).asDouble()).isZero();
			assertThat(requests.get(1).at(temperature).asDouble()).isEqualTo(0.3);
			assertThat(requests.get(2).at(temperature).asDouble()).isZero();
			assertThat(requests.get(0).at("ollama".equals(profile) ? "/options/num_predict" : "/max_tokens").asInt())
				.isEqualTo(4096);
			assertThat(requests.get(3).path("tools").size()).isEqualTo(1);
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void omitsTemperatureFromEveryBedrockTier() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		List<String> paths = new ArrayList<>();
		List<JsonNode> requests = new ArrayList<>();
		JsonMapper json = JsonMapper.builder().build();
		server.createContext("/model/", exchange -> {
			try (exchange) {
				paths.add(exchange.getRequestURI().getPath());
				requests.add(json.readTree(exchange.getRequestBody()));
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				byte[] body = """
						{"output":{"message":{"role":"assistant","content":[{"text":"ok"}]}},"stopReason":"end_turn",
						 "usage":{"inputTokens":1,"outputTokens":1,"totalTokens":2},"metrics":{"latencyMs":1}}
						""".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		server.start();
		try {
			URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
			providerRunner("bedrock")
				.withBean(BedrockRuntimeClient.class,
						() -> BedrockRuntimeClient.builder()
							.endpointOverride(endpoint)
							.region(Region.EU_CENTRAL_1)
							.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
							.build())
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(AgenticProperties.class).temperatureSupported()).isFalse();
					Agents agents = context.getBean(Agents.class);
					assertThat(agents.fast().build().prompt().user("test").call().content()).isEqualTo("ok");
					assertThat(agents.smart().build().prompt().user("test").call().content()).isEqualTo("ok");
					assertThat(agents.judge().build().prompt().user("test").call().content()).isEqualTo("ok");
				});
			assertThat(paths).containsExactly("/model/eu.anthropic.claude-haiku-5-5/converse",
					"/model/eu.anthropic.claude-opus-5-5/converse", "/model/eu.anthropic.claude-haiku-5-5/converse");
			assertThat(requests).allSatisfy(request -> {
				assertThat(request.at("/inferenceConfig/maxTokens").asInt()).isEqualTo(4096);
				assertThat(request.path("inferenceConfig").has("temperature")).isFalse();
			});
		}
		finally {
			server.stop(0);
		}
	}

	private ApplicationContextRunner providerRunner(String profile) {
		ApplicationContextRunner configured = runner.withPropertyValues("spring.profiles.active=" + profile);
		return switch (profile) {
			case "bedrock" -> configured.withPropertyValues("spring.ai.bedrock.aws.access-key=test-access-key",
					"spring.ai.bedrock.aws.secret-key=test-secret-key");
			case "openai" -> configured.withPropertyValues("OPENAI_API_KEY=test-openai-key");
			case "anthropic" -> configured.withPropertyValues("ANTHROPIC_API_KEY=test-anthropic-key");
			// Tests never pull a model or depend on a developer's local Ollama.
			case "ollama" -> configured.withPropertyValues("spring.ai.ollama.init.pull-model-strategy=never");
			default -> configured;
		};
	}

	static class DemoTools {

		@Tool(description = "Return the demo status")
		String status() {
			return "ready";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableConfigurationProperties(AgenticProperties.class)
	@Import(Agents.class)
	static class ProviderConfiguration {

	}

}

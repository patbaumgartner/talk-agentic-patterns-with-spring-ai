package com.fortytwotalents.agentic.core;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * The atom of every pattern: one prompt template in, one typed record out.
 *
 * It is a {@link Function}, so it composes: chain it, fan it out, wrap it as a tool.
 *
 * @param <I> request type, rendered into the template via {@code params}
 * @param <O> response type, the JSON schema the model must satisfy
 */
public final class TaskExecutionAgent<I, O> implements Function<I, O> {

	private static final Logger log = LoggerFactory.getLogger(TaskExecutionAgent.class);

	private final String name;

	private final ChatClient chatClient;

	private final Resource promptTemplate;

	private final Function<I, Map<String, Object>> params;

	private final Class<O> responseType;

	private TaskExecutionAgent(Builder<I, O> builder) {
		this.name = Objects.requireNonNull(builder.name, "name");
		this.chatClient = Objects.requireNonNull(builder.chatClient, "chatClient");
		this.promptTemplate = Objects.requireNonNull(builder.promptTemplate, "promptTemplate");
		this.params = Objects.requireNonNull(builder.params, "params");
		this.responseType = Objects.requireNonNull(builder.responseType, "responseType");
	}

	@Override
	public O apply(I request) {
		log.debug("[{}] executing", name);
		O response = chatClient.prompt()
			.user(user -> user.text(promptTemplate).params(params.apply(request)))
			.advisors(StructuredOutputValidationAdvisor.builder()
				.outputType(responseType)
				.maxRepeatAttempts(2) // re-prompt with the schema violation
				.build())
			.call()
			.entity(responseType);
		log.debug("[{}] done", name);
		return response;
	}

	public String name() {
		return name;
	}

	public static <I, O> Builder<I, O> builder(String name) {
		return new Builder<I, O>().name(name);
	}

	public static final class Builder<I, O> {

		private String name;

		private ChatClient chatClient;

		private Resource promptTemplate;

		private Function<I, Map<String, Object>> params;

		private Class<O> responseType;

		public Builder<I, O> name(String name) {
			this.name = name;
			return this;
		}

		public Builder<I, O> chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		/** Classpath location, e.g. {@code prompts/extract-invoice.st}. */
		public Builder<I, O> promptTemplate(String classpathLocation) {
			this.promptTemplate = new ClassPathResource(classpathLocation);
			return this;
		}

		public Builder<I, O> params(Function<I, Map<String, Object>> params) {
			this.params = params;
			return this;
		}

		public Builder<I, O> responseType(Class<O> responseType) {
			this.responseType = responseType;
			return this;
		}

		public TaskExecutionAgent<I, O> build() {
			return new TaskExecutionAgent<>(this);
		}

	}

}

package com.fortytwotalents.agentic.core;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders typed records into prompt text. Upstream results travel between agents as JSON,
 * never as free-form prose.
 */
public final class Json {

	private static final JsonMapper MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

	private Json() {
	}

	public static String write(Object value) {
		return MAPPER.writeValueAsString(value);
	}

}

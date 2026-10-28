package com.fortytwotalents.agentic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring AI renders prompts with StringTemplate and {@code {}} delimiters. A stray brace
 * (say, a JSON example in a prompt) fails at runtime, in front of the audience. Catch it
 * here instead.
 */
class PromptTemplateTests {

	@Test
	void promptTemplatesContainOnlyPlaceholderBraces() throws Exception {
		List<Path> templates;
		try (Stream<Path> files = Files.walk(Path.of("src/main/resources/prompts"))) {
			templates = files.filter(path -> path.toString().endsWith(".st")).toList();
		}

		assertThat(templates).hasSizeGreaterThanOrEqualTo(20);
		for (Path template : templates) {
			String withoutPlaceholders = Files.readString(template).replaceAll("\\{[a-zA-Z][a-zA-Z0-9]*\\}", "");
			assertThat(withoutPlaceholders).as(template.toString()).doesNotContain("{", "}");
		}
	}

}

package com.fortytwotalents.agentic.evaluatoroptimizer;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.Draft;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.IncidentFacts;

import static org.assertj.core.api.Assertions.assertThat;

class BreachNotificationHardRulesTests {

	private final IncidentFacts facts = new IncidentFacts("Helvetia Demo AG", "Misconfigured bucket",
			List.of("name", "email"), 1200, "2026-10-20", List.of("Bucket closed"), "dpo@demo.example");

	@Test
	void compliantDraftHasNoViolations() {
		Draft draft = new Draft("Important", "We inform you ... Contact dpo@demo.example for questions.");

		assertThat(BreachNotificationWorkflow.hardRuleViolations(facts, draft)).isEmpty();
	}

	@Test
	void detectsMissingDpoContactForbiddenPhrasesAndLength() {
		String tooLong = "word ".repeat(BreachNotificationWorkflow.MAX_WORDS + 1);
		Draft draft = new Draft("Important", "There is No Risk for you. " + tooLong);

		assertThat(BreachNotificationWorkflow.hardRuleViolations(facts, draft)).hasSize(3)
			.anySatisfy(v -> assertThat(v).startsWith("CODE-1"))
			.anySatisfy(v -> assertThat(v).startsWith("CODE-2"))
			.anySatisfy(v -> assertThat(v).contains("no risk"));
	}

}

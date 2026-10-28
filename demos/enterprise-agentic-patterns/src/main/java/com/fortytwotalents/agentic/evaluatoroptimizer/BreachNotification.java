package com.fortytwotalents.agentic.evaluatoroptimizer;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public final class BreachNotification {

	private BreachNotification() {
	}

	public record IncidentFacts(@NotBlank String organisation, @NotBlank String incidentSummary,
			@NotEmpty List<@NotBlank String> dataCategories, @Positive int affectedCustomers,
			@NotBlank String discoveredAt, @NotNull List<@NotBlank String> measuresTaken, @NotBlank String dpoContact) {
	}

	public record Draft(String subject, String body) {
	}

	public record ComplianceVerdict(@JsonPropertyDescription("true only if ALL requirements are met") boolean passed,
			@JsonPropertyDescription("0-100, how close the draft is to being sendable") int score,
			@JsonPropertyDescription("Requirement ids that are not met, e.g. R3") List<String> missingRequirements,
			@JsonPropertyDescription("Concrete, actionable instructions for the writer") String feedback) {
	}

	public record NotificationResult(Draft finalDraft, boolean approvedForSending, ComplianceVerdict finalVerdict,
			List<EvaluatorOptimizer.Round<Draft, ComplianceVerdict>> rounds) {
	}

}

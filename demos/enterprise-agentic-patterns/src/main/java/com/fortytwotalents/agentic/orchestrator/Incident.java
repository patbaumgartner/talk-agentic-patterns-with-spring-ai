package com.fortytwotalents.agentic.orchestrator;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

public final class Incident {

	private Incident() {
	}

	public record IncidentReport(@NotBlank String incidentId, @NotBlank String description) {
	}

	/** What the orchestrator sends to a worker. */
	public record WorkerRequest(@JsonPropertyDescription("Service name, e.g. payment-gateway") String service,
			@JsonPropertyDescription("The specific question the worker should answer") String question) {
	}

	public record DeploymentQuery(
			@JsonPropertyDescription("Service name, or null/empty for all services") @Nullable String service) {
	}

	/** What a worker sends back. Evidence must be quoted, not paraphrased. */
	public record WorkerFindings(String summary,
			@JsonPropertyDescription("Exact log lines or metric values with timestamps") List<String> evidence) {
	}

	public record TimelineEntry(String time, String event) {
	}

	public record ActionItem(String action, String owner, String priority) {
	}

	public record Postmortem(String title, String severity, String summary, String rootCause,
			@JsonPropertyDescription("Contributing factors and ruled-out hypotheses") List<String> contributingFactors,
			String customerImpact, List<TimelineEntry> timeline, List<ActionItem> actionItems,
			@JsonPropertyDescription("0.0 to 1.0") double confidence) {
	}

	public record Delegation(String worker, String input, long durationMs) {
	}

	public record IncidentAnalysis(Postmortem postmortem, List<Delegation> delegations) {
	}

}

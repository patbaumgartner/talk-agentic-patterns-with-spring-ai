package com.fortytwotalents.agentic.swarm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public final class Procurement {

	private Procurement() {
	}

	public record ProcurementRequest(@NotBlank String requester, @NotBlank String costCenter, @NotBlank String request) {
	}

	public record HandoffRequest(@JsonPropertyDescription("Name of the peer to hand off to") String targetAgent,
			@JsonPropertyDescription("Why this peer is needed now") String reason,
			@JsonPropertyDescription("Your findings for the case file, so nobody has to redo your work") String notes) {
	}

	public record Hop(int turn, String agent, String handedOffTo, String reason) {
	}

	public enum StopReason {

		COMPLETED, MAX_HANDOFFS_REACHED, PING_PONG_DETECTED, UNKNOWN_AGENT

	}

	public record SwarmResult(String finalAnswer, String finishedBy, List<Hop> path, Map<String, String> caseFile,
			StopReason stopReason) {
	}

	public record VendorProfile(String vendor, String hostingRegion, List<String> certifications, String lastPenTest,
			boolean processesPersonalData) {
	}

	public record DpaStatus(String vendor, String status, String transferMechanism, String note) {
	}

	public record BudgetCheck(String costCenter, BigDecimal remainingChf, BigDecimal requestedChf, boolean withinBudget,
			String approverRequired) {
	}

}

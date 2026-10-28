package com.fortytwotalents.agentic.parallelization;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.fortytwotalents.agentic.core.RiskLevel;

public final class DueDiligence {

	private DueDiligence() {
	}

	public record DueDiligenceRequest(@NotBlank String supplierName, @NotBlank String dossier) {
	}

	/**
	 * Sectioning: each dimension is a different subtask with its own criteria.
	 */
	public enum Dimension {

		FINANCIAL_STABILITY("""
				Revenue and profit trend, liquidity, auditor opinion, going-concern notes,
				dependency on a single customer or investor, payment behaviour."""),

		INFORMATION_SECURITY("""
				Valid ISO 27001 or SOC 2 Type II, penetration tests, security incidents in the
				last 3 years, MFA and encryption, subprocessors, data residency."""),

		ESG("""
				CO2 reporting (Scope 1-3), science-based targets, supply-chain due diligence,
				labour and human-rights controversies, code of conduct."""),

		LEGAL_AND_COMPLIANCE("""
				Sanctions exposure, ongoing litigation, regulatory fines, anti-bribery programme,
				beneficial ownership transparency, export control.""");

		private final String criteria;

		Dimension(String criteria) {
			this.criteria = criteria;
		}

		public String criteria() {
			return criteria;
		}

	}

	public record DimensionAssessment(Dimension dimension, RiskLevel rating,
			@JsonPropertyDescription("Facts from the dossier that justify the rating, max 5") List<String> findings,
			@JsonPropertyDescription("Contract conditions that would reduce the risk, max 3") List<String> requiredMitigations) {
	}

	public enum Decision {

		APPROVE, APPROVE_WITH_CONDITIONS, ESCALATE_TO_RISK_COMMITTEE, REJECT;

		/** Business rule as code: the worst dimension decides. */
		public static Decision from(RiskLevel overall) {
			return switch (overall) {
				case LOW -> APPROVE;
				case MEDIUM -> APPROVE_WITH_CONDITIONS;
				case HIGH -> ESCALATE_TO_RISK_COMMITTEE;
				case CRITICAL -> REJECT;
			};
		}

	}

	public record DueDiligenceReport(String supplierName, Decision decision, RiskLevel overallRisk,
			List<DimensionAssessment> assessments, List<String> conditions, String executiveSummary, Duration elapsed) {
	}

}

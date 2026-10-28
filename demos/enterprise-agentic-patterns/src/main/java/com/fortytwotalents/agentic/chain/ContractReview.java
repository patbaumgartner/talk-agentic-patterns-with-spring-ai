package com.fortytwotalents.agentic.chain;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

import com.fortytwotalents.agentic.core.RiskLevel;

/**
 * Typed hand-offs between the steps of the contract review chain.
 */
public final class ContractReview {

	private ContractReview() {
	}

	public record ContractReviewRequest(@NotBlank String contractText) {
	}

	/** Step 1 output. */
	public record ContractTerms(String counterparty, String contractType,
			@JsonPropertyDescription("true if the contract contains no liability cap at all") boolean unlimitedLiability,
			@JsonPropertyDescription("Liability cap amount, omitted if none") @Nullable BigDecimal liabilityCapAmount,
			@JsonPropertyDescription("Annual contract value") @Nullable BigDecimal annualContractValue,
			@JsonPropertyDescription("Payment terms in days after invoice") int paymentTermsDays,
			@JsonPropertyDescription("Termination notice period in days") int terminationNoticeDays,
			boolean autoRenewal, @JsonPropertyDescription("Country whose law governs the contract") String governingLaw,
			@JsonPropertyDescription("true if the supplier processes personal data on our behalf") boolean personalDataProcessed,
			@JsonPropertyDescription("true if personal data may leave Switzerland / the EEA") boolean dataTransferOutsideEea,
			@JsonPropertyDescription("Other clauses a lawyer should look at") List<String> notableClauses) {
	}

	/** Step 2 output: produced by code, not by an LLM. */
	public record PolicyDeviation(String rule, String expected, String actual, RiskLevel severity) {
	}

	/** Step 3 output. */
	public record RiskAssessment(RiskLevel overallRisk, String rationale,
			@JsonPropertyDescription("Ordered list, most important first") List<String> negotiationPriorities) {
	}

	/** Step 4 output. */
	public record NegotiationMemo(String subject, String body, List<Redline> redlines) {

		public record Redline(String clause, String currentWording, String proposedWording) {
		}

	}

	public record ContractReviewResult(String outcome, ContractTerms terms, List<PolicyDeviation> deviations,
			@Nullable RiskAssessment risk, @Nullable NegotiationMemo memo, List<String> executedSteps) {
	}

}

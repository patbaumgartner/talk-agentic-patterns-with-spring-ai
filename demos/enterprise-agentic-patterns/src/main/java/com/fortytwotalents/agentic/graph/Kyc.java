package com.fortytwotalents.agentic.graph;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

import com.fortytwotalents.agentic.core.RiskLevel;

public final class Kyc {

	private Kyc() {
	}

	public record KycApplication(@NotBlank String applicationForm, @NotNull List<@NotBlank String> newsSnippets) {
	}

	public record Applicant(String fullName, @Nullable LocalDate dateOfBirth, String nationality,
			String countryOfResidence, String occupation, @Nullable String employer,
			@JsonPropertyDescription("Expected monthly incoming funds in CHF") BigDecimal expectedMonthlyDepositsChf,
			@JsonPropertyDescription("The applicant's own words on where the money comes from") String sourceOfFundsStatement,
			@JsonPropertyDescription("Applicant declared to be a politically exposed person") boolean declaredPoliticallyExposed) {
	}

	public record ScreeningResult(boolean hit, List<String> matches, String listVersion) {
	}

	public record AdverseMediaResult(RiskLevel risk,
			@JsonPropertyDescription("Only findings clearly about THIS applicant") List<String> findings) {
	}

	public record SourceOfFundsResult(boolean plausible, RiskLevel risk, String rationale) {
	}

	public enum Outcome {

		APPROVE, ENHANCED_DUE_DILIGENCE, REJECT

	}

	public record KycDecision(Outcome outcome, RiskLevel risk, String rationale, List<String> requiredDocuments) {
	}

	public record KycResult(KycDecision decision, boolean overriddenByCode, Applicant applicant,
			ScreeningResult sanctions, AdverseMediaResult adverseMedia, SourceOfFundsResult sourceOfFunds,
			List<List<String>> executionBatches) {
	}

}

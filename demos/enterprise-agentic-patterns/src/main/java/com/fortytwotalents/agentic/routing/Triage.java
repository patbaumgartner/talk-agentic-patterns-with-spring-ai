package com.fortytwotalents.agentic.routing;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

public final class Triage {

	private Triage() {
	}

	public record CustomerEmail(@NotBlank @Email String from, String subject, @NotBlank String body) {
	}

	/**
	 * Every branch answers with the same envelope; {@code details} is branch specific.
	 */
	public record CaseOutcome(String queue, String summary, Object details, @Nullable String draftReply) {
	}

	public record ClaimIntake(@Nullable String policyNumber, @Nullable LocalDate dateOfLoss, @Nullable String location,
			String whatHappened, boolean thirdPartyInvolved, boolean injuries,
			@JsonPropertyDescription("Information we must still ask the customer for") List<String> missingInformation,
			@JsonPropertyDescription("Short acknowledgement to the customer asking for the missing information") String draftReply) {
	}

	public record BillingReply(String summary, String draftReply) {
	}

	public record ComplaintCase(String caseId, String assignedTo, LocalDate acknowledgeBy, LocalDate resolveBy) {
	}

}

package com.fortytwotalents.agentic.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fortytwotalents.agentic.chain.ContractReview.ContractTerms;
import com.fortytwotalents.agentic.chain.ContractReview.PolicyDeviation;
import com.fortytwotalents.agentic.core.RiskLevel;

/**
 * Tool as Agent: the legal department's playbook as plain Java.
 *
 * It sits in the middle of an LLM chain as a deterministic code gate. Same input, same
 * verdict, every time, and auditors can read it.
 */
public final class PolicyPlaybook {

	private static final Set<String> ACCEPTED_GOVERNING_LAWS = Set.of("switzerland", "germany", "austria");

	private PolicyPlaybook() {
	}

	public static List<PolicyDeviation> check(ContractTerms terms) {
		List<PolicyDeviation> deviations = new ArrayList<>();
		if (terms.unlimitedLiability() || terms.liabilityCapAmount() == null) {
			deviations.add(new PolicyDeviation("LIA-01 Liability cap", "Capped at max. 1x annual contract value",
					"Unlimited", RiskLevel.CRITICAL));
		}
		else if (terms.annualContractValue() != null
				&& terms.liabilityCapAmount().compareTo(terms.annualContractValue()) > 0) {
			deviations.add(new PolicyDeviation("LIA-01 Liability cap", "Capped at max. 1x annual contract value",
					terms.liabilityCapAmount() + " > " + terms.annualContractValue(), RiskLevel.HIGH));
		}
		if (terms.paymentTermsDays() < 30) {
			deviations.add(new PolicyDeviation("FIN-02 Payment terms", ">= 30 days", terms.paymentTermsDays() + " days",
					RiskLevel.MEDIUM));
		}
		if (terms.terminationNoticeDays() > 90) {
			deviations.add(new PolicyDeviation("TRM-01 Termination notice", "<= 90 days",
					terms.terminationNoticeDays() + " days", RiskLevel.MEDIUM));
		}
		if (terms.autoRenewal()) {
			deviations.add(
					new PolicyDeviation("TRM-02 Auto-renewal", "No silent auto-renewal", "Auto-renews", RiskLevel.LOW));
		}
		if (!ACCEPTED_GOVERNING_LAWS.contains(terms.governingLaw().trim().toLowerCase())) {
			deviations.add(new PolicyDeviation("LAW-01 Governing law", "Swiss, German or Austrian law",
					terms.governingLaw(), RiskLevel.HIGH));
		}
		if (terms.personalDataProcessed() && terms.dataTransferOutsideEea()) {
			deviations.add(new PolicyDeviation("DP-03 Data transfer", "Personal data stays in CH/EEA or SCCs + TIA",
					"Transfer outside CH/EEA", RiskLevel.HIGH));
		}
		return deviations;
	}

}

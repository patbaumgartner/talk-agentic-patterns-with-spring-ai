package com.fortytwotalents.agentic.chain;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.chain.ContractReview.ContractTerms;
import com.fortytwotalents.agentic.chain.ContractReview.PolicyDeviation;
import com.fortytwotalents.agentic.core.RiskLevel;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyPlaybookTests {

	private static ContractTerms terms(boolean unlimited, String cap, int paymentDays, int noticeDays,
			boolean autoRenewal, String law, boolean transferOutsideEea) {
		return new ContractTerms("CloudCorp", "SaaS", unlimited, cap == null ? null : new BigDecimal(cap),
				new BigDecimal("120000"), paymentDays, noticeDays, autoRenewal, law, true, transferOutsideEea,
				List.of());
	}

	@Test
	void playbookCompliantContractHasNoDeviations() {
		assertThat(PolicyPlaybook.check(terms(false, "120000", 30, 90, false, "Switzerland", false))).isEmpty();
	}

	@Test
	void flagsEveryDeviationWithItsSeverity() {
		List<PolicyDeviation> deviations = PolicyPlaybook
			.check(terms(true, null, 10, 180, true, "State of Delaware", true));

		assertThat(deviations).extracting(PolicyDeviation::rule)
			.extracting(rule -> rule.substring(0, 6))
			.containsExactly("LIA-01", "FIN-02", "TRM-01", "TRM-02", "LAW-01", "DP-03 ");
		assertThat(RiskLevel.highest(deviations.stream().map(PolicyDeviation::severity).toList()))
			.isEqualTo(RiskLevel.CRITICAL);
	}

	@Test
	void capAboveAnnualValueIsHighNotCritical() {
		List<PolicyDeviation> deviations = PolicyPlaybook
			.check(terms(false, "500000", 30, 90, false, "germany", false));

		assertThat(deviations).singleElement().satisfies(d -> {
			assertThat(d.rule()).startsWith("LIA-01");
			assertThat(d.severity()).isEqualTo(RiskLevel.HIGH);
		});
	}

}

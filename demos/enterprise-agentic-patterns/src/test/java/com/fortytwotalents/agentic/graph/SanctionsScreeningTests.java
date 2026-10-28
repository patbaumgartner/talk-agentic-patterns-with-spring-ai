package com.fortytwotalents.agentic.graph;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.core.RiskLevel;
import com.fortytwotalents.agentic.graph.Kyc.KycDecision;
import com.fortytwotalents.agentic.graph.Kyc.Outcome;
import com.fortytwotalents.agentic.graph.Kyc.ScreeningResult;

import static org.assertj.core.api.Assertions.assertThat;

class SanctionsScreeningTests {

	private final SanctionsScreening screening = new SanctionsScreening();

	@Test
	void matchesListedPersonIgnoringOrderCaseAndAccents() {
		ScreeningResult result = screening.screen("VOLKOV, Ivan Sérgeyevich");

		assertThat(result.hit()).isTrue();
		assertThat(result.matches()).containsExactly("Ivan Sergeyevich Volkov");
		assertThat(result.listVersion()).isEqualTo(SanctionsScreening.LIST_VERSION);
	}

	@Test
	void doesNotFlagNamesakesWithOnlyOneCommonToken() {
		assertThat(screening.screen("Ivan Meier").hit()).isFalse();
		assertThat(screening.screen("Sandra Keller").hit()).isFalse();
	}

	@Test
	void sanctionsHitOverridesTheModelDecision() {
		ScreeningResult hit = new ScreeningResult(true, List.of("Viktor Antonov Petrov"), "v1");
		KycDecision proposed = new KycDecision(Outcome.APPROVE, RiskLevel.LOW, "Looks fine", List.of());

		KycDecision decision = KycOnboardingWorkflow.applyHardRules(proposed, hit);

		assertThat(decision.outcome()).isEqualTo(Outcome.REJECT);
		assertThat(decision.risk()).isEqualTo(RiskLevel.CRITICAL);
		assertThat(decision.rationale()).contains("Viktor Antonov Petrov").contains("APPROVE");
	}

	@Test
	void keepsModelDecisionWithoutSanctionsHit() {
		ScreeningResult clean = new ScreeningResult(false, List.of(), "v1");
		KycDecision proposed = new KycDecision(Outcome.ENHANCED_DUE_DILIGENCE, RiskLevel.HIGH, "PEP",
				List.of("Proof of wealth"));

		assertThat(KycOnboardingWorkflow.applyHardRules(proposed, clean)).isSameAs(proposed);
	}

}

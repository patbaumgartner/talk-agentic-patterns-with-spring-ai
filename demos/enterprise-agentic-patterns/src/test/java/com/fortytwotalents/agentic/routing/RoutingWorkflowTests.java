package com.fortytwotalents.agentic.routing;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.routing.RoutingWorkflow.Route;
import com.fortytwotalents.agentic.routing.RoutingWorkflow.Routed;
import com.fortytwotalents.agentic.routing.RoutingWorkflow.RoutingDecision;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingWorkflowTests {

	private static final List<Route<String, String>> ROUTES = List.of(
			new Route<>("billing", "Invoices and payments", request -> "billing:" + request),
			new Route<>("claims", "Damage reports", request -> "claims:" + request));

	private static final Route<String, String> HUMAN = new Route<>("human-review", "Fallback",
			request -> "human:" + request);

	private static RoutingWorkflow<String, String> router(RoutingDecision decision) {
		return new RoutingWorkflow<>(ROUTES, (request, routes) -> decision, HUMAN, 0.7);
	}

	@Test
	void routesToTheSelectedBranch() {
		Routed<String> routed = router(new RoutingDecision("claims", "Water damage", 0.92)).route("mail");

		assertThat(routed.handledBy()).isEqualTo("claims");
		assertThat(routed.fallback()).isFalse();
		assertThat(routed.output()).isEqualTo("claims:mail");
	}

	@Test
	void fallsBackWhenTheRouterIsUnsure() {
		Routed<String> routed = router(new RoutingDecision("billing", "Maybe", 0.4)).route("mail");

		assertThat(routed.handledBy()).isEqualTo("human-review");
		assertThat(routed.fallback()).isTrue();
		assertThat(routed.decision().route()).isEqualTo("billing");
	}

	@Test
	void fallsBackWhenTheRouterHallucinatesARoute() {
		Routed<String> routed = router(new RoutingDecision("legal", "Sounds legal", 0.99)).route("mail");

		assertThat(routed.output()).isEqualTo("human:mail");
	}

	@Test
	void complaintDeadlinesSkipWeekends() {
		LocalDate friday = LocalDate.of(2026, 10, 30);

		assertThat(ClaimsInboxTriage.addBusinessDays(friday, 1)).isEqualTo(LocalDate.of(2026, 11, 2));
		assertThat(ClaimsInboxTriage.addBusinessDays(friday, 5)).isEqualTo(LocalDate.of(2026, 11, 6));
	}

}

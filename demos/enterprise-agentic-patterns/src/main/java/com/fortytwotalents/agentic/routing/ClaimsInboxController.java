package com.fortytwotalents.agentic.routing;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.routing.RoutingWorkflow.Routed;
import com.fortytwotalents.agentic.routing.Triage.CaseOutcome;
import com.fortytwotalents.agentic.routing.Triage.CustomerEmail;

@RestController
@RequestMapping("/api/routing")
class ClaimsInboxController {

	private final ClaimsInboxTriage triage;

	ClaimsInboxController(ClaimsInboxTriage triage) {
		this.triage = triage;
	}

	@PostMapping("/insurance-inbox")
	Routed<CaseOutcome> triage(@Valid @RequestBody CustomerEmail email) {
		return triage.triage(email);
	}

}

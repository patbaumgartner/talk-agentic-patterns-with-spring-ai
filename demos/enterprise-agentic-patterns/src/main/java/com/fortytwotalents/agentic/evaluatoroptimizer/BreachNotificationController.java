package com.fortytwotalents.agentic.evaluatoroptimizer;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.IncidentFacts;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.NotificationResult;

@RestController
@RequestMapping("/api/evaluator-optimizer")
class BreachNotificationController {

	private final BreachNotificationWorkflow workflow;

	BreachNotificationController(BreachNotificationWorkflow workflow) {
		this.workflow = workflow;
	}

	@PostMapping("/breach-notification")
	NotificationResult draft(@Valid @RequestBody IncidentFacts facts) {
		return workflow.draft(facts);
	}

}

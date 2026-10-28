package com.fortytwotalents.agentic.graph;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.graph.Kyc.KycApplication;
import com.fortytwotalents.agentic.graph.Kyc.KycResult;

@RestController
@RequestMapping("/api/graph")
class KycOnboardingController {

	private final KycOnboardingWorkflow workflow;

	KycOnboardingController(KycOnboardingWorkflow workflow) {
		this.workflow = workflow;
	}

	@PostMapping("/kyc-onboarding")
	KycResult onboard(@Valid @RequestBody KycApplication application) {
		return workflow.onboard(application);
	}

}

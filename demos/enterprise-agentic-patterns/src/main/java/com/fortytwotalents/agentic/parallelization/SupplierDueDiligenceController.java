package com.fortytwotalents.agentic.parallelization;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.parallelization.DueDiligence.DueDiligenceReport;
import com.fortytwotalents.agentic.parallelization.DueDiligence.DueDiligenceRequest;

@RestController
@RequestMapping("/api/parallelization")
class SupplierDueDiligenceController {

	private final SupplierDueDiligenceWorkflow workflow;

	SupplierDueDiligenceController(SupplierDueDiligenceWorkflow workflow) {
		this.workflow = workflow;
	}

	@PostMapping("/supplier-due-diligence")
	DueDiligenceReport assess(@Valid @RequestBody DueDiligenceRequest request) {
		return workflow.assess(request);
	}

}

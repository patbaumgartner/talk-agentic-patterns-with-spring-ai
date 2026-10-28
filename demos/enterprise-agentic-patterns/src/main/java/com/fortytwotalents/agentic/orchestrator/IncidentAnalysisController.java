package com.fortytwotalents.agentic.orchestrator;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.orchestrator.Incident.IncidentAnalysis;
import com.fortytwotalents.agentic.orchestrator.Incident.IncidentReport;

@RestController
@RequestMapping("/api/orchestrator")
class IncidentAnalysisController {

	private final IncidentAnalysisOrchestrator orchestrator;

	IncidentAnalysisController(IncidentAnalysisOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@PostMapping("/incident-analysis")
	IncidentAnalysis analyse(@Valid @RequestBody IncidentReport report) {
		return orchestrator.analyse(report);
	}

}

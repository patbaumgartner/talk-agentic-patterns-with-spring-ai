package com.fortytwotalents.agentic.swarm;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.swarm.Procurement.ProcurementRequest;
import com.fortytwotalents.agentic.swarm.Procurement.SwarmResult;

@RestController
@RequestMapping("/api/swarm")
class SaasProcurementController {

	private final SaasProcurementSwarm swarm;

	SaasProcurementController(SaasProcurementSwarm swarm) {
		this.swarm = swarm;
	}

	@PostMapping("/saas-procurement")
	SwarmResult review(@Valid @RequestBody ProcurementRequest request) {
		return swarm.review(request);
	}

}

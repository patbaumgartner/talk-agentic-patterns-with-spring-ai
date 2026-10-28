package com.fortytwotalents.agentic.mono;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.mono.ServiceDeskAgent.ServiceDeskRequest;
import com.fortytwotalents.agentic.mono.ServiceDeskAgent.ServiceDeskResponse;

@RestController
@RequestMapping("/api/mono")
class ServiceDeskController {

	private final ServiceDeskAgent agent;

	ServiceDeskController(ServiceDeskAgent agent) {
		this.agent = agent;
	}

	@PostMapping("/service-desk")
	ServiceDeskResponse handle(@Valid @RequestBody ServiceDeskRequest request) {
		return agent.handle(request);
	}

}

package com.fortytwotalents.agentic.evaluatoroptimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(BreachNotificationController.class)
class BreachNotificationControllerValidationTests {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	BreachNotificationWorkflow workflow;

	@Test
	void missingNumberIsNamed() {
		assertDetail("""
				{"organisation": "ACME", "incidentSummary": "Laptop stolen", "dataCategories": ["name"],
				 "discoveredAt": "2026-10-19", "measuresTaken": [], "dpoContact": "dpo@acme.example"}""",
				"affectedCustomers is missing or has the wrong type");
	}

	@Test
	void emptyCategoriesAndZeroCustomersAreRejected() {
		assertDetail("""
				{"organisation": "ACME", "incidentSummary": "Laptop stolen", "dataCategories": [],
				 "affectedCustomers": 0, "discoveredAt": "2026-10-19", "measuresTaken": [],
				 "dpoContact": "dpo@acme.example"}""",
				"affectedCustomers must be greater than 0; dataCategories must not be empty");
	}

	private void assertDetail(String json, String detail) {
		assertThat(mvc.post()
			.uri("/api/evaluator-optimizer/breach-notification")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail").isEqualTo(detail);
		verifyNoInteractions(workflow);
	}

}

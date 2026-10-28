package com.fortytwotalents.agentic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application without AWS: Bedrock is switched off and a canned
 * {@link ChatModel} answers. Proves the wiring, the prompt rendering and the
 * LLM-output-then-code-gate flow end to end.
 */
@SpringBootTest(properties = "spring.ai.model.chat=none")
@AutoConfigureMockMvc
class EnterpriseAgenticPatternsApplicationTests {

	static final List<Prompt> PROMPTS = new CopyOnWriteArrayList<>();

	static final String EXTRACTED_INVOICE = """
			{
			  "invoiceNumber": "R-2026-118",
			  "supplierName": "Alpine Cloud Services AG",
			  "supplierVatId": "CHE-123.456.789 MWST",
			  "issueDate": "2026-10-01",
			  "dueDate": "2026-10-31",
			  "currency": "CHF",
			  "lineItems": [
			    { "description": "Kubernetes support", "quantity": 10, "unitPrice": 150.00, "total": 1500.00 }
			  ],
			  "netAmount": 1500.00,
			  "vatAmount": 121.50,
			  "grossAmount": 1621.50
			}
			""";

	@TestConfiguration
	static class CannedModel {

		@Bean
		ChatModel chatModel() {
			return prompt -> {
				PROMPTS.add(prompt);
				return new ChatResponse(List.of(new Generation(new AssistantMessage(EXTRACTED_INVOICE))));
			};
		}

	}

	@Autowired
	MockMvc mvc;

	@Test
	void extractsAnInvoiceAndLetsCodeDecideTheNextStep() throws Exception {
		mvc.perform(post("/api/task-execution/invoices").contentType(MediaType.APPLICATION_JSON)
			.content("{\"document\": \"Rechnung R-2026-118 von Alpine Cloud Services AG\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.invoice.invoiceNumber").value("R-2026-118"))
			.andExpect(jsonPath("$.nextStep").value("MANUAL_REVIEW"))
			.andExpect(jsonPath("$.validationIssues[0]").value("No purchase order number: three-way match impossible"));

		// One call: the canned answer satisfies the JSON schema, so no validation
		// retries.
		assertThat(PROMPTS).singleElement()
			.satisfies(prompt -> assertThat(prompt.getUserMessage().getText())
				.contains("Rechnung R-2026-118 von Alpine Cloud Services AG"));
	}

}

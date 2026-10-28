package com.fortytwotalents.agentic.taskexecution;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.TaskExecutionAgent;

/**
 * Pattern: Task Execution (the atom).
 *
 * Enterprise problem: accounts payable receives thousands of PDF / e-mail invoices in
 * every layout imaginable. One template in, one typed {@link Invoice} out, then
 * deterministic code decides whether it can be auto-posted.
 */
@Service
public class InvoiceExtractionService {

	private final TaskExecutionAgent<InvoiceExtractionRequest, Invoice> extractor;

	public InvoiceExtractionService(Agents agents) {
		this.extractor = TaskExecutionAgent.<InvoiceExtractionRequest, Invoice>builder("InvoiceExtraction")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/extract-invoice.st")
			.params(request -> Map.of("document", request.document()))
			.responseType(Invoice.class)
			.build();
	}

	public InvoiceExtractionResult extract(InvoiceExtractionRequest request) {
		Invoice invoice = extractor.apply(request);
		List<String> issues = InvoiceValidator.validate(invoice);
		return new InvoiceExtractionResult(invoice, issues, issues.isEmpty() ? "AUTO_POST" : "MANUAL_REVIEW");
	}

	public record InvoiceExtractionRequest(@NotBlank String document) {
	}

	public record InvoiceExtractionResult(Invoice invoice, List<String> validationIssues, String nextStep) {
	}

}

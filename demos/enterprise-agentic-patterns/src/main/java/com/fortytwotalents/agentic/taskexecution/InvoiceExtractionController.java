package com.fortytwotalents.agentic.taskexecution;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.taskexecution.InvoiceExtractionService.InvoiceExtractionRequest;
import com.fortytwotalents.agentic.taskexecution.InvoiceExtractionService.InvoiceExtractionResult;

@RestController
@RequestMapping("/api/task-execution")
class InvoiceExtractionController {

	private final InvoiceExtractionService service;

	InvoiceExtractionController(InvoiceExtractionService service) {
		this.service = service;
	}

	@PostMapping("/invoices")
	InvoiceExtractionResult extract(@Valid @RequestBody InvoiceExtractionRequest request) {
		return service.extract(request);
	}

}

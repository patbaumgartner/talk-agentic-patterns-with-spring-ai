package com.fortytwotalents.agentic.chain;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortytwotalents.agentic.chain.ContractReview.ContractReviewRequest;
import com.fortytwotalents.agentic.chain.ContractReview.ContractReviewResult;

@RestController
@RequestMapping("/api/chain")
class ContractReviewController {

	private final ContractReviewChain chain;

	ContractReviewController(ContractReviewChain chain) {
		this.chain = chain;
	}

	@PostMapping("/contract-review")
	ContractReviewResult review(@Valid @RequestBody ContractReviewRequest request) {
		return chain.review(request);
	}

}

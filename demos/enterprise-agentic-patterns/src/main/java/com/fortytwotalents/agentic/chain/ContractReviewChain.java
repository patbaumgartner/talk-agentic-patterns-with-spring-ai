package com.fortytwotalents.agentic.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.chain.ContractReview.ContractReviewRequest;
import com.fortytwotalents.agentic.chain.ContractReview.ContractReviewResult;
import com.fortytwotalents.agentic.chain.ContractReview.ContractTerms;
import com.fortytwotalents.agentic.chain.ContractReview.NegotiationMemo;
import com.fortytwotalents.agentic.chain.ContractReview.PolicyDeviation;
import com.fortytwotalents.agentic.chain.ContractReview.RiskAssessment;
import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.Json;
import com.fortytwotalents.agentic.core.TaskExecutionAgent;

/**
 * Pattern: Graph / Chain Workflow. Output of step N is the input of step N+1.
 *
 * Enterprise problem: legal reviews hundreds of supplier contracts a year against the
 * same playbook. The chain is explicit Java code, so the flow is readable, testable and
 * auditable:
 *
 * <pre>
 * extract terms (LLM) → playbook gate (CODE) → assess risk (LLM) → draft memo (LLM)
 *                              └── no deviations: stop, approve, save two LLM calls
 * </pre>
 */
@Service
public class ContractReviewChain {

	private final TaskExecutionAgent<String, ContractTerms> extractTerms;

	private final TaskExecutionAgent<StepInput, RiskAssessment> assessRisk;

	private final TaskExecutionAgent<StepInput, NegotiationMemo> draftMemo;

	public ContractReviewChain(Agents agents) {
		this.extractTerms = TaskExecutionAgent.<String, ContractTerms>builder("ContractReview#1-ExtractTerms")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/chain/1-extract-contract-terms.st")
			.params(contract -> Map.of("contract", contract))
			.responseType(ContractTerms.class)
			.build();
		this.assessRisk = TaskExecutionAgent.<StepInput, RiskAssessment>builder("ContractReview#3-AssessRisk")
			.chatClient(agents.smart().build())
			.promptTemplate("prompts/chain/3-assess-contract-risk.st")
			.params(StepInput::asParams)
			.responseType(RiskAssessment.class)
			.build();
		this.draftMemo = TaskExecutionAgent.<StepInput, NegotiationMemo>builder("ContractReview#4-DraftMemo")
			.chatClient(agents.smart().build())
			.promptTemplate("prompts/chain/4-draft-negotiation-memo.st")
			.params(StepInput::asParams)
			.responseType(NegotiationMemo.class)
			.build();
	}

	public ContractReviewResult review(ContractReviewRequest request) {
		List<String> steps = new ArrayList<>();

		ContractTerms terms = extractTerms.apply(request.contractText());
		steps.add("1. extract-terms (LLM, fast)");

		List<PolicyDeviation> deviations = PolicyPlaybook.check(terms);
		steps.add("2. playbook-gate (code): %d deviation(s)".formatted(deviations.size()));
		if (deviations.isEmpty()) {
			return new ContractReviewResult("APPROVED_PER_PLAYBOOK", terms, deviations, null, null, steps);
		}

		RiskAssessment risk = assessRisk.apply(new StepInput(request.contractText(), terms, deviations, null));
		steps.add("3. assess-risk (LLM, smart): " + risk.overallRisk());

		NegotiationMemo memo = draftMemo.apply(new StepInput(request.contractText(), terms, deviations, risk));
		steps.add("4. draft-memo (LLM, smart): %d redline(s)".formatted(memo.redlines().size()));

		return new ContractReviewResult("NEGOTIATE", terms, deviations, risk, memo, steps);
	}

	record StepInput(String contract, ContractTerms terms, List<PolicyDeviation> deviations, RiskAssessment risk) {

		Map<String, Object> asParams() {
			return Map.of("contract", contract, "terms", Json.write(terms), "deviations", Json.write(deviations),
					"risk", risk == null ? "n/a" : Json.write(risk));
		}

	}

}

package com.fortytwotalents.agentic.graph;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.Json;
import com.fortytwotalents.agentic.core.RiskLevel;
import com.fortytwotalents.agentic.core.TaskExecutionAgent;
import com.fortytwotalents.agentic.graph.GraphWorkflow.GraphRun;
import com.fortytwotalents.agentic.graph.Kyc.AdverseMediaResult;
import com.fortytwotalents.agentic.graph.Kyc.Applicant;
import com.fortytwotalents.agentic.graph.Kyc.KycApplication;
import com.fortytwotalents.agentic.graph.Kyc.KycDecision;
import com.fortytwotalents.agentic.graph.Kyc.KycResult;
import com.fortytwotalents.agentic.graph.Kyc.Outcome;
import com.fortytwotalents.agentic.graph.Kyc.ScreeningResult;
import com.fortytwotalents.agentic.graph.Kyc.SourceOfFundsResult;

/**
 * Pattern: Graph. A known-up-front DAG of agents and code.
 *
 * Enterprise problem: KYC / AML onboarding of a new private-banking client.
 *
 * <pre>
 *                        ┌─> sanctions-screening (CODE) ─┐
 * extract-applicant (LLM)├─> adverse-media       (LLM)  ─┼─> risk-decision (LLM) ─> code override
 *                        └─> source-of-funds     (LLM)  ─┘
 * </pre>
 *
 * The audience can predict the execution order before you hit run: that is the point.
 */
@Service
public class KycOnboardingWorkflow {

	static final String EXTRACT = "extract-applicant";

	static final String SANCTIONS = "sanctions-screening";

	static final String ADVERSE_MEDIA = "adverse-media";

	static final String SOURCE_OF_FUNDS = "source-of-funds";

	static final String DECISION = "risk-decision";

	private final GraphWorkflow graph;

	public KycOnboardingWorkflow(Agents agents, SanctionsScreening sanctions) {
		var extract = TaskExecutionAgent.<KycApplication, Applicant>builder("KYC#Extract")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/graph/extract-applicant.st")
			.params(app -> Map.of("form", app.applicationForm()))
			.responseType(Applicant.class)
			.build();
		var adverseMedia = TaskExecutionAgent.<AdverseMediaInput, AdverseMediaResult>builder("KYC#AdverseMedia")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/graph/adverse-media.st")
			.params(in -> Map.of("applicant", Json.write(in.applicant()), "news", String.join("\n---\n", in.news())))
			.responseType(AdverseMediaResult.class)
			.build();
		var sourceOfFunds = TaskExecutionAgent.<Applicant, SourceOfFundsResult>builder("KYC#SourceOfFunds")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/graph/source-of-funds.st")
			.params(applicant -> Map.of("applicant", Json.write(applicant)))
			.responseType(SourceOfFundsResult.class)
			.build();
		var decide = TaskExecutionAgent.<Map<String, Object>, KycDecision>builder("KYC#Decision")
			.chatClient(agents.smart().build())
			.promptTemplate("prompts/graph/kyc-decision.st")
			.params(upstream -> Map.of("upstream", Json.write(upstream)))
			.responseType(KycDecision.class)
			.build();

		this.graph = GraphWorkflow.builder()
			.node(EXTRACT, state -> extract.apply(state.input(KycApplication.class)))
			.node(SANCTIONS, state -> sanctions.screen(state.output(EXTRACT, Applicant.class).fullName()))
			.node(ADVERSE_MEDIA,
					state -> adverseMedia.apply(new AdverseMediaInput(state.output(EXTRACT, Applicant.class),
							state.input(KycApplication.class).newsSnippets())))
			.node(SOURCE_OF_FUNDS, state -> sourceOfFunds.apply(state.output(EXTRACT, Applicant.class)))
			.node(DECISION, state -> decide.apply(Map.of( //
					"applicant", state.output(EXTRACT, Applicant.class), //
					"sanctionsScreening", state.output(SANCTIONS, ScreeningResult.class), //
					"adverseMedia", state.output(ADVERSE_MEDIA, AdverseMediaResult.class), //
					"sourceOfFunds", state.output(SOURCE_OF_FUNDS, SourceOfFundsResult.class))))
			.edge(EXTRACT, SANCTIONS)
			.edge(EXTRACT, ADVERSE_MEDIA)
			.edge(EXTRACT, SOURCE_OF_FUNDS)
			.edge(SANCTIONS, DECISION)
			.edge(ADVERSE_MEDIA, DECISION)
			.edge(SOURCE_OF_FUNDS, DECISION)
			.maxConcurrency(agents.limits().maxParallelCalls())
			.batchTimeout(Duration.ofMinutes(2))
			.build();
	}

	public KycResult onboard(KycApplication application) {
		GraphRun run = graph.run(application);
		ScreeningResult screening = (ScreeningResult) run.outputs().get(SANCTIONS);
		KycDecision proposed = (KycDecision) run.outputs().get(DECISION);
		KycDecision decision = applyHardRules(proposed, screening);
		return new KycResult(decision, decision != proposed, (Applicant) run.outputs().get(EXTRACT), screening,
				(AdverseMediaResult) run.outputs().get(ADVERSE_MEDIA),
				(SourceOfFundsResult) run.outputs().get(SOURCE_OF_FUNDS), run.batches());
	}

	/** Code has the last word: a sanctions hit is never negotiable. */
	static KycDecision applyHardRules(KycDecision proposed, ScreeningResult screening) {
		if (screening.hit() && proposed.outcome() != Outcome.REJECT) {
			return new KycDecision(Outcome.REJECT, RiskLevel.CRITICAL,
					"Sanctions list match " + screening.matches() + " on " + screening.listVersion()
							+ ". Rejected by policy, escalated to Compliance. Model proposed: " + proposed.outcome(),
					List.of());
		}
		return proposed;
	}

	record AdverseMediaInput(Applicant applicant, List<String> news) {
	}

}

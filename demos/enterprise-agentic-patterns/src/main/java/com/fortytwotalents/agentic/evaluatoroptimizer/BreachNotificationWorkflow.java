package com.fortytwotalents.agentic.evaluatoroptimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.Json;
import com.fortytwotalents.agentic.core.TaskExecutionAgent;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.ComplianceVerdict;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.Draft;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.IncidentFacts;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.NotificationResult;

/**
 * Pattern: Graph / Evaluator-Optimizer. A bounded cycle on a structured verdict.
 *
 * Enterprise problem: after a personal-data breach, GDPR Art. 34 and the Swiss revFADP
 * Art. 24 require a notification to affected customers with mandatory content. Legal
 * reviews every draft by hand, under time pressure. Here a writer drafts, a DIFFERENT
 * model judges against the legal checklist, plain code enforces the hard rules, and the
 * loop is capped. Nothing is sent automatically: a human approves the best draft.
 */
@Service
public class BreachNotificationWorkflow {

	static final int MAX_WORDS = 350;

	static final List<String> FORBIDDEN_PHRASES = List.of("no reason to worry", "not liable", "no risk",
			"sophisticated attack");

	private final TaskExecutionAgent<IncidentFacts, Draft> writer;

	private final TaskExecutionAgent<JudgeInput, ComplianceVerdict> judge;

	private final TaskExecutionAgent<RewriteInput, Draft> rewriter;

	private final int maxRounds;

	public BreachNotificationWorkflow(Agents agents) {
		this.writer = TaskExecutionAgent.<IncidentFacts, Draft>builder("BreachNotice#Writer")
			.chatClient(agents.smart().build())
			.promptTemplate("prompts/evaluatoroptimizer/write-breach-notice.st")
			.params(facts -> Map.of("facts", Json.write(facts)))
			.responseType(Draft.class)
			.build();
		this.judge = TaskExecutionAgent.<JudgeInput, ComplianceVerdict>builder("BreachNotice#ComplianceJudge")
			.chatClient(agents.judge().build())
			.promptTemplate("prompts/evaluatoroptimizer/judge-breach-notice.st")
			.params(in -> Map.of("facts", Json.write(in.facts()), "draft", Json.write(in.draft())))
			.responseType(ComplianceVerdict.class)
			.build();
		this.rewriter = TaskExecutionAgent.<RewriteInput, Draft>builder("BreachNotice#Rewriter")
			.chatClient(agents.smart().build())
			.promptTemplate("prompts/evaluatoroptimizer/rewrite-breach-notice.st")
			.params(in -> Map.of("facts", Json.write(in.facts()), "draft", Json.write(in.draft()), "verdict",
					Json.write(in.verdict())))
			.responseType(Draft.class)
			.build();
		this.maxRounds = agents.limits().maxEvaluationRounds();
	}

	public NotificationResult draft(IncidentFacts facts) {
		var loop = new EvaluatorOptimizer<Draft, ComplianceVerdict>( //
				() -> writer.apply(facts), //
				draft -> evaluate(facts, draft), //
				(draft, verdict) -> rewriter.apply(new RewriteInput(facts, draft, verdict)), //
				ComplianceVerdict::passed, //
				ComplianceVerdict::score, //
				maxRounds);
		var outcome = loop.run();
		return new NotificationResult(outcome.best(), outcome.passed(), outcome.bestVerdict(), outcome.rounds());
	}

	/** LLM judge for the fuzzy rules, code for the hard ones. Code always wins. */
	ComplianceVerdict evaluate(IncidentFacts facts, Draft draft) {
		ComplianceVerdict llm = judge.apply(new JudgeInput(facts, draft));
		List<String> hardFailures = hardRuleViolations(facts, draft);
		if (hardFailures.isEmpty()) {
			return llm;
		}
		List<String> missing = new ArrayList<>(llm.missingRequirements());
		missing.addAll(hardFailures);
		return new ComplianceVerdict(false, Math.min(llm.score(), 60), missing,
				llm.feedback() + " Hard rules violated: " + String.join("; ", hardFailures));
	}

	static List<String> hardRuleViolations(IncidentFacts facts, Draft draft) {
		List<String> violations = new ArrayList<>();
		String body = draft.body().toLowerCase(Locale.ROOT);
		if (!body.contains(facts.dpoContact().toLowerCase(Locale.ROOT))) {
			violations.add("CODE-1 DPO contact '" + facts.dpoContact() + "' must appear verbatim");
		}
		int words = draft.body().trim().split("\\s+").length;
		if (words > MAX_WORDS) {
			violations.add("CODE-2 body has " + words + " words, max " + MAX_WORDS);
		}
		FORBIDDEN_PHRASES.stream()
			.filter(body::contains)
			.forEach(phrase -> violations.add("CODE-3 forbidden phrase: '" + phrase + "'"));
		return violations;
	}

	record JudgeInput(IncidentFacts facts, Draft draft) {
	}

	record RewriteInput(IncidentFacts facts, Draft draft, ComplianceVerdict verdict) {
	}

}

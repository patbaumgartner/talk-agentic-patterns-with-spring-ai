package com.fortytwotalents.agentic.parallelization;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.Json;
import com.fortytwotalents.agentic.core.Parallel;
import com.fortytwotalents.agentic.core.RiskLevel;
import com.fortytwotalents.agentic.core.TaskExecutionAgent;
import com.fortytwotalents.agentic.parallelization.DueDiligence.Decision;
import com.fortytwotalents.agentic.parallelization.DueDiligence.DimensionAssessment;
import com.fortytwotalents.agentic.parallelization.DueDiligence.DueDiligenceReport;
import com.fortytwotalents.agentic.parallelization.DueDiligence.DueDiligenceRequest;

/**
 * Pattern: Graph / Parallelization (sectioning). Fan out, then assemble.
 *
 * Enterprise problem: third-party risk management. Onboarding a new supplier needs a
 * finance, infosec, ESG and legal review. Four independent reviewers run at the same time
 * on virtual threads; assembly is plain code (the worst dimension decides), the LLM only
 * writes the executive summary.
 */
@Service
public class SupplierDueDiligenceWorkflow {

	private static final Logger log = LoggerFactory.getLogger(SupplierDueDiligenceWorkflow.class);

	private final TaskExecutionAgent<Subtask, DimensionAssessment> reviewer;

	private final ChatClient summaryWriter;

	private final int maxParallelCalls;

	public SupplierDueDiligenceWorkflow(Agents agents) {
		this.reviewer = TaskExecutionAgent.<Subtask, DimensionAssessment>builder("SupplierReviewer")
			.chatClient(agents.fast().build())
			.promptTemplate("prompts/parallelization/assess-supplier-dimension.st")
			.params(subtask -> Map.of("dimension", subtask.dimension().name(), "criteria",
					subtask.dimension().criteria(), "supplier", subtask.request().supplierName(), "dossier",
					subtask.request().dossier()))
			.responseType(DimensionAssessment.class)
			.build();
		this.summaryWriter = agents.smart()
			.defaultSystem("You write executive summaries for the procurement board. Max 120 words, no bullet points.")
			.build();
		this.maxParallelCalls = agents.limits().maxParallelCalls();
	}

	public DueDiligenceReport assess(DueDiligenceRequest request) {
		long start = System.nanoTime();

		// Fan out: one Task Execution agent per dimension, bounded against rate limits.
		List<Subtask> subtasks = Arrays.stream(DueDiligence.Dimension.values())
			.map(dimension -> new Subtask(dimension, request))
			.toList();
		List<DimensionAssessment> assessments = Parallel.map(subtasks, this::review, maxParallelCalls,
				Duration.ofMinutes(2));

		// Assemble with code when you can ...
		RiskLevel overall = RiskLevel.highest(assessments.stream().map(DimensionAssessment::rating).toList());
		Decision decision = Decision.from(overall);
		List<String> conditions = assessments.stream()
			.filter(a -> a.rating().compareTo(RiskLevel.MEDIUM) >= 0)
			.flatMap(a -> a.requiredMitigations().stream())
			.distinct()
			.toList();

		// ... with an LLM when you must.
		String summary = summaryWriter.prompt()
			.user(user -> user.text("""
					Supplier: {supplier}
					Decision (final, computed by policy): {decision}
					Assessments: {assessments}
					""")
				.param("supplier", request.supplierName())
				.param("decision", decision.name())
				.param("assessments", Json.write(assessments)))
			.call()
			.content();

		return new DueDiligenceReport(request.supplierName(), decision, overall, assessments, conditions, summary,
				Duration.ofNanos(System.nanoTime() - start));
	}

	private DimensionAssessment review(Subtask subtask) {
		log.atInfo()
			.addArgument(subtask.dimension())
			.addArgument(() -> Thread.currentThread())
			.log("[due-diligence] {} reviewing on {}");
		DimensionAssessment assessment = reviewer.apply(subtask);
		// The model must not relabel the section it was asked to review.
		return new DimensionAssessment(subtask.dimension(), assessment.rating(), assessment.findings(),
				assessment.requiredMitigations());
	}

	record Subtask(DueDiligence.Dimension dimension, DueDiligenceRequest request) {
	}

}

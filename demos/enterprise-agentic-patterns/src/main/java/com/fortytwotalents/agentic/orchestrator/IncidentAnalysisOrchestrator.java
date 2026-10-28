package com.fortytwotalents.agentic.orchestrator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.core.Json;
import com.fortytwotalents.agentic.orchestrator.Incident.Delegation;
import com.fortytwotalents.agentic.orchestrator.Incident.DeploymentQuery;
import com.fortytwotalents.agentic.orchestrator.Incident.IncidentAnalysis;
import com.fortytwotalents.agentic.orchestrator.Incident.IncidentReport;
import com.fortytwotalents.agentic.orchestrator.Incident.Postmortem;
import com.fortytwotalents.agentic.orchestrator.Incident.WorkerFindings;
import com.fortytwotalents.agentic.orchestrator.Incident.WorkerRequest;
import com.fortytwotalents.agentic.orchestrator.ObservabilityTools.LogTools;
import com.fortytwotalents.agentic.orchestrator.ObservabilityTools.MetricTools;

/**
 * Pattern: Orchestrator-Workers. The LLM decides which worker to ask, in which order and
 * how often. Workers are agents exposed as tools ("agents as tools").
 *
 * Enterprise problem: root-cause analysis of a production incident and a postmortem
 * draft. The SRE gets a first hypothesis with evidence in minutes instead of hours.
 */
@Service
public class IncidentAnalysisOrchestrator {

	private static final Resource ORCHESTRATOR_PROMPT = new ClassPathResource(
			"prompts/orchestrator/incident-commander.st");

	private static final Resource WORKER_PROMPT = new ClassPathResource("prompts/orchestrator/worker.st");

	private final Agents agents;

	private final ObservabilityBackend backend;

	private final ChatClient logAnalyst;

	private final ChatClient metricsAnalyst;

	public IncidentAnalysisOrchestrator(Agents agents, ObservabilityBackend backend) {
		this.agents = agents;
		this.backend = backend;
		this.logAnalyst = agents.fast()
			.defaultSystem("You are a log analyst. Use your tools, quote log lines verbatim with timestamps.")
			.defaultTools(new LogTools(backend))
			.build();
		this.metricsAnalyst = agents.fast()
			.defaultSystem("You are a metrics analyst. Use your tools, quote metric values with timestamps.")
			.defaultTools(new MetricTools(backend))
			.build();
	}

	public IncidentAnalysis analyse(IncidentReport report) {
		List<Delegation> delegations = new CopyOnWriteArrayList<>();

		List<ToolCallback> workers = List.of(worker("log_analyst",
				"Ask the log analyst a question about the logs of ONE service. Returns findings with quoted evidence.",
				WorkerRequest.class, request -> askWorker(logAnalyst, request), delegations),
				worker("metrics_analyst",
						"Ask the metrics analyst a question about latency, error rates, throughput or "
								+ "connection pools of ONE service. Returns findings with quoted evidence.",
						WorkerRequest.class, request -> askWorker(metricsAnalyst, request), delegations),
				// Tool as Agent: no LLM needed to read a deployment log.
				worker("deployment_history",
						"List deployments with version, time and changelog. Pass a service name or leave empty for all.",
						DeploymentQuery.class, query -> backend.deployments(query.service()), delegations));

		Postmortem postmortem = agents.smart()
			.build()
			.prompt()
			.user(user -> user.text(ORCHESTRATOR_PROMPT)
				.param("incidentId", report.incidentId())
				.param("description", report.description()))
			.toolCallbacks(workers)
			.call()
			.entity(Postmortem.class);

		return new IncidentAnalysis(postmortem, List.copyOf(delegations));
	}

	private WorkerFindings askWorker(ChatClient worker, WorkerRequest request) {
		return worker.prompt()
			.user(user -> user.text(WORKER_PROMPT)
				.param("service", request.service())
				.param("question", request.question()))
			.call()
			.entity(WorkerFindings.class);
	}

	/** Wraps a worker as a tool and records every delegation: the audit trail is code. */
	private static <I, O> ToolCallback worker(String name, String description, Class<I> inputType,
			Function<I, O> worker, List<Delegation> delegations) {
		return FunctionToolCallback.builder(name, (I input) -> {
			long start = System.currentTimeMillis();
			O output = worker.apply(input);
			delegations.add(new Delegation(name, Json.write(input), System.currentTimeMillis() - start));
			return output;
		}).description(description).inputType(inputType).build();
	}

}

package com.fortytwotalents.agentic.swarm;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;
import com.fortytwotalents.agentic.swarm.Procurement.HandoffRequest;
import com.fortytwotalents.agentic.swarm.Procurement.ProcurementRequest;
import com.fortytwotalents.agentic.swarm.Procurement.SwarmResult;
import com.fortytwotalents.agentic.swarm.ProcurementTools.BudgetTools;
import com.fortytwotalents.agentic.swarm.ProcurementTools.ContractTools;
import com.fortytwotalents.agentic.swarm.ProcurementTools.VendorRiskTools;

/**
 * Pattern: Swarm. Intake, security, legal and finance are peers; each owns one system of
 * record and decides who needs to look at the case next.
 *
 * Enterprise problem: a business team wants to buy a SaaS tool. Classic ticket ping-pong
 * between departments, now with a shared case file and a hard handoff budget.
 */
@Service
public class SaasProcurementSwarm {

	private final Swarm swarm;

	public SaasProcurementSwarm(Agents agents) {
		this.swarm = new Swarm(List.of( //
				new LlmPeer(agents, "intake", "Clarifies the request: vendor, use case, data involved, annual cost.",
						null), //
				new LlmPeer(agents, "security", "Assesses vendor security and data residency.", new VendorRiskTools()), //
				new LlmPeer(agents, "legal", "Checks the data processing agreement and cross-border transfers.",
						new ContractTools()), //
				new LlmPeer(agents, "finance", "Checks budget and the required approver.", new BudgetTools())),
				agents.limits().maxHandoffs());
	}

	public SwarmResult review(ProcurementRequest request) {
		String task = "Requester: %s%nCost center: %s%n%s".formatted(request.requester(), request.costCenter(),
				request.request());
		return swarm.run("intake", task);
	}

	/** A peer is a ChatClient with its own tools plus the shared handoff tool. */
	private record LlmPeer(Agents agents, String name, String description, Object domainTools) implements Swarm.Peer {

		@Override
		public String act(String prompt, Consumer<HandoffRequest> handoff) {
			ToolCallback handoffTool = FunctionToolCallback.builder("handoff_to_agent", (HandoffRequest request) -> {
				handoff.accept(request);
				return "Handed off to " + request.targetAgent();
			})
				.description("Pass the case to a peer. Write your findings into notes.")
				.inputType(HandoffRequest.class)
				// End this agent's turn right after the handoff; the swarm loop takes
				// over.
				.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
				.build();

			var spec = agents.smart()
				.build()
				.prompt()
				.system(new ClassPathResource("prompts/swarm/" + name + ".st"))
				.user(prompt)
				.toolCallbacks(handoffTool);
			if (domainTools != null) {
				spec = spec.tools(domainTools);
			}
			return spec.call().content();
		}

	}

}

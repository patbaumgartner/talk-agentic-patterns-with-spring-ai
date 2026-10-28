package com.fortytwotalents.agentic.swarm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortytwotalents.agentic.swarm.Procurement.HandoffRequest;
import com.fortytwotalents.agentic.swarm.Procurement.Hop;
import com.fortytwotalents.agentic.swarm.Procurement.StopReason;
import com.fortytwotalents.agentic.swarm.Procurement.SwarmResult;

/**
 * Peers hand control to each other; there is no boss. The loop itself is plain code and
 * owns the guard rails: a handoff budget, ping-pong detection, unknown targets.
 *
 * LLM-agnostic on purpose, so the guards are unit-testable without a model.
 */
public final class Swarm {

	private static final Logger log = LoggerFactory.getLogger(Swarm.class);

	/** Last N hops that may involve only two agents before we call it ping-pong. */
	static final int PING_PONG_WINDOW = 4;

	public interface Peer {

		String name();

		String description();

		/**
		 * Run one turn. Call {@code handoff} to pass control on; return without calling
		 * it to finish the swarm with the returned text.
		 */
		String act(String prompt, Consumer<HandoffRequest> handoff);

	}

	private final Map<String, Peer> peers = new LinkedHashMap<>();

	private final int maxHandoffs;

	public Swarm(List<Peer> peers, int maxHandoffs) {
		peers.forEach(peer -> this.peers.put(peer.name(), peer));
		this.maxHandoffs = maxHandoffs;
	}

	public SwarmResult run(String entryPeer, String task) {
		Peer current = peers.get(entryPeer);
		Map<String, String> caseFile = new LinkedHashMap<>();
		List<Hop> path = new ArrayList<>();
		String handoffReason = null;

		for (int turn = 1;; turn++) {
			AtomicReference<HandoffRequest> requested = new AtomicReference<>();
			String reply = current.act(prompt(current, task, caseFile, handoffReason), requested::set);
			HandoffRequest handoff = requested.get();

			if (handoff == null) {
				log.info("[swarm] turn {}: {} finished", turn, current.name());
				return new SwarmResult(reply, current.name(), path, caseFile, StopReason.COMPLETED);
			}

			log.atInfo()
				.addArgument(turn)
				.addArgument(current.name())
				.addArgument(handoff.targetAgent())
				.addArgument(handoff.reason())
				.log("[swarm] turn {}: {} -> {} ({})");
			caseFile.merge(current.name(), handoff.notes(), (a, b) -> a + "\n" + b);
			path.add(new Hop(turn, current.name(), handoff.targetAgent(), handoff.reason()));

			if (path.size() > maxHandoffs) {
				return stopped(current, path, caseFile, StopReason.MAX_HANDOFFS_REACHED);
			}
			Peer next = peers.get(handoff.targetAgent());
			if (next == null) {
				return stopped(current, path, caseFile, StopReason.UNKNOWN_AGENT);
			}
			if (isPingPong(path)) {
				return stopped(current, path, caseFile, StopReason.PING_PONG_DETECTED);
			}
			current = next;
			handoffReason = handoff.reason();
		}
	}

	static boolean isPingPong(List<Hop> path) {
		if (path.size() < PING_PONG_WINDOW) {
			return false;
		}
		Set<String> agents = new HashSet<>();
		for (Hop hop : path.subList(path.size() - PING_PONG_WINDOW, path.size())) {
			agents.add(hop.agent());
			agents.add(hop.handedOffTo());
		}
		return agents.size() <= 2;
	}

	private static SwarmResult stopped(Peer current, List<Hop> path, Map<String, String> caseFile, StopReason reason) {
		log.warn("[swarm] stopped: {} after {} handoffs", reason, path.size());
		return new SwarmResult("Escalated to a human: the swarm stopped (" + reason + "). See the case file.",
				current.name(), path, caseFile, reason);
	}

	private String prompt(Peer current, String task, Map<String, String> caseFile, String handoffReason) {
		String roster = peers.values()
			.stream()
			.filter(p -> !p.name().equals(current.name()))
			.map(p -> "- " + p.name() + ": " + p.description())
			.collect(Collectors.joining("\n"));
		String notes = caseFile.isEmpty() ? "(empty)"
				: caseFile.entrySet()
					.stream()
					.map(e -> "## " + e.getKey() + "\n" + e.getValue())
					.collect(Collectors.joining("\n\n"));
		return """
				Request:
				%s

				You were handed this case because: %s

				Case file (findings of your peers):
				%s

				Your peers:
				%s

				Rules:
				- Use your own tools for your own domain. Never guess another peer's findings.
				- Either call handoff_to_agent exactly ONCE, or answer without calling it to close the case.
				- Close the case only when every peer needed for a decision has written to the case file,
				  or when a blocking finding makes approval impossible.
				""".formatted(task, handoffReason == null ? "you are the entry point" : handoffReason, notes, roster);
	}

}

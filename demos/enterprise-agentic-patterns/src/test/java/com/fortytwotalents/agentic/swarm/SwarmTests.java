package com.fortytwotalents.agentic.swarm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.swarm.Procurement.HandoffRequest;
import com.fortytwotalents.agentic.swarm.Procurement.Hop;
import com.fortytwotalents.agentic.swarm.Procurement.StopReason;
import com.fortytwotalents.agentic.swarm.Procurement.SwarmResult;

import static org.assertj.core.api.Assertions.assertThat;

class SwarmTests {

	/** Scripted peer: each turn either hands off to the next target or finishes. */
	private static final class ScriptedPeer implements Swarm.Peer {

		private final String name;

		private final Deque<String> targets;

		private String lastPrompt;

		ScriptedPeer(String name, String... targets) {
			this.name = name;
			this.targets = new ArrayDeque<>(List.of(targets));
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String description() {
			return name + " peer";
		}

		@Override
		public String act(String prompt, Consumer<HandoffRequest> handoff) {
			this.lastPrompt = prompt;
			String target = targets.poll();
			if (target == null) {
				return name + " closes the case";
			}
			handoff.accept(new HandoffRequest(target, name + " needs " + target, name + " notes"));
			return "handed off";
		}

	}

	@Test
	void peersHandOffUntilOneClosesTheCase() {
		ScriptedPeer intake = new ScriptedPeer("intake", "security");
		ScriptedPeer security = new ScriptedPeer("security", "finance");
		ScriptedPeer finance = new ScriptedPeer("finance", "intake");

		SwarmResult result = new Swarm(List.of(intake, security, finance), 8).run("intake", "Buy Miro");

		assertThat(result.stopReason()).isEqualTo(StopReason.COMPLETED);
		assertThat(result.finishedBy()).isEqualTo("intake");
		assertThat(result.finalAnswer()).isEqualTo("intake closes the case");
		assertThat(result.path()).extracting(Hop::agent).containsExactly("intake", "security", "finance");
		assertThat(result.caseFile()).containsEntry("security", "security notes")
			.containsEntry("finance", "finance notes");
		// The peer that closes the case sees what everybody else found.
		assertThat(intake.lastPrompt).contains("security notes").contains("finance notes");
	}

	@Test
	void stopsPingPongBetweenTwoPeers() {
		ScriptedPeer legal = new ScriptedPeer("legal", "security", "security", "security", "security");
		ScriptedPeer security = new ScriptedPeer("security", "legal", "legal", "legal", "legal");
		ScriptedPeer finance = new ScriptedPeer("finance");

		SwarmResult result = new Swarm(List.of(legal, security, finance), 20).run("legal", "Buy Notion");

		assertThat(result.stopReason()).isEqualTo(StopReason.PING_PONG_DETECTED);
		assertThat(result.path()).hasSize(Swarm.PING_PONG_WINDOW);
		assertThat(result.finalAnswer()).contains("Escalated to a human");
	}

	@Test
	void enforcesTheHandoffBudget() {
		ScriptedPeer a = new ScriptedPeer("a", "b", "b", "b");
		ScriptedPeer b = new ScriptedPeer("b", "c", "c", "c");
		ScriptedPeer c = new ScriptedPeer("c", "a", "a", "a");

		SwarmResult result = new Swarm(List.of(a, b, c), 3).run("a", "Loop forever");

		assertThat(result.stopReason()).isEqualTo(StopReason.MAX_HANDOFFS_REACHED);
		assertThat(result.path()).hasSize(4);
	}

	@Test
	void stopsOnHandoffToUnknownPeer() {
		SwarmResult result = new Swarm(List.of(new ScriptedPeer("intake", "ceo")), 8).run("intake", "Buy Slack");

		assertThat(result.stopReason()).isEqualTo(StopReason.UNKNOWN_AGENT);
		assertThat(result.finishedBy()).isEqualTo("intake");
	}

}

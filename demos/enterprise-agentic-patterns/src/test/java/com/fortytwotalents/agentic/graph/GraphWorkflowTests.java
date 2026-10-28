package com.fortytwotalents.agentic.graph;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.graph.GraphWorkflow.GraphRun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class GraphWorkflowTests {

	@Test
	void runsDiamondInReadyBatchesAndPassesOutputsDownstream() {
		GraphRun run = GraphWorkflow.builder()
			.node("a", state -> state.input(Integer.class) + 1)
			.node("b", state -> state.output("a", Integer.class) * 10)
			.node("c", state -> state.output("a", Integer.class) * 100)
			.node("d", state -> state.output("b", Integer.class) + state.output("c", Integer.class))
			.edge("a", "b")
			.edge("a", "c")
			.edge("b", "d")
			.edge("c", "d")
			.build()
			.run(1);

		assertThat(run.batches()).containsExactly(List.of("a"), List.of("b", "c"), List.of("d"));
		assertThat(run.outputs()).containsEntry("d", 220);
	}

	@Test
	void runsNodesOfOneBatchConcurrently() {
		// Both nodes wait for each other: this only finishes if they really run in
		// parallel.
		CountDownLatch latch = new CountDownLatch(2);
		GraphWorkflow.Node waitForSibling = state -> {
			latch.countDown();
			try {
				return latch.await(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
		};
		GraphRun run = GraphWorkflow.builder()
			.node("left", waitForSibling)
			.node("right", waitForSibling)
			.batchTimeout(Duration.ofSeconds(10))
			.build()
			.run("go");

		assertThat(run.outputs()).containsEntry("left", true).containsEntry("right", true);
	}

	@Test
	void rejectsCyclesAtBuildTime() {
		var builder = GraphWorkflow.builder()
			.node("a", state -> "a")
			.node("b", state -> "b")
			.edge("a", "b")
			.edge("b", "a");

		assertThatIllegalStateException().isThrownBy(builder::build).withMessageContaining("cycle");
	}

	@Test
	void rejectsEdgesToUnknownNodes() {
		var builder = GraphWorkflow.builder().node("a", state -> "a");

		assertThatIllegalArgumentException().isThrownBy(() -> builder.edge("a", "missing"));
	}

	@Test
	void rejectsDuplicateNodes() {
		var builder = GraphWorkflow.builder().node("a", state -> "a");

		assertThatIllegalArgumentException().isThrownBy(() -> builder.node("a", state -> "again"));
	}

}

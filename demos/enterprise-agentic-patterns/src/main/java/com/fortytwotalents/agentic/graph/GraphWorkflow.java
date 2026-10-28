package com.fortytwotalents.agentic.graph;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortytwotalents.agentic.core.Parallel;

/**
 * The topology decides who runs, not the LLM.
 *
 * Nodes run in ready batches: every node whose predecessors are all done runs in parallel
 * on virtual threads; fan-in nodes wait for all their predecessors. A node can be an LLM
 * agent or plain code ("Tool as Agent"); the graph does not care.
 */
public final class GraphWorkflow {

	private static final Logger log = LoggerFactory.getLogger(GraphWorkflow.class);

	@FunctionalInterface
	public interface Node {

		Object execute(GraphState state);

	}

	public record GraphRun(List<List<String>> batches, Map<String, Object> outputs) {
	}

	/**
	 * Read-only view for nodes: the workflow input plus the outputs of finished nodes.
	 */
	public static final class GraphState {

		private final Object input;

		private final Map<String, Object> outputs = new ConcurrentHashMap<>();

		GraphState(Object input) {
			this.input = input;
		}

		public <T> T input(Class<T> type) {
			return type.cast(input);
		}

		public <T> T output(String nodeId, Class<T> type) {
			Object value = outputs.get(nodeId);
			if (value == null) {
				throw new IllegalStateException("Node '" + nodeId + "' has not produced an output yet");
			}
			return type.cast(value);
		}

	}

	private final Map<String, Node> nodes;

	private final Map<String, Set<String>> predecessors;

	private final int maxConcurrency;

	private final Duration batchTimeout;

	private GraphWorkflow(Builder builder) {
		this.nodes = Map.copyOf(builder.nodes);
		this.predecessors = builder.predecessors;
		this.maxConcurrency = builder.maxConcurrency;
		this.batchTimeout = builder.batchTimeout;
	}

	public GraphRun run(Object input) {
		GraphState state = new GraphState(input);
		List<List<String>> batches = new ArrayList<>();
		Set<String> done = new LinkedHashSet<>();
		while (done.size() < predecessors.size()) {
			List<String> batch = readyBatch(done);
			log.atInfo().addArgument(() -> batches.size() + 1).addArgument(batch).log("[graph] batch {} -> {}");
			List<Object> results = Parallel.map(batch, id -> nodes.get(id).execute(state), maxConcurrency,
					batchTimeout);
			for (int i = 0; i < batch.size(); i++) {
				state.outputs.put(batch.get(i), results.get(i));
			}
			done.addAll(batch);
			batches.add(batch);
		}
		return new GraphRun(batches, Map.copyOf(state.outputs));
	}

	private List<String> readyBatch(Set<String> done) {
		List<String> batch = predecessors.entrySet()
			.stream()
			.filter(e -> !done.contains(e.getKey()) && done.containsAll(e.getValue()))
			.map(Map.Entry::getKey)
			.toList();
		if (batch.isEmpty()) {
			throw new IllegalStateException("Graph contains a cycle");
		}
		return batch;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private final Map<String, Node> nodes = new LinkedHashMap<>();

		private final Map<String, Set<String>> predecessors = new LinkedHashMap<>();

		private int maxConcurrency = 4;

		private Duration batchTimeout = Duration.ofMinutes(3);

		public Builder node(String id, Node node) {
			if (nodes.putIfAbsent(id, node) != null) {
				throw new IllegalArgumentException("Duplicate node: " + id);
			}
			predecessors.put(id, new LinkedHashSet<>());
			return this;
		}

		public Builder edge(String from, String to) {
			if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
				throw new IllegalArgumentException("Unknown node in edge " + from + " -> " + to);
			}
			predecessors.get(to).add(from);
			return this;
		}

		public Builder maxConcurrency(int maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
			return this;
		}

		public Builder batchTimeout(Duration batchTimeout) {
			this.batchTimeout = batchTimeout;
			return this;
		}

		public GraphWorkflow build() {
			assertAcyclic();
			return new GraphWorkflow(this);
		}

		private void assertAcyclic() {
			Set<String> done = new LinkedHashSet<>();
			while (done.size() < predecessors.size()) {
				List<String> ready = predecessors.entrySet()
					.stream()
					.filter(e -> !done.contains(e.getKey()) && done.containsAll(e.getValue()))
					.map(Map.Entry::getKey)
					.toList();
				if (ready.isEmpty()) {
					throw new IllegalStateException("Graph contains a cycle");
				}
				done.addAll(ready);
			}
		}

	}

}

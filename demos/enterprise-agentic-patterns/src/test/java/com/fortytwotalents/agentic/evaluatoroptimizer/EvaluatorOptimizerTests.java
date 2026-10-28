package com.fortytwotalents.agentic.evaluatoroptimizer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.evaluatoroptimizer.EvaluatorOptimizer.Outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EvaluatorOptimizerTests {

	/** Candidate is a number, verdict is its score; passing means score >= 80. */
	private static EvaluatorOptimizer<Integer, Integer> loop(int start, int step, int maxRounds,
			List<Integer> optimized) {
		return new EvaluatorOptimizer<>(() -> start, candidate -> candidate, (candidate, verdict) -> {
			optimized.add(candidate);
			return candidate + step;
		}, verdict -> verdict >= 80, verdict -> verdict, maxRounds);
	}

	@Test
	void stopsAsSoonAsTheJudgePasses() {
		List<Integer> optimized = new ArrayList<>();

		Outcome<Integer, Integer> outcome = loop(50, 20, 5, optimized).run();

		assertThat(outcome.passed()).isTrue();
		assertThat(outcome.best()).isEqualTo(90);
		assertThat(outcome.rounds()).hasSize(3);
		assertThat(optimized).containsExactly(50, 70);
	}

	@Test
	void passingFirstDraftNeedsNoOptimizer() {
		List<Integer> optimized = new ArrayList<>();

		Outcome<Integer, Integer> outcome = loop(95, 1, 3, optimized).run();

		assertThat(outcome.passed()).isTrue();
		assertThat(outcome.rounds()).hasSize(1);
		assertThat(optimized).isEmpty();
	}

	@Test
	void isBoundedAndReturnsBestEffortWhenNeverPassing() {
		List<Integer> optimized = new ArrayList<>();

		// The "optimizer" makes things worse: the loop must keep the best round, not the
		// last.
		Outcome<Integer, Integer> outcome = loop(60, -10, 3, optimized).run();

		assertThat(outcome.passed()).isFalse();
		assertThat(outcome.rounds()).hasSize(3);
		assertThat(outcome.best()).isEqualTo(60);
		assertThat(optimized).hasSize(2);
	}

	@Test
	void requiresAtLeastOneRound() {
		assertThatIllegalArgumentException().isThrownBy(() -> loop(0, 0, 0, new ArrayList<>()));
	}

}

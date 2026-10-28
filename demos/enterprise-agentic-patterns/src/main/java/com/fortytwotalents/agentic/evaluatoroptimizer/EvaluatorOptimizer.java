package com.fortytwotalents.agentic.evaluatoroptimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate, judge, improve, repeat, but not forever.
 *
 * Deliberately LLM-agnostic: the generator, judge and optimizer are just functions, so
 * the loop itself is unit-tested without a model.
 *
 * @param <D> the candidate (draft)
 * @param <V> the verdict
 */
public final class EvaluatorOptimizer<D, V> {

	private static final Logger log = LoggerFactory.getLogger(EvaluatorOptimizer.class);

	public record Round<D, V>(int round, D candidate, V verdict) {
	}

	public record Outcome<D, V>(D best, V bestVerdict, boolean passed, List<Round<D, V>> rounds) {
	}

	/** Gives the optimizer the candidate and the judge's verdict. */
	@FunctionalInterface
	public interface Optimizer<D, V> {

		D improve(D candidate, V verdict);

	}

	private final Supplier<D> generator;

	private final Function<D, V> evaluator;

	private final Optimizer<D, V> optimizer;

	private final Predicate<V> passed;

	private final ToIntFunction<V> score;

	private final int maxRounds;

	public EvaluatorOptimizer(Supplier<D> generator, Function<D, V> evaluator, Optimizer<D, V> optimizer,
			Predicate<V> passed, ToIntFunction<V> score, int maxRounds) {
		if (maxRounds < 1) {
			throw new IllegalArgumentException("maxRounds must be >= 1");
		}
		this.generator = generator;
		this.evaluator = evaluator;
		this.optimizer = optimizer;
		this.passed = passed;
		this.score = score;
		this.maxRounds = maxRounds;
	}

	public Outcome<D, V> run() {
		List<Round<D, V>> rounds = new ArrayList<>();
		D candidate = generator.get();
		for (int round = 1; round <= maxRounds; round++) {
			V verdict = evaluator.apply(candidate);
			rounds.add(new Round<>(round, candidate, verdict));
			log.atInfo()
				.addArgument(round)
				.addArgument(maxRounds)
				.addArgument(() -> score.applyAsInt(verdict))
				.addArgument(() -> passed.test(verdict))
				.log("[evaluator-optimizer] round {}/{} score={} passed={}");
			if (passed.test(verdict)) {
				return new Outcome<>(candidate, verdict, true, rounds);
			}
			if (round < maxRounds) {
				candidate = optimizer.improve(candidate, verdict);
			}
		}
		// Bounded by design: return the best effort, flagged as not passed.
		Round<D, V> best = rounds.stream()
			.max(Comparator.comparingInt((Round<D, V> r) -> score.applyAsInt(r.verdict())))
			.orElseThrow();
		return new Outcome<>(best.candidate(), best.verdict(), false, rounds);
	}

}

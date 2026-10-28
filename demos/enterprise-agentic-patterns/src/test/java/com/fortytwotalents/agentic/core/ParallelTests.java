package com.fortytwotalents.agentic.core;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParallelTests {

	@Test
	void keepsInputOrderAndRespectsTheConcurrencyLimit() {
		AtomicInteger running = new AtomicInteger();
		AtomicInteger peak = new AtomicInteger();

		List<Integer> results = Parallel.map(List.of(5, 4, 3, 2, 1, 0), n -> {
			peak.accumulateAndGet(running.incrementAndGet(), Math::max);
			sleep(20L * n);
			running.decrementAndGet();
			return n * n;
		}, 2, Duration.ofSeconds(5));

		assertThat(results).containsExactly(25, 16, 9, 4, 1, 0);
		assertThat(peak.get()).isLessThanOrEqualTo(2);
	}

	@Test
	void rethrowsTheOriginalRuntimeException() {
		assertThatThrownBy(() -> Parallel.map(List.of(1), n -> {
			throw new UnsupportedOperationException("boom");
		}, 1, Duration.ofSeconds(1))).isInstanceOf(UnsupportedOperationException.class).hasMessage("boom");
	}

	@Test
	void enforcesTheTimeBudget() {
		assertThatIllegalStateException()
			.isThrownBy(() -> Parallel.map(List.of(1), n -> sleep(2_000), 1, Duration.ofMillis(50)))
			.withMessageContaining("time budget");
	}

	private static int sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return 0;
	}

}

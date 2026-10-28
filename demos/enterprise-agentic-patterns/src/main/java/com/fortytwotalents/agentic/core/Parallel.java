package com.fortytwotalents.agentic.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Bounded fan-out on virtual threads.
 *
 * Virtual threads make waiting on Bedrock cheap; the semaphore keeps us under the
 * account's requests-per-minute quota. Results keep the order of the inputs.
 */
public final class Parallel {

	private Parallel() {
	}

	public static <T, R> List<R> map(List<T> inputs, Function<T, R> task, int maxConcurrency, Duration timeout) {
		Semaphore permits = new Semaphore(maxConcurrency);
		try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<R>> futures = new ArrayList<>(inputs.size());
			for (T input : inputs) {
				futures.add(pool.submit(() -> {
					permits.acquire();
					try {
						return task.apply(input);
					}
					finally {
						permits.release();
					}
				}));
			}
			long deadline = System.nanoTime() + timeout.toNanos();
			List<R> results = new ArrayList<>(futures.size());
			for (Future<R> future : futures) {
				results.add(await(future, deadline));
			}
			return results;
		}
	}

	private static <R> R await(Future<R> future, long deadlineNanos) {
		try {
			return future.get(Math.max(0, deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
		}
		catch (TimeoutException ex) {
			future.cancel(true);
			throw new IllegalStateException("Parallel task exceeded its time budget", ex);
		}
		catch (ExecutionException ex) {
			if (ex.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw new IllegalStateException(ex.getCause());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for parallel task", ex);
		}
	}

}

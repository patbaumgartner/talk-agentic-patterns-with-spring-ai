package com.fortytwotalents.agentic.core;

import java.util.Comparator;
import java.util.List;

/**
 * Shared risk scale. Ordinal order matters: aggregations take the maximum.
 */
public enum RiskLevel {

	LOW, MEDIUM, HIGH, CRITICAL;

	public static RiskLevel highest(List<RiskLevel> levels) {
		return levels.stream().max(Comparator.naturalOrder()).orElse(LOW);
	}

}

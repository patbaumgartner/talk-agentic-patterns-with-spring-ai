package com.fortytwotalents.agentic.graph;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fortytwotalents.agentic.graph.Kyc.ScreeningResult;

/**
 * Tool as Agent: sanctions screening is a regulated, deterministic lookup. It joins the
 * graph as a node like any LLM agent, but it never hallucinates and it is reproducible
 * for the auditor.
 *
 * Stand-in for SECO / OFAC / EU consolidated lists with a naive token match.
 */
@Component
public class SanctionsScreening {

	static final String LIST_VERSION = "DEMO-CONSOLIDATED-2026-10-26";

	private static final List<String> SANCTIONED_PERSONS = List.of("Ivan Sergeyevich Volkov", "Viktor Antonov Petrov",
			"Leyla Rahimova Aliyeva", "Global Trade Holdings FZE");

	public ScreeningResult screen(String fullName) {
		Set<String> applicant = tokens(fullName);
		List<String> matches = SANCTIONED_PERSONS.stream().filter(entry -> matches(applicant, tokens(entry))).toList();
		return new ScreeningResult(!matches.isEmpty(), matches, LIST_VERSION);
	}

	/** Hit when at least two name tokens and two thirds of the listed tokens match. */
	private static boolean matches(Set<String> applicant, Set<String> listed) {
		long common = listed.stream().filter(applicant::contains).count();
		return common >= 2 && common * 3 >= listed.size() * 2L;
	}

	static Set<String> tokens(String name) {
		String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return Arrays.stream(ascii.toLowerCase(Locale.ROOT).split("[^a-z]+"))
			.filter(token -> token.length() > 1)
			.collect(Collectors.toSet());
	}

}

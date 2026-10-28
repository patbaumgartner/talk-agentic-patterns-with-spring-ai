package com.fortytwotalents.agentic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import com.fortytwotalents.agentic.chain.ContractReview.ContractReviewRequest;
import com.fortytwotalents.agentic.evaluatoroptimizer.BreachNotification.IncidentFacts;
import com.fortytwotalents.agentic.graph.Kyc.KycApplication;
import com.fortytwotalents.agentic.mono.ServiceDeskAgent.ServiceDeskRequest;
import com.fortytwotalents.agentic.orchestrator.Incident.IncidentReport;
import com.fortytwotalents.agentic.parallelization.DueDiligence.DueDiligenceRequest;
import com.fortytwotalents.agentic.routing.Triage.CustomerEmail;
import com.fortytwotalents.agentic.swarm.Procurement.ProcurementRequest;
import com.fortytwotalents.agentic.taskexecution.InvoiceExtractionService.InvoiceExtractionRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** The samples are what you paste on stage: they must match the request records. */
class SamplesTests {

	private static final Map<String, Class<?>> REQUEST_TYPE_BY_PREFIX = Map.of( //
			"1-", InvoiceExtractionRequest.class, //
			"2-", ServiceDeskRequest.class, //
			"3-", ContractReviewRequest.class, //
			"4-", DueDiligenceRequest.class, //
			"5-", IncidentFacts.class, //
			"6-", CustomerEmail.class, //
			"7-", KycApplication.class, //
			"8-", IncidentReport.class, //
			"9-", ProcurementRequest.class);

	private final JsonMapper mapper = JsonMapper.builder()
		.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
		.build();

	private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

	private final Validator validator = VALIDATOR_FACTORY.getValidator();

	@Test
	void everySampleDeserialisesIntoItsRequestRecord() throws Exception {
		List<Path> samples;
		try (Stream<Path> files = Files.list(Path.of("samples"))) {
			samples = files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
		}

		assertThat(samples).hasSizeGreaterThanOrEqualTo(REQUEST_TYPE_BY_PREFIX.size());
		for (Path sample : samples) {
			Class<?> type = REQUEST_TYPE_BY_PREFIX.get(sample.getFileName().toString().substring(0, 2));
			assertThat(type).as("request type for " + sample).isNotNull();
			Object request = mapper.readValue(sample.toFile(), type);
			assertThat(request).as(sample.toString()).hasNoNullFieldsOrProperties();
			assertThat(validator.validate(request)).as("constraint violations in " + sample).isEmpty();
		}
	}

}

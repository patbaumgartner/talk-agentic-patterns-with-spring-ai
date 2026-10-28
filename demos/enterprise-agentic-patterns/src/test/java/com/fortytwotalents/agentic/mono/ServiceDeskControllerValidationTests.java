package com.fortytwotalents.agentic.mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.fortytwotalents.agentic.mono.ServiceDeskAgent.ServiceDeskResponse;

@WebMvcTest(ServiceDeskController.class)
class ServiceDeskControllerValidationTests {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	ServiceDeskAgent agent;

	@Test
	void validRequestReachesTheAgent() {
		given(agent.handle(any())).willReturn(new ServiceDeskResponse("done", List.of(), List.of()));

		assertThat(post("""
				{"employeeEmail": "anna.meier@acme.example", "message": "Laptop broken"}"""))
			.hasStatusOk();
	}

	@Test
	void missingFieldIsRejectedWithoutCallingTheLlm() {
		assertThat(post("""
				{"employeeEmail": "anna.meier@acme.example"}"""))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("message must not be blank");
		verifyNoInteractions(agent);
	}

	@Test
	void invalidEmailAndBlankMessageAreBothReported() {
		assertThat(post("""
				{"employeeEmail": "not-an-email", "message": "  "}"""))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("employeeEmail must be a well-formed email address; message must not be blank");
		verifyNoInteractions(agent);
	}

	@Test
	void malformedJsonIsBadRequest() {
		assertThat(post("{oops")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.title")
			.isEqualTo("Invalid request");
		verifyNoInteractions(agent);
	}

	private MockMvcTester.MockMvcRequestBuilder post(String json) {
		return mvc.post().uri("/api/mono/service-desk").contentType(MediaType.APPLICATION_JSON).content(json);
	}

}

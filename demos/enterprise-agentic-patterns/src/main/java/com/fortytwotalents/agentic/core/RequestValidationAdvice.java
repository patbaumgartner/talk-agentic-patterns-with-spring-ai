package com.fortytwotalents.agentic.core;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;

/**
 * Rejects bad input with 400 before any LLM call is made, and names the offending fields.
 */
@RestControllerAdvice
class RequestValidationAdvice {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidRequest(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getField() + " " + error.getDefaultMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableRequest(HttpMessageNotReadableException ex) {
		String detail = (ex.getCause() instanceof DatabindException databind && !databind.getPath().isEmpty())
				? fieldPath(databind) + " is missing or has the wrong type"
				: "Request body is missing or is not valid JSON for this pattern";
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request");
		return problem;
	}

	private static String fieldPath(DatabindException ex) {
		StringBuilder path = new StringBuilder();
		for (JacksonException.Reference reference : ex.getPath()) {
			if (reference.getPropertyName() != null) {
				path.append(path.isEmpty() ? "" : ".").append(reference.getPropertyName());
			}
			else if (reference.getIndex() >= 0) {
				path.append('[').append(reference.getIndex()).append(']');
			}
		}
		return path.toString();
	}

}

package com.fortytwotalents.agentic.mono;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fortytwotalents.agentic.core.Agents;

/**
 * Pattern: Mono (one agent, one loop, a handful of tools).
 *
 * Enterprise problem: "I am locked out" is the #1 L1 service desk ticket. One agent with
 * four tools resolves it end-to-end, or escalates when policy says so.
 */
@Service
public class ServiceDeskAgent {

	private static final String SYSTEM_PROMPT = """
			You are the first-level IT service desk agent of ACME Corp.
			Resolve the employee's request using your tools, then reply to the employee.

			Rules:
			- Always identify the employee with lookupEmployee and check getAccountStatus before acting.
			- You may unlock an account only if it is locked and NOT on security hold.
			- If a tool refuses an action, do not retry it. Create a ticket for the right resolver group instead.
			- Never reveal security-hold details to the employee; say that Security will contact them.
			- Reply in 3 to 5 short sentences, in the language of the employee. Mention ticket ids.
			""";

	private final ChatClient agent;

	private final EmployeeDirectory directory;

	public ServiceDeskAgent(Agents agents, EmployeeDirectory directory) {
		this.agent = agents.fast().defaultSystem(SYSTEM_PROMPT).build();
		this.directory = directory;
	}

	public ServiceDeskResponse handle(ServiceDeskRequest request) {
		ServiceDeskTools tools = new ServiceDeskTools(directory);

		// One .call() = N iterations of the agentic loop, until no more tool calls.
		String reply = agent.prompt()
			.user(user -> user.text("""
					Employee e-mail: {email}
					Request: {message}
					""").param("email", request.employeeEmail()).param("message", request.message()))
			.tools(tools)
			.call()
			.content();

		return new ServiceDeskResponse(reply, tools.ticketIds(), tools.auditTrail());
	}

	public record ServiceDeskRequest(@NotBlank @Email String employeeEmail, @NotBlank String message) {
	}

	/** The audit trail is recorded by the tools, not reported by the model. */
	public record ServiceDeskResponse(String reply, List<String> ticketIds, List<String> auditTrail) {
	}

}

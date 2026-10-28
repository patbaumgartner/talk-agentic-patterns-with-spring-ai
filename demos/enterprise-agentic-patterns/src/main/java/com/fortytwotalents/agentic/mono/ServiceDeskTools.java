package com.fortytwotalents.agentic.mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.fortytwotalents.agentic.mono.EmployeeDirectory.Account;

/**
 * The agent's hands. One instance per conversation, so the audit trail belongs to that
 * conversation only.
 *
 * Policy lives HERE, in code: the model can ask to unlock an account on security hold as
 * often as it likes, the tool will refuse.
 */
public class ServiceDeskTools {

	private static final AtomicInteger TICKET_SEQUENCE = new AtomicInteger(4700);

	private final EmployeeDirectory directory;

	private final List<String> auditTrail = new CopyOnWriteArrayList<>();

	private final List<String> ticketIds = new CopyOnWriteArrayList<>();

	public ServiceDeskTools(EmployeeDirectory directory) {
		this.directory = directory;
	}

	@Tool(description = "Look up an employee in the corporate directory by e-mail address")
	public Object lookupEmployee(@ToolParam(description = "Corporate e-mail address") String email) {
		audit("lookupEmployee(%s)".formatted(email));
		return directory.findByEmail(email).<Object>map(e -> e).orElse("No employee found for " + email);
	}

	@Tool(description = "Get the sign-in status of an employee account: lock state, failed logins, MFA, security holds")
	public Object getAccountStatus(@ToolParam(description = "Employee id, e.g. E-1001") String employeeId) {
		audit("getAccountStatus(%s)".formatted(employeeId));
		return directory.account(employeeId).<Object>map(a -> a).orElse("No account for " + employeeId);
	}

	@Tool(description = "Unlock a locked employee account. Refused when the account is on security hold.")
	public String unlockAccount(@ToolParam(description = "Employee id") String employeeId,
			@ToolParam(description = "Why the unlock is justified") String justification) {
		Account account = directory.account(employeeId).orElse(null);
		if (account == null) {
			audit("unlockAccount(%s) REJECTED: unknown account".formatted(employeeId));
			return "REFUSED: unknown account " + employeeId;
		}
		if (account.securityHold()) {
			audit("unlockAccount(%s) REJECTED: security hold".formatted(employeeId));
			return "REFUSED by policy SEC-07: account is on security hold (" + account.securityHoldReason()
					+ "). Only the Security Operations Center may release it. Open a SECURITY ticket.";
		}
		if (!account.locked()) {
			audit("unlockAccount(%s) NO-OP: not locked".formatted(employeeId));
			return "Account " + employeeId + " is not locked.";
		}
		directory.unlock(employeeId);
		audit("unlockAccount(%s) DONE: %s".formatted(employeeId, justification));
		return "Account " + employeeId + " unlocked. Failed login counter reset.";
	}

	@Tool(description = "Create a ticket in the IT service management system and return its id")
	public String createTicket(
			@ToolParam(description = "One of: ACCESS, SECURITY, HARDWARE, SOFTWARE, OTHER") String category,
			@ToolParam(description = "One of: P1, P2, P3, P4") String priority,
			@ToolParam(description = "One-line summary for the resolver group") String summary) {
		String ticketId = "INC-" + TICKET_SEQUENCE.incrementAndGet();
		ticketIds.add(ticketId);
		audit("createTicket(%s, %s) -> %s: %s".formatted(category, priority, ticketId, summary));
		return ticketId;
	}

	public List<String> auditTrail() {
		return List.copyOf(auditTrail);
	}

	public List<String> ticketIds() {
		return List.copyOf(ticketIds);
	}

	private void audit(String entry) {
		auditTrail.add(entry);
	}

}

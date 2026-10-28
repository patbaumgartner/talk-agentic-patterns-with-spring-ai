package com.fortytwotalents.agentic.mono;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Stand-in for Entra ID / LDAP. In-memory so the demo runs anywhere.
 */
@Component
public class EmployeeDirectory {

	public record Employee(String id, String name, String email, String department, String manager) {
	}

	public record Account(String employeeId, boolean locked, int failedLogins, boolean mfaEnrolled,
			boolean securityHold, String securityHoldReason, LocalDate lastPasswordChange) {
	}

	private final Map<String, Employee> employeesByEmail = Map.of( //
			"anna.meier@acme.example",
			new Employee("E-1001", "Anna Meier", "anna.meier@acme.example", "Finance", "Peter Keller"),
			"marco.rossi@acme.example",
			new Employee("E-1002", "Marco Rossi", "marco.rossi@acme.example", "Sales", "Sara Frei"),
			"lea.brunner@acme.example",
			new Employee("E-1003", "Lea Brunner", "lea.brunner@acme.example", "Engineering", "Tom Huber"));

	private final Map<String, Account> accounts = new ConcurrentHashMap<>(Map.of( //
			"E-1001", new Account("E-1001", true, 5, true, false, null, LocalDate.of(2026, 7, 1)), "E-1002",
			new Account("E-1002", true, 9, true, true,
					"Impossible-travel alert: logins from Zurich and Lagos within 20 minutes",
					LocalDate.of(2026, 2, 14)),
			"E-1003", new Account("E-1003", false, 0, false, false, null, LocalDate.of(2025, 11, 3))));

	public Optional<Employee> findByEmail(String email) {
		return Optional.ofNullable(employeesByEmail.get(email.toLowerCase().trim()));
	}

	public Optional<Account> account(String employeeId) {
		return Optional.ofNullable(accounts.get(employeeId));
	}

	public Account unlock(String employeeId) {
		return accounts.computeIfPresent(employeeId, (id, a) -> new Account(id, false, 0, a.mfaEnrolled(),
				a.securityHold(), a.securityHoldReason(), a.lastPasswordChange()));
	}

}

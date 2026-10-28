package com.fortytwotalents.agentic.taskexecution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.taskexecution.Invoice.LineItem;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceValidatorTests {

	private static LineItem line(String qty, String price, String total) {
		return new LineItem("Consulting", new BigDecimal(qty), new BigDecimal(price), new BigDecimal(total));
	}

	@Test
	void consistentInvoiceIsValid() {
		Invoice invoice = new Invoice("R-1", "ACME AG", "CHE-123.456.789", "PO-77", LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31), "CHF", List.of(line("10", "150.00", "1500.00"), line("2", "50", "100")),
				new BigDecimal("1600.00"), new BigDecimal("129.60"), new BigDecimal("1729.60"));

		assertThat(InvoiceValidator.validate(invoice)).isEmpty();
	}

	@Test
	void catchesArithmeticDatesAndMissingPurchaseOrder() {
		Invoice invoice = new Invoice("R-2", "ACME AG", null, " ", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1),
				"CHF", List.of(line("10", "150.00", "1550.00")), new BigDecimal("1500.00"), new BigDecimal("121.50"),
				new BigDecimal("1700.00"));

		assertThat(InvoiceValidator.validate(invoice)).hasSize(5)
			.anySatisfy(issue -> assertThat(issue).startsWith("Line 'Consulting'"))
			.anySatisfy(issue -> assertThat(issue).startsWith("Sum of line items"))
			.anySatisfy(issue -> assertThat(issue).startsWith("Net 1500.00 + VAT"))
			.anySatisfy(issue -> assertThat(issue).startsWith("Due date"))
			.anySatisfy(issue -> assertThat(issue).startsWith("No purchase order"));
	}

}

package com.fortytwotalents.agentic.taskexecution;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Trust, but verify: the LLM extracts, plain Java checks the arithmetic and the business
 * rules before anything is posted to the ERP.
 */
public final class InvoiceValidator {

	private static final BigDecimal TOLERANCE = new BigDecimal("0.05");

	private InvoiceValidator() {
	}

	public static List<String> validate(Invoice invoice) {
		List<String> issues = new ArrayList<>();
		for (Invoice.LineItem item : invoice.lineItems()) {
			BigDecimal expected = item.quantity().multiply(item.unitPrice());
			if (differs(expected, item.total())) {
				issues.add("Line '%s': %s x %s = %s, but invoice says %s".formatted(item.description(), item.quantity(),
						item.unitPrice(), expected, item.total()));
			}
		}
		BigDecimal lineSum = invoice.lineItems()
			.stream()
			.map(Invoice.LineItem::total)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (differs(lineSum, invoice.netAmount())) {
			issues.add("Sum of line items %s does not match net amount %s".formatted(lineSum, invoice.netAmount()));
		}
		if (differs(invoice.netAmount().add(invoice.vatAmount()), invoice.grossAmount())) {
			issues.add("Net %s + VAT %s does not match gross amount %s".formatted(invoice.netAmount(),
					invoice.vatAmount(), invoice.grossAmount()));
		}
		if (invoice.dueDate().isBefore(invoice.issueDate())) {
			issues.add("Due date %s is before issue date %s".formatted(invoice.dueDate(), invoice.issueDate()));
		}
		if (invoice.purchaseOrderNumber() == null || invoice.purchaseOrderNumber().isBlank()) {
			issues.add("No purchase order number: three-way match impossible");
		}
		return issues;
	}

	private static boolean differs(BigDecimal a, BigDecimal b) {
		return a.subtract(b).abs().compareTo(TOLERANCE) > 0;
	}

}

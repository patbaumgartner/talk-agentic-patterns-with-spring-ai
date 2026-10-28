package com.fortytwotalents.agentic.taskexecution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

/**
 * The record is the contract: JSON schema for the model, type for the ERP posting code.
 */
@JsonClassDescription("A supplier invoice extracted from an unstructured document")
public record Invoice(@JsonPropertyDescription("Invoice number exactly as printed") String invoiceNumber,
		@JsonPropertyDescription("Legal name of the supplier") String supplierName,
		@JsonPropertyDescription("Supplier VAT / UID number, omitted if absent") @Nullable String supplierVatId,
		@JsonPropertyDescription("Our purchase order number, omitted if absent") @Nullable String purchaseOrderNumber,
		@JsonPropertyDescription("Issue date, ISO-8601") LocalDate issueDate,
		@JsonPropertyDescription("Due date, ISO-8601. Derive from payment terms if only terms are given") LocalDate dueDate,
		@JsonPropertyDescription("ISO 4217 currency code, e.g. CHF, EUR") String currency, List<LineItem> lineItems,
		@JsonPropertyDescription("Total before VAT") BigDecimal netAmount,
		@JsonPropertyDescription("Total VAT amount") BigDecimal vatAmount,
		@JsonPropertyDescription("Total including VAT") BigDecimal grossAmount) {

	public record LineItem(String description, BigDecimal quantity, BigDecimal unitPrice,
			@JsonPropertyDescription("quantity x unitPrice as printed on the invoice") BigDecimal total) {
	}

}

package com.fortytwotalents.agentic.swarm;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fortytwotalents.agentic.swarm.ProcurementTools.BudgetTools;
import com.fortytwotalents.agentic.swarm.ProcurementTools.ContractTools;
import com.fortytwotalents.agentic.swarm.ProcurementTools.VendorRiskTools;

import static org.assertj.core.api.Assertions.assertThat;

class ProcurementToolsTests {

	@Test
	void approvalLimitsAreCode() {
		BudgetTools budget = new BudgetTools();

		assertThat(budget.checkBudget("CC-4100", new BigDecimal("8000")))
			.satisfies(check -> assertThat(check.withinBudget()).isTrue())
			.satisfies(check -> assertThat(check.approverRequired()).isEqualTo("Team lead"));
		assertThat(budget.checkBudget("cc-4100", new BigDecimal("24000")))
			.satisfies(check -> assertThat(check.withinBudget()).isFalse())
			.satisfies(check -> assertThat(check.approverRequired()).isEqualTo("Head of department"));
		assertThat(budget.checkBudget("CC-2200", new BigDecimal("80000")).approverRequired()).isEqualTo("CFO");
	}

	@Test
	void vendorLookupsAreForgivingAboutSpellingAndSafeForUnknownVendors() {
		assertThat(new VendorRiskTools().vendorRiskProfile("SlideWizard-AI").certifications()).isEmpty();
		assertThat(new ContractTools().dpaStatus("NOTION").status()).isEqualTo("SIGNED");
		assertThat(new ContractTools().dpaStatus("Unknown Startup").status()).isEqualTo("NONE");
	}

}

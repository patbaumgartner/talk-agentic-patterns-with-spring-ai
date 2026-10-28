package com.fortytwotalents.agentic.swarm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.fortytwotalents.agentic.swarm.Procurement.BudgetCheck;
import com.fortytwotalents.agentic.swarm.Procurement.DpaStatus;
import com.fortytwotalents.agentic.swarm.Procurement.VendorProfile;

/**
 * Each peer owns exactly one system of record. Canned data stands in for the vendor risk
 * platform, the contract repository and the ERP.
 */
public final class ProcurementTools {

	private ProcurementTools() {
	}

	private static String key(String vendor) {
		return vendor == null ? "" : vendor.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	public static final class VendorRiskTools {

		private static final Map<String, VendorProfile> PROFILES = Map.of( //
				"miro",
				new VendorProfile("Miro", "EU (Frankfurt)", List.of("SOC 2 Type II", "ISO 27001"), "2026-03", true),
				"notion", new VendorProfile("Notion", "US (Oregon)", List.of("SOC 2 Type II"), "2026-01", true),
				"slidewizardai", new VendorProfile("SlideWizard AI", "unknown", List.of(), "never", true));

		@Tool(description = "Look up the vendor risk profile: hosting region, certifications, last pen test")
		public VendorProfile vendorRiskProfile(@ToolParam(description = "Vendor name") String vendor) {
			return PROFILES.getOrDefault(key(vendor), new VendorProfile(vendor, "unknown", List.of(), "unknown", true));
		}

	}

	public static final class ContractTools {

		private static final Map<String, DpaStatus> DPAS = Map.of( //
				"miro", new DpaStatus("Miro", "SIGNED", "EU hosting, no transfer", "Group agreement 2025"), "notion",
				new DpaStatus("Notion", "SIGNED", "EU SCC + Swiss addendum", "Renewal due 2027"), "slidewizardai",
				new DpaStatus("SlideWizard AI", "NONE", "none", "Vendor refuses custom DPA"));

		@Tool(description = "Check the data processing agreement (DPA) status with a vendor")
		public DpaStatus dpaStatus(@ToolParam(description = "Vendor name") String vendor) {
			return DPAS.getOrDefault(key(vendor), new DpaStatus(vendor, "NONE", "none", "No contract on file"));
		}

	}

	public static final class BudgetTools {

		private static final Map<String, BigDecimal> REMAINING = Map.of( //
				"CC-4100", new BigDecimal("18000"), // Marketing
				"CC-2200", new BigDecimal("150000")); // Engineering

		/** Approval limits are policy, so they live in code, not in a prompt. */
		@Tool(description = "Check the remaining annual budget of a cost center and who must approve the spend")
		public BudgetCheck checkBudget(@ToolParam(description = "Cost center, e.g. CC-4100") String costCenter,
				@ToolParam(description = "Annual cost in CHF") BigDecimal annualCostChf) {
			BigDecimal remaining = REMAINING.getOrDefault(costCenter.toUpperCase(Locale.ROOT), BigDecimal.ZERO);
			String approver = annualCostChf.compareTo(new BigDecimal("50000")) > 0 ? "CFO"
					: annualCostChf.compareTo(new BigDecimal("10000")) > 0 ? "Head of department" : "Team lead";
			return new BudgetCheck(costCenter, remaining, annualCostChf, annualCostChf.compareTo(remaining) <= 0,
					approver);
		}

	}

}

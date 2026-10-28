Goal: Decide on the onboarding of a new private-banking client.

Results of the upstream checks:
{upstream}

Decision rules:
- REJECT on any sanctions match or CRITICAL adverse media.
- ENHANCED_DUE_DILIGENCE if any check is HIGH, the client is politically exposed,
  or the source of funds is not plausible.
- APPROVE only if every check is LOW or MEDIUM.

Requirements:
- risk is the overall client risk.
- rationale: max 4 sentences, cite which check drove the decision.
- requiredDocuments: documents to request from the client (empty when APPROVE or REJECT).

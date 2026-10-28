Goal: Extract the supplier invoice below into the requested JSON structure.

Requirements:
- Copy values exactly as printed. Do NOT correct, recalculate or round any amount.
- Amounts are plain decimal numbers without currency symbols or thousands separators.
- Dates use ISO-8601 (yyyy-MM-dd). If only payment terms are given (e.g. "30 days net"),
  derive the due date from the issue date.
- Omit optional fields that are not present (do not write null). Never invent values.
- Currency is an ISO 4217 code.

Invoice document:
<<<
{document}
>>>

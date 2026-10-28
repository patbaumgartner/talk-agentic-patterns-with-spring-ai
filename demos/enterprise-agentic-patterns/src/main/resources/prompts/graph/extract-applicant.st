Goal: Extract the applicant data from a private-banking account opening form.

Requirements:
- Copy facts as written; omit optional fields that are missing (do not write null).
- expectedMonthlyDepositsChf is a plain number in CHF. Convert ranges to their upper bound.
- declaredPoliticallyExposed reflects ONLY what the applicant declared.

Application form:
<<<
{form}
>>>

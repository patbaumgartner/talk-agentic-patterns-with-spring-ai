Goal: Extract the commercial and legal key terms from the supplier contract below.

Requirements:
- Report what the contract SAYS, not what it should say.
- If there is no liability cap, set unlimitedLiability to true and omit liabilityCapAmount.
- Convert notice periods and payment terms to days (1 month = 30 days).
- governingLaw is the country name only, e.g. "Switzerland", "Delaware, USA".
- notableClauses lists unusual clauses a lawyer should read (max 5, one sentence each).

Contract:
<<<
{contract}
>>>

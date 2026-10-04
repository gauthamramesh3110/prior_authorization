# Demonstration policies and scenarios

These policies belong to a fictional plan associated with the Synthea Humana payer identity. They are inspired by published clinical/coverage guidance, not actual Humana benefit or authorization rules. Current sources are used as design inspiration for historical synthetic requests, not as historical coverage determinations. All PA and benefit flags are demo choices.

## Clinical policies

| Service | Demo rule | What it exercises |
| --- | --- | --- |
| Bone density monitoring, 312681000 | ANY: osteoporosis 64859006 or pathological fracture due to osteoporosis 443165006 | Either condition can satisfy the rule |
| Diagnostic spirometry, 127783003 | ANY: chronic obstructive bronchitis 185086009, emphysema 87433001, or childhood asthma 233678006 | Multiple alternative diagnoses |
| ICD teaching subset, 447365002 | ALL: chronic congestive heart failure 88805009 and latest ejection fraction 10230-1 <= 35 % | Mixed condition/observation criteria and numeric boundary |
| Coronary bypass, 232717009; cardioversion, 180325003 | MANUAL_REVIEW | Complex clinical decisions remain with a reviewer |

The ICD rule is deliberately simplified for the evaluator exercise. CMS also requires specific cardiomyopathy/heart-failure characteristics, waiting periods, shared decision making, and other checks. CRT-specific requirements are not modeled. These two demo criteria cannot establish real eligibility. A real authorization implementation would route this incomplete clinical assessment to manual review.

Bone density rules do not model medication monitoring, repeat-test intervals, or ordering requirements. Spirometry rules do not model symptoms or testing frequency. Real PA requirements also depend on plan and setting; the seed has neither a real contract nor CPT-level policy mapping.

Sources:
- [CMS bone mass measurement LCD](https://www.cms.gov/medicare-coverage-database/view/lcd.aspx?lcdId=39268)
- [CMS respiratory care LCD](https://www.cms.gov/medicare-coverage-database/view/lcd.aspx?lcdid=34149&ver=52)
- [CMS ICD NCD](https://www.cms.gov/medicare-coverage-database/view/ncd.aspx?NCDId=110)
- [Humana authorization submission guidance](https://provider.humana.com/coverage-claims/prior-authorizations)
- [Aetna cosmetic procedure policy](https://www.aetna.com/cpb/medical/data/1_99/0031.html): inspiration for a purely cosmetic exclusion fixture; reconstructive/functional care is a different indication.

## Testing the workflow

Expected outcomes below follow the Phase 1 design. They are test targets, not a claim that the application evaluator is implemented.

Start with a covered historical patient and an in-network provider/organization pair. Keep submittedAt fixed while adding evidence. Unless stated otherwise, evidence timestamps must be before submittedAt. Evidence added later can report an earlier fact.

| Scenario | Input or action | Expected outcome |
| --- | --- | --- |
| Initial acceptance | Valid request | SUBMITTED / NULL |
| ANY satisfied | Bone scan with one qualifying osteoporosis condition | APPROVED / AUTO_APPROVED |
| ALL satisfied | ICD demo with heart failure and latest EF 35 % or less | APPROVED / AUTO_APPROVED |
| Numeric boundary fails | ICD demo with heart failure and latest EF 35.1 % | PENDING / CRITERIA_NOT_MET |
| Required evidence missing | ICD demo with heart failure but no EF measurement | PENDING / AWAITING_EVIDENCE |
| No matching condition | Bone scan with neither eligible condition | PENDING / AWAITING_EVIDENCE |
| Evidence inconclusive | ICD demo with nonnumeric EF, wrong unit, or conflicting latest measurements | PENDING / MANUAL_REVIEW_REQUIRED |
| Latest result governs | ICD demo: earlier EF 32 %, latest EF 40 % | PENDING / CRITERIA_NOT_MET |
| After-cutoff evidence | ICD demo: only EF measurement after original submission | PENDING / AWAITING_EVIDENCE |
| Manual policy | Bypass or cardioversion | PENDING / MANUAL_REVIEW_REQUIRED |
| Policy missing | Immunotherapy 180256009 | PENDING / MANUAL_REVIEW_REQUIRED |
| Policy has zero criteria | Polypectomy 274031008 | PENDING / MANUAL_REVIEW_REQUIRED |
| Service configuration missing | Throat culture 117015009 (not in configuration.json) | PENDING / MANUAL_REVIEW_REQUIRED |
| PA not required | Colonoscopy 73761001 or medication reconciliation 430193006 | APPROVED / PRIOR_AUTH_NOT_REQUIRED; no review |
| Benefit excluded | DEMO-COSMETIC | REJECTED / SERVICE_EXCLUDED; no review |
| Coverage inactive | Known patient, submission after their last Humana coverage span | REJECTED / COVERAGE_INACTIVE; no review |
| No enrollment | Add a patient reference with no coverage for this plan | REJECTED / NOT_COVERED; no review |
| Out of network | Use the out-of-network pair below with active coverage | REJECTED / OUT_OF_NETWORK; no review |
| System evidence request fulfilled | Supply missing qualifying pre-submission evidence | SUBMITTED / EVIDENCE_UPDATED, then reevaluation |
| Reviewer evidence request fulfilled | Supply additional pre-submission evidence | PENDING / EVIDENCE_UPDATED, then human review |
| Reviewer approval | Claim a pending manual review and approve | APPROVED / MANUAL_APPROVED |
| Reviewer rejection | Claim a pending manual review and reject | REJECTED / MANUAL_REJECTED |

For the ALL-satisfied ICD case, construct a clearly labeled synthetic request payload using a seeded covered patient. The cohort contains both heart-failure diagnoses and EF values <= 35 %, but I did not find a naturally covered patient/time satisfying both using the latest-measurement rule. Do not describe a constructed combination as an untouched Synthea record.

Other targeted variations (35/35.1 boundaries, wrong units, conflicts, and malformed input) are deliberately modified test payloads. Never edit raw CSVs to create these cases. Unknown patient/provider identities and malformed JSON should fail validation before a workflow row is written.

## Unmodified Synthea examples

Plan ID: d1a8f693-cb70-5006-9e11-e093571143f9.

In-network provider: 3421aa75-dec7-378d-a9e0-0bc764e4cb0d.
Organization: ef58ea08-d883-3957-8300-150554edc8fb.

Out-of-network provider: 446d1609-858f-3a54-8a52-0c4eacedd00e.
Organization: f1fbcbfb-fcfa-3bd2-b7f4-df20f1b3c3a4.

These provider pairs are demo choices from the seeded references; they need not be the clinicians from the original condition encounter.

For an ANY bone-density match:
- Patient: 87f05059-de42-4630-a35b-edb53d880640.
- submittedAt: 1982-12-08T00:00:00Z.
- Condition 64859006, startDate 1982-12-07T00:00:00Z, endDate null.
- The patient has active Humana coverage at this submission time.

For an ANY spirometry match:
- Patient: e8ae3951-69d1-4257-a22b-ed03327260d7.
- submittedAt: 1999-07-06T00:00:00Z.
- Condition 233678006, startDate 1999-07-05T00:00:00Z, endDate 2015-09-03T00:00:00Z.
- The patient has active Humana coverage at this submission time.

Dates converted from condition CSV dates use midnight UTC; that is a normalization convention, not original timestamp precision.

## Reloading configuration

Rerun seed.py after editing configuration.json. Current policy, criterion, and service rows are refreshed; reference and workflow rows are preserved. Policies or criteria removed from the JSON are not automatically deleted. Unused policies from an older seed may remain, but updated plan_service rows point to the current policies. To remove a criterion from an existing policy, use a new policy name so it receives a new ID and the service links to that policy.

---
document_id: HHI-MP-001
policy_id: 70000000-0000-4000-8000-000000000001
version: "1.0.0"
title: "Bone Density Measurement for Osteoporosis"
payer_name: "Harbor Health Insurance"
plan_name: "Harbor Choice PPO"
plan_id: "20000000-0000-4000-8000-000000000001"
service_code: "312681000"
service_code_system: "SNOMED-CT"
document_status: "FICTIONAL_DEMONSTRATION"
published_date: "2026-10-06"
effective_from: "2026-10-01"
effective_to: null
last_review_date: "2026-10-06"
review_due_date: "2027-10-06"
seed_review_mode: "AUTO_APPROVAL_ELIGIBLE"
seed_match: "ANY"
---

# HHI-MP-001: Bone Density Measurement for Osteoporosis

**Harbor Health Insurance | Harbor Choice PPO**

> FICTIONAL DEMONSTRATION POLICY. Prepared for a synthetic-data software learning project. This is not an actual insurer policy, a clinically validated guideline, or medical advice. Published and effective dates are fictional document metadata.

| Document control | Detail |
| --- | --- |
| Policy number | HHI-MP-001 |
| Policy record ID | 70000000-0000-4000-8000-000000000001 |
| Version | 1.0.0 |
| Effective period | October 1, 2026 onward; no document end date assigned |
| Publication / last document review | October 6, 2026 |
| Planned document review | October 6, 2027 |
| Issuer / applicable product | Harbor Health Insurance / Harbor Choice PPO |
| Benefit and authorization configuration | Covered service; prior authorization required in the seeded plan |
| Seeded review designation | AUTO_APPROVAL_ELIGIBLE |
| Seeded criterion relationship | ANY |
| Scope of evidence | Structured case evidence and any separately supplied supporting narrative |

## 1. Application and benefit interpretation

This document applies to requests mapped to service **312681000**, **Bone density scan (procedure)**, under Harbor Choice PPO. It describes the limited clinical-review policy associated with policy record 70000000-0000-4000-8000-000000000001.

Member eligibility, coverage period, provider organization, network participation, service benefit status, and prior-authorization requirements are determined through the plan's separate benefit configuration. A policy match does not create coverage when those prerequisites are not satisfied. A clinical reference or procedure code alone does not establish entitlement to payment.

Use the applicable document version for review. This document's publication dates do not themselves implement policy version selection in the application. Where applicable-version information is unavailable or inconsistent, disclose that limitation rather than claiming that a different version governs the case.

## 2. Clinical background and review purpose

Bone density measurement can contribute to assessment and follow-up of skeletal health. The reason for testing, available prior measurements, and the clinical question should be documented. Aetna's published bone-mass policy illustrates that actual payer policies contain more indications and technique-specific qualifications than the two indications used in this demonstration. [R1]

The procedure and evidence descriptions in this document follow the supplied synthetic-data terminology. A code in section 9 defines the scope of this demonstration; it is not a complete billing-code crosswalk. Clinical context helps organize an individual assessment but does not silently expand the configured eligibility rules.

## 3. Policy statement and configured criteria

### 3.1 Qualifying indications

The configured indication requirement is satisfied when **at least one** of the following conditions is documented as active at the evidence assessment time:

| Section | Criterion ID | Evidence | Code | Requirement |
| --- | --- | --- | --- | --- |
| HHI-MP-001-3.1.a | b0000000-0000-4000-8000-000000000001 | Condition | 64859006 | Osteoporosis (disorder) is present |
| HHI-MP-001-3.1.b | b0000000-0000-4000-8000-000000000002 | Condition | 443165006 | Pathological fracture due to osteoporosis (disorder) is present |

The relationship between these indications is **OR**, not AND. Documentation of either indication can satisfy this limited indication rule. A pathological fracture diagnosis is not additionally required when qualifying osteoporosis is already documented.

### 3.2 Requests outside the configured indications

An absent, future-dated, or ended condition does not establish an active qualifying indication. Request relevant additional evidence when neither indication can be established. Do not infer that the member has no osteoporosis simply because it was not submitted.

### 3.3 Additional restrictions

No age threshold, prior-treatment duration, bone-density value, scan interval, or modality-specific exclusion is defined by this document's seeded rule. Additional clinical context in section 4 does not create such a restriction.

## 4. Supporting clinical review considerations

The following questions provide context for individual review. They are **not additional executable criteria** and do not establish unlisted denial thresholds.

### 4.1 Review consideration

Explain whether the requested measurement is an initial assessment, treatment follow-up, or evaluation after a documented fracture.

### 4.2 Review consideration

Identify the proposed measurement method and anatomical site where available. The service mapping alone does not identify a specific technique.

### 4.3 Review consideration

Summarize prior bone-density reports and why a repeat measurement would address the stated clinical question.

### 4.4 Review consideration

Where the fracture pathway is used, identify the supporting diagnosis and report; distinguish a documented osteoporosis-related fracture from an unrelated traumatic injury.

A reviewer may seek clarification relevant to a consideration. The record should explain why it is needed for this case. Do not assume that an unavailable report, consultation, or treatment history is evidence that the corresponding clinical event did not occur.

## 5. Documentation for review

### 5.1 Evidence supporting the configured pathway

Provide the evidence required by section 3 for any route the submission relies on. Numeric observations require their code, value, unit, and recording time; active-condition routes require their coded condition and dates. For policies with no executable pathway, submit a narrative sufficient for individual review.

### 5.2 Supporting records

The following items form a practical review packet. Items described as available or relevant are supporting context, not new automatic prerequisites:

- A referring clinician's summary of the clinical question and reason for testing.
- For the chosen indication, the submitted condition code, start date, and end date if applicable.
- Where available, prior imaging or bone-density reports and information about the proposed site and method.
- A clarification when the narrative and coded diagnosis describe different conditions.

### 5.3 Record integrity

Identify the requested service, quantity, requested date, and the source of submitted clinical evidence. Where a report is not part of the submitted record, state that it was not supplied. Do not label a report attached or verified solely because the clinical summary refers to it. Submitted reports are supporting records; this document does not imply that the current API can upload or parse attachments.

## 6. Evidence interpretation and discrepancies

### 6.1 Policy-specific interpretation

A condition is active when its start date is on or before the evidence assessment time and its end date is absent or strictly after that time. A condition ending exactly at the assessment time is not active. Narrative mention, family history, or a report suggesting possible disease does not by itself satisfy a coded PRESENT criterion. A historical fracture whose submitted condition has ended does not count as active for this limited rule.

### 6.2 Assessment time and active conditions

For this demonstration, the evidence assessment time is the most recent evidence-update timestamp, or the submission timestamp when no evidence update exists. It is not automatically the requested service date or the current wall-clock time.

Where a condition criterion applies, a start date equal to the assessment time is included; an end date equal to that time is excluded. A missing end date represents an open-ended submitted condition. This active-date rule does not establish whether a historical diagnosis is clinically irrelevant outside the seeded rule.

### 6.3 Measurements and evidence gaps

Where a numeric observation criterion applies, future-dated observations are excluded. There is no configured maximum lookback period in these seed policies. Do not invent a 30-, 90-, or 180-day freshness requirement.

Distinguish three findings: evidence supports the criterion; usable evidence is present but does not meet its threshold; evidence is missing or cannot be interpreted. Unit discrepancies, simultaneous conflicting latest measurements, and unsupported criterion configurations require clarification or manual assessment. Do not silently correct units or choose a favorable result.

## 7. Limitations, related services, and exceptions

This policy concerns the mapped bone-density procedure. It does not authorize a different imaging service, diagnose osteoporosis, prescribe treatment, or establish a universal screening or repeat-testing schedule. Do not import another payer's age, frequency, or technique restrictions into this fictional plan.

No quantity cap, authorization-validity period, repeat-service interval, geographic restriction, statutory entitlement, or appeal deadline is established by this document unless explicitly stated in section 3. Requested quantities and any human-approved quantity or validity dates belong in the case determination.

An exception considered by a human reviewer must describe its factual basis and unresolved questions. It does not alter the seeded rule or retrospectively create another automatic approval route. Material changes to policy criteria require a separately identified document version and consistent configuration.

## 8. Determination and requests for additional evidence

### 8.1 Assessment sequence

Confirm the service and applicable policy; inspect the evidence; apply the exact criterion relationship in section 3; identify data-quality limitations; and determine whether an automatic pathway is available or individual review is required.

An automated match may support approval only where a supported configured automatic pathway exists and the separate plan checks have passed. Missing evidence, unsupported configuration, and a nonmatching numeric result are distinct reasons for further review. A nonmatch is not an automatic final rejection.

### 8.2 Individual determination

Document which of sections 3.1.a and 3.1.b is supported, identify the submitted condition and its dates, and explain any unresolved discrepancy. If neither route is established, identify the evidence needed to complete assessment. Other clinically supported reasons may be discussed by a reviewer, but are not automatic qualifying routes under this version.

When additional evidence is needed, identify the item and the criterion or clinical question it would resolve. Where the reviewer reaches a final decision, document the facts supporting it, any relevant limitations, and the authorized service details. If clarification or reconsideration is sought, preserve the prior rationale and identify what new material was reviewed; this document does not specify a legal appeal process or deadline.

### 8.3 AI-assisted assessment

Generated summaries and criterion assessments are drafts for verification against submitted evidence and the cited policy section. The assistant must not invent reports, introduce another service's threshold, infer missing clinical facts, or submit the decision. The reviewer's recorded determination and the configured workflow remain authoritative.

## 9. Applicable terminology

| Role | Code system / category | Code | Seeded description |
| --- | --- | --- | --- |
| Requested service | SNOMED-CT / PROCEDURE | 312681000 | Bone density scan (procedure) |

Evidence codes and exact descriptions appear in section 3 where configured. Condition terminology is sourced from the repository's Synthea condition dataset; observation terminology is sourced from its observation dataset and uses LOINC. These are project mappings, not an assertion that every real payer uses these codes for authorization or reimbursement. No CPT, HCPCS, or ICD-10 crosswalk is asserted.

## 10. Review record and traceability

Record the case identifier, the policy number and document version consulted, the relevant section references, and the evidence used for each assessment. Preserve the selected measurement's value, unit, and recording time where relevant. A reviewer should identify which portions of a draft were verified and explain material unresolved discrepancies.

Store the human decision, reason, reviewer identity, approved quantity, and any chosen validity dates through the existing review workflow. These documentation expectations do not assert that document-version or citation persistence has already been implemented.

## 11. Sources and provenance

### 11.1 Authoritative source for demonstration rules

The operative criteria, operators, thresholds, units, and policy identifiers were transcribed from [policy.json](../data/policy.json). The service mapping and benefit flags were taken from [plan.json](../data/plan.json); payer identity was taken from [payer.json](../data/payer.json). The current processing behavior was checked against PolicyEvaluationService and ReviewService in this repository.

### 11.2 Background references

- **R1.** [Aetna Clinical Policy Bulletin 0134: Bone Mass Measurements](https://www.aetna.com/cpb/medical/data/100_199/0134.html). Example of payer-policy structure and broader bone-mass indications; its rules are not adopted.

External sources inform the document's structure or limited background discussion. They do not endorse Harbor Health Insurance, validate the seeded thresholds, or incorporate their complete clinical recommendations into this fictional plan. The operational rule remains the limited rule explicitly stated in section 3.

## 12. Document history

| Version | Published | Demonstration effective date | Summary |
| --- | --- | --- | --- |
| 1.0.0 | 2026-10-06 | 2026-10-01 | Initial fictional narrative document aligned with seeded policy 1; clinical context and limitations documented. |

No earlier narrative document is asserted. A future revision should identify changes to applicability, evidence requirements, interpretation, or configured rules and retain the previous version for reproducible review.

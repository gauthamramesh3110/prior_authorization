---
document_id: HHI-MP-002
policy_id: 70000000-0000-4000-8000-000000000002
version: "1.0.0"
title: "Biventricular Implantable Cardioverter Defibrillator"
payer_name: "Harbor Health Insurance"
plan_name: "Harbor Choice PPO"
plan_id: "20000000-0000-4000-8000-000000000001"
service_code: "447365002"
service_code_system: "SNOMED-CT"
document_status: "FICTIONAL_DEMONSTRATION"
published_date: "2026-10-06"
effective_from: "2026-10-01"
effective_to: null
last_review_date: "2026-10-06"
review_due_date: "2027-10-06"
seed_review_mode: "AUTO_APPROVAL_ELIGIBLE"
seed_match: "ALL"
---

# HHI-MP-002: Biventricular Implantable Cardioverter Defibrillator

**Harbor Health Insurance | Harbor Choice PPO**

> FICTIONAL DEMONSTRATION POLICY. Prepared for a synthetic-data software learning project. This is not an actual insurer policy, a clinically validated guideline, or medical advice. Published and effective dates are fictional document metadata.

| Document control | Detail |
| --- | --- |
| Policy number | HHI-MP-002 |
| Policy record ID | 70000000-0000-4000-8000-000000000002 |
| Version | 1.0.0 |
| Effective period | October 1, 2026 onward; no document end date assigned |
| Publication / last document review | October 6, 2026 |
| Planned document review | October 6, 2027 |
| Issuer / applicable product | Harbor Health Insurance / Harbor Choice PPO |
| Benefit and authorization configuration | Covered service; prior authorization required in the seeded plan |
| Seeded review designation | AUTO_APPROVAL_ELIGIBLE |
| Seeded criterion relationship | ALL |
| Scope of evidence | Structured case evidence and any separately supplied supporting narrative |

## 1. Application and benefit interpretation

This document applies to requests mapped to service **447365002**, **Insertion of biventricular implantable cardioverter defibrillator**, under Harbor Choice PPO. It describes the limited clinical-review policy associated with policy record 70000000-0000-4000-8000-000000000002.

Member eligibility, coverage period, provider organization, network participation, service benefit status, and prior-authorization requirements are determined through the plan's separate benefit configuration. A policy match does not create coverage when those prerequisites are not satisfied. A clinical reference or procedure code alone does not establish entitlement to payment.

Use the applicable document version for review. This document's publication dates do not themselves implement policy version selection in the application. Where applicable-version information is unavailable or inconsistent, disclose that limitation rather than claiming that a different version governs the case.

## 2. Clinical background and review purpose

A combined resynchronization-defibrillation device involves clinical considerations beyond an ejection-fraction measurement. Aetna's cardiac-resynchronization policy includes qualifications involving clinical status, therapy, and electrical findings. This demonstration intentionally retains only its seeded heart-failure diagnosis and ejection-fraction rule; those two items are not presented as complete real-world device-selection criteria. [R1]

The procedure and evidence descriptions in this document follow the supplied synthetic-data terminology. A code in section 9 defines the scope of this demonstration; it is not a complete billing-code crosswalk. Clinical context helps organize an individual assessment but does not silently expand the configured eligibility rules.

## 3. Policy statement and configured criteria

### 3.1 Configured eligibility requirements

**Both** of the following must be established:

| Section | Criterion ID | Evidence | Code | Requirement |
| --- | --- | --- | --- | --- |
| HHI-MP-002-3.1.a | b0000000-0000-4000-8000-000000000003 | Condition | 88805009 | Chronic congestive heart failure (disorder) is active |
| HHI-MP-002-3.1.b | b0000000-0000-4000-8000-000000000004 | Observation | 10230-1 | Left ventricular Ejection fraction is less than or equal to 35, with unit % |

The relationship is **AND**. An ejection fraction of exactly **35%** satisfies section 3.1.b; **35.1%** does not. A low ejection fraction does not substitute for the specified diagnosis.

### 3.2 Incomplete or discordant evidence

If a required diagnosis or measurement is missing, identify the missing item. If a qualifying measurement is available but exceeds 35%, record that the configured numeric criterion is not met. Neither circumstance is, by itself, an automatic final denial.

### 3.3 Limits of the configured pathway

This version does not define thresholds for QRS duration, NYHA class, medication duration, infarction waiting periods, or anticipated survival. Review considerations below do not create additional automatic criteria.

## 4. Supporting clinical review considerations

The following questions provide context for individual review. They are **not additional executable criteria** and do not establish unlisted denial thresholds.

### 4.1 Review consideration

Identify whether this is an initial implantation, upgrade, replacement, or another device-related procedure, and reconcile it with the submitted service code.

### 4.2 Review consideration

Review the clinician's reason for choosing combined resynchronization and defibrillation and the supporting cardiac reports.

### 4.3 Review consideration

Where available, summarize symptoms, therapy history, electrocardiographic findings, contraindications, and the treating team's assessment.

### 4.4 Review consideration

Distinguish a clinical recommendation from a completed shared-decision discussion; do not assume either from the service name.

A reviewer may seek clarification relevant to a consideration. The record should explain why it is needed for this case. Do not assume that an unavailable report, consultation, or treatment history is evidence that the corresponding clinical event did not occur.

## 5. Documentation for review

### 5.1 Evidence supporting the configured pathway

Provide the evidence required by section 3 for any route the submission relies on. Numeric observations require their code, value, unit, and recording time; active-condition routes require their coded condition and dates. For policies with no executable pathway, submit a narrative sufficient for individual review.

### 5.2 Supporting records

The following items form a practical review packet. Items described as available or relevant are supporting context, not new automatic prerequisites:

- Documentation of chronic congestive heart failure with its coded condition and active dates.
- An ejection-fraction observation including code 10230-1, numeric value, the exact unit %, and recording time.
- Where available, the source cardiac imaging report and relevant cardiology or electrophysiology assessment.
- Clarification of inconsistent measurements, device type, or procedure intent.

### 5.3 Record integrity

Identify the requested service, quantity, requested date, and the source of submitted clinical evidence. Where a report is not part of the submitted record, state that it was not supplied. Do not label a report attached or verified solely because the clinical summary refers to it. Submitted reports are supporting records; this document does not imply that the current API can upload or parse attachments.

## 6. Evidence interpretation and discrepancies

### 6.1 Policy-specific interpretation

Use the most recent observation with the required code recorded on or before the evidence assessment time. Do not select an older, lower result merely because it meets the threshold. If two latest observations share a recording time but disagree in value or unit, the discrepancy requires manual review. A fraction such as 0.35 with unit 'ratio' is not silently converted into 35%. The condition must meet the active-date definition in section 6.2.

### 6.2 Assessment time and active conditions

For this demonstration, the evidence assessment time is the most recent evidence-update timestamp, or the submission timestamp when no evidence update exists. It is not automatically the requested service date or the current wall-clock time.

Where a condition criterion applies, a start date equal to the assessment time is included; an end date equal to that time is excluded. A missing end date represents an open-ended submitted condition. This active-date rule does not establish whether a historical diagnosis is clinically irrelevant outside the seeded rule.

### 6.3 Measurements and evidence gaps

Where a numeric observation criterion applies, future-dated observations are excluded. There is no configured maximum lookback period in these seed policies. Do not invent a 30-, 90-, or 180-day freshness requirement.

Distinguish three findings: evidence supports the criterion; usable evidence is present but does not meet its threshold; evidence is missing or cannot be interpreted. Unit discrepancies, simultaneous conflicting latest measurements, and unsupported criterion configurations require clarification or manual assessment. Do not silently correct units or choose a favorable result.

## 7. Limitations, related services, and exceptions

Meeting this limited demonstration rule is not a complete clinical determination that a biventricular ICD is suitable for a patient. The document does not establish policy for standalone ICDs, wearable defibrillators, lead replacement, or devices mapped to other service codes. No fabricated duration or survival requirement may be used as a rejection reason.

No quantity cap, authorization-validity period, repeat-service interval, geographic restriction, statutory entitlement, or appeal deadline is established by this document unless explicitly stated in section 3. Requested quantities and any human-approved quantity or validity dates belong in the case determination.

An exception considered by a human reviewer must describe its factual basis and unresolved questions. It does not alter the seeded rule or retrospectively create another automatic approval route. Material changes to policy criteria require a separately identified document version and consistent configuration.

## 8. Determination and requests for additional evidence

### 8.1 Assessment sequence

Confirm the service and applicable policy; inspect the evidence; apply the exact criterion relationship in section 3; identify data-quality limitations; and determine whether an automatic pathway is available or individual review is required.

An automated match may support approval only where a supported configured automatic pathway exists and the separate plan checks have passed. Missing evidence, unsupported configuration, and a nonmatching numeric result are distinct reasons for further review. A nonmatch is not an automatic final rejection.

### 8.2 Individual determination

Assess sections 3.1.a and 3.1.b separately. Identify the measurement selected, its recording time and unit, and why it is the latest usable evidence. Explain whether the result is supported, incomplete, numerically not met, or conflicting. Any human decision that differs from the configured result must include a case-specific rationale.

When additional evidence is needed, identify the item and the criterion or clinical question it would resolve. Where the reviewer reaches a final decision, document the facts supporting it, any relevant limitations, and the authorized service details. If clarification or reconsideration is sought, preserve the prior rationale and identify what new material was reviewed; this document does not specify a legal appeal process or deadline.

### 8.3 AI-assisted assessment

Generated summaries and criterion assessments are drafts for verification against submitted evidence and the cited policy section. The assistant must not invent reports, introduce another service's threshold, infer missing clinical facts, or submit the decision. The reviewer's recorded determination and the configured workflow remain authoritative.

## 9. Applicable terminology

| Role | Code system / category | Code | Seeded description |
| --- | --- | --- | --- |
| Requested service | SNOMED-CT / PROCEDURE | 447365002 | Insertion of biventricular implantable cardioverter defibrillator |

Evidence codes and exact descriptions appear in section 3 where configured. Condition terminology is sourced from the repository's Synthea condition dataset; observation terminology is sourced from its observation dataset and uses LOINC. These are project mappings, not an assertion that every real payer uses these codes for authorization or reimbursement. No CPT, HCPCS, or ICD-10 crosswalk is asserted.

## 10. Review record and traceability

Record the case identifier, the policy number and document version consulted, the relevant section references, and the evidence used for each assessment. Preserve the selected measurement's value, unit, and recording time where relevant. A reviewer should identify which portions of a draft were verified and explain material unresolved discrepancies.

Store the human decision, reason, reviewer identity, approved quantity, and any chosen validity dates through the existing review workflow. These documentation expectations do not assert that document-version or citation persistence has already been implemented.

## 11. Sources and provenance

### 11.1 Authoritative source for demonstration rules

The operative criteria, operators, thresholds, units, and policy identifiers were transcribed from [policy.json](../data/policy.json). The service mapping and benefit flags were taken from [plan.json](../data/plan.json); payer identity was taken from [payer.json](../data/payer.json). The current processing behavior was checked against PolicyEvaluationService and ReviewService in this repository.

### 11.2 Background references

- **R1.** [Aetna Clinical Policy Bulletin 0610: Cardiac Resynchronization Therapy and Other Pacing/Defibrillator Treatments for Heart Failure](https://www.aetna.com/cpb/medical/data/600_699/0610.html). Clinical context and policy structure; not a complete adopted device-coverage guideline.

External sources inform the document's structure or limited background discussion. They do not endorse Harbor Health Insurance, validate the seeded thresholds, or incorporate their complete clinical recommendations into this fictional plan. The operational rule remains the limited rule explicitly stated in section 3.

## 12. Document history

| Version | Published | Demonstration effective date | Summary |
| --- | --- | --- | --- |
| 1.0.0 | 2026-10-06 | 2026-10-01 | Initial fictional narrative document aligned with seeded policy 2; clinical context and limitations documented. |

No earlier narrative document is asserted. A future revision should identify changes to applicability, evidence requirements, interpretation, or configured rules and retain the previous version for reproducible review.

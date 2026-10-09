---
document_id: HHI-MP-003
policy_id: 70000000-0000-4000-8000-000000000003
version: "1.0.0"
title: "Coronary Artery Bypass Grafting: Individual Clinical Review"
payer_name: "Harbor Health Insurance"
plan_name: "Harbor Choice PPO"
plan_id: "20000000-0000-4000-8000-000000000001"
service_code: "232717009"
service_code_system: "SNOMED-CT"
document_status: "FICTIONAL_DEMONSTRATION"
published_date: "2026-10-06"
effective_from: "2026-10-01"
effective_to: null
last_review_date: "2026-10-06"
review_due_date: "2027-10-06"
seed_review_mode: "MANUAL_REVIEW"
seed_match: "ALL"
---

# HHI-MP-003: Coronary Artery Bypass Grafting: Individual Clinical Review

**Harbor Health Insurance | Harbor Choice PPO**

> FICTIONAL DEMONSTRATION POLICY. Prepared for a synthetic-data software learning project. This is not an actual insurer policy, a clinically validated guideline, or medical advice. Published and effective dates are fictional document metadata.

| Document control | Detail |
| --- | --- |
| Policy number | HHI-MP-003 |
| Policy record ID | 70000000-0000-4000-8000-000000000003 |
| Version | 1.0.0 |
| Effective period | October 1, 2026 onward; no document end date assigned |
| Publication / last document review | October 6, 2026 |
| Planned document review | October 6, 2027 |
| Issuer / applicable product | Harbor Health Insurance / Harbor Choice PPO |
| Benefit and authorization configuration | Covered service; prior authorization required in the seeded plan |
| Seeded review designation | MANUAL_REVIEW |
| Seeded criterion relationship | ALL |
| Scope of evidence | Structured case evidence and any separately supplied supporting narrative |

## 1. Application and benefit interpretation

This document applies to requests mapped to service **232717009**, **Coronary artery bypass grafting**, under Harbor Choice PPO. It describes the limited clinical-review policy associated with policy record 70000000-0000-4000-8000-000000000003.

Member eligibility, coverage period, provider organization, network participation, service benefit status, and prior-authorization requirements are determined through the plan's separate benefit configuration. A policy match does not create coverage when those prerequisites are not satisfied. A clinical reference or procedure code alone does not establish entitlement to payment.

Use the applicable document version for review. This document's publication dates do not themselves implement policy version selection in the application. Where applicable-version information is unavailable or inconsistent, disclose that limitation rather than claiming that a different version governs the case.

## 2. Clinical background and review purpose

The ACC/AHA/SCAI revascularization guidance emphasizes patient-centered assessment and a multidisciplinary Heart Team where the optimal approach is uncertain. Relevant considerations include coronary anatomy, symptoms, clinical status, competing strategies, and patient preferences. This supports a narrative review framework, not an automatic surgery recommendation from a single diagnosis or laboratory value. [R1]

The procedure and evidence descriptions in this document follow the supplied synthetic-data terminology. A code in section 9 defines the scope of this demonstration; it is not a complete billing-code crosswalk. Clinical context helps organize an individual assessment but does not silently expand the configured eligibility rules.

## 3. Policy statement and configured criteria

### 3.1 Individual review requirement

All requests for the mapped coronary artery bypass grafting service require **manual review**. No executable clinical criteria are configured for this policy version.

An empty criteria list does not mean that all indications are satisfied. The configured ALL relationship does not create an automatic approval pathway for an empty list.

### 3.2 Clinical review framework

The reviewer considers the treating team's documented indication, anatomical findings, clinical presentation, and explanation for the proposed strategy. Section 4 provides questions for organizing that assessment. It is not a numeric checklist or an assertion that any one finding mandates CABG.

### 3.3 Incomplete submissions

A service label or diagnosis alone does not provide the clinical reasoning for surgery. Request the material necessary to understand the proposed procedure when the submitted record is insufficient. Explain what question each requested item would answer.

### 3.4 Decision authority

The reviewer records a case-specific determination. The assistant may summarize or compare submitted material with this document, but cannot select a revascularization strategy or submit an authorization decision.

## 4. Supporting clinical review considerations

The following questions provide context for individual review. They are **not additional executable criteria** and do not establish unlisted denial thresholds.

### 4.1 Review consideration

What indication and intended benefit does the treating clinician document for surgical revascularization?

### 4.2 Review consideration

What source report describes the coronary anatomy and findings relevant to the proposed procedure?

### 4.3 Review consideration

How does the team explain its choice among surgery, percutaneous intervention, and medical management, where applicable?

### 4.4 Review consideration

What relevant clinical status, comorbidities, procedural risks, and patient preferences are documented?

### 4.5 Review consideration

Where the strategy is uncertain or complex, is there a documented multidisciplinary assessment? Absence should be described, not replaced with a fabricated consultation.

A reviewer may seek clarification relevant to a consideration. The record should explain why it is needed for this case. Do not assume that an unavailable report, consultation, or treatment history is evidence that the corresponding clinical event did not occur.

## 5. Documentation for review

### 5.1 Evidence supporting the configured pathway

Provide the evidence required by section 3 for any route the submission relies on. Numeric observations require their code, value, unit, and recording time; active-condition routes require their coded condition and dates. For policies with no executable pathway, submit a narrative sufficient for individual review.

### 5.2 Supporting records

The following items form a practical review packet. Items described as available or relevant are supporting context, not new automatic prerequisites:

- A referral or surgical assessment explaining the proposed procedure and its clinical indication.
- Where available and relevant, angiographic findings, cardiac imaging, ischemia assessment, and the treating team's interpretation.
- A summary of symptoms, therapy history, prior coronary interventions, and clinical urgency.
- The documented rationale for the proposed strategy and any available patient-preference or multidisciplinary discussion.

### 5.3 Record integrity

Identify the requested service, quantity, requested date, and the source of submitted clinical evidence. Where a report is not part of the submitted record, state that it was not supplied. Do not label a report attached or verified solely because the clinical summary refers to it. Submitted reports are supporting records; this document does not imply that the current API can upload or parse attachments.

## 6. Evidence interpretation and discrepancies

### 6.1 Policy-specific interpretation

Report the date, source, and stated finding of each submitted report. An assistant must not infer anatomy from a diagnosis code, reconstruct an angiogram, or claim failed medical treatment where no history was supplied. Contradictory narrative and structured evidence should be identified for clinician clarification. Incomplete documentation is different from an established lack of indication.

### 6.2 Assessment time and active conditions

For this demonstration, the evidence assessment time is the most recent evidence-update timestamp, or the submission timestamp when no evidence update exists. It is not automatically the requested service date or the current wall-clock time.

Where a condition criterion applies, a start date equal to the assessment time is included; an end date equal to that time is excluded. A missing end date represents an open-ended submitted condition. This active-date rule does not establish whether a historical diagnosis is clinically irrelevant outside the seeded rule.

### 6.3 Measurements and evidence gaps

Where a numeric observation criterion applies, future-dated observations are excluded. There is no configured maximum lookback period in these seed policies. Do not invent a 30-, 90-, or 180-day freshness requirement.

Distinguish three findings: evidence supports the criterion; usable evidence is present but does not meet its threshold; evidence is missing or cannot be interpreted. Unit discrepancies, simultaneous conflicting latest measurements, and unsupported criterion configurations require clarification or manual assessment. Do not silently correct units or choose a favorable result.

## 7. Limitations, related services, and exceptions

This version provides no automatic anatomical stenosis threshold, vessel-count requirement, waiting period, or ejection-fraction cutoff for CABG. Do not reuse the 35% device criterion from HHI-MP-002. This document does not replace urgent clinical care pathways or provide eligibility criteria for percutaneous coronary intervention, valve surgery, or hospital admission before surgery.

No quantity cap, authorization-validity period, repeat-service interval, geographic restriction, statutory entitlement, or appeal deadline is established by this document unless explicitly stated in section 3. Requested quantities and any human-approved quantity or validity dates belong in the case determination.

An exception considered by a human reviewer must describe its factual basis and unresolved questions. It does not alter the seeded rule or retrospectively create another automatic approval route. Material changes to policy criteria require a separately identified document version and consistent configuration.

## 8. Determination and requests for additional evidence

### 8.1 Assessment sequence

Confirm the service and applicable policy; inspect the evidence; apply the exact criterion relationship in section 3; identify data-quality limitations; and determine whether an automatic pathway is available or individual review is required.

An automated match may support approval only where a supported configured automatic pathway exists and the separate plan checks have passed. Missing evidence, unsupported configuration, and a nonmatching numeric result are distinct reasons for further review. A nonmatch is not an automatic final rejection.

### 8.2 Individual determination

State the proposed procedure and indication, summarize the available anatomical and clinical evidence, identify material gaps, and explain the reviewer's determination in relation to those facts. Cite section 3.1 for the manual pathway and the relevant section 4 consideration for each question examined. Do not describe a nonexistent automated criterion as met.

When additional evidence is needed, identify the item and the criterion or clinical question it would resolve. Where the reviewer reaches a final decision, document the facts supporting it, any relevant limitations, and the authorized service details. If clarification or reconsideration is sought, preserve the prior rationale and identify what new material was reviewed; this document does not specify a legal appeal process or deadline.

### 8.3 AI-assisted assessment

Generated summaries and criterion assessments are drafts for verification against submitted evidence and the cited policy section. The assistant must not invent reports, introduce another service's threshold, infer missing clinical facts, or submit the decision. The reviewer's recorded determination and the configured workflow remain authoritative.

## 9. Applicable terminology

| Role | Code system / category | Code | Seeded description |
| --- | --- | --- | --- |
| Requested service | SNOMED-CT / PROCEDURE | 232717009 | Coronary artery bypass grafting |

Evidence codes and exact descriptions appear in section 3 where configured. Condition terminology is sourced from the repository's Synthea condition dataset; observation terminology is sourced from its observation dataset and uses LOINC. These are project mappings, not an assertion that every real payer uses these codes for authorization or reimbursement. No CPT, HCPCS, or ICD-10 crosswalk is asserted.

## 10. Review record and traceability

Record the case identifier, the policy number and document version consulted, the relevant section references, and the evidence used for each assessment. Preserve the selected measurement's value, unit, and recording time where relevant. A reviewer should identify which portions of a draft were verified and explain material unresolved discrepancies.

Store the human decision, reason, reviewer identity, approved quantity, and any chosen validity dates through the existing review workflow. These documentation expectations do not assert that document-version or citation persistence has already been implemented.

## 11. Sources and provenance

### 11.1 Authoritative source for demonstration rules

The operative criteria, operators, thresholds, units, and policy identifiers were transcribed from [policy.json](../data/policy.json). The service mapping and benefit flags were taken from [plan.json](../data/plan.json); payer identity was taken from [payer.json](../data/payer.json). The current processing behavior was checked against PolicyEvaluationService and ReviewService in this repository.

### 11.2 Background references

- **R1.** [ACC: American College of Cardiology, American Heart Association Issue Coronary Artery Revascularization Guideline (December 9, 2021)](https://www.acc.org/About-ACC/Press-Releases/2021/12/09/16/12/American-College-of-Cardiology-American-Heart-Association-Issue-Coronary-Artery-Revascularization-Guideline). Official summary supporting patient-centered and multidisciplinary review; no specific treatment thresholds adopted.

External sources inform the document's structure or limited background discussion. They do not endorse Harbor Health Insurance, validate the seeded thresholds, or incorporate their complete clinical recommendations into this fictional plan. The operational rule remains the limited rule explicitly stated in section 3.

## 12. Document history

| Version | Published | Demonstration effective date | Summary |
| --- | --- | --- | --- |
| 1.0.0 | 2026-10-06 | 2026-10-01 | Initial fictional narrative document aligned with seeded policy 3; clinical context and limitations documented. |

No earlier narrative document is asserted. A future revision should identify changes to applicability, evidence requirements, interpretation, or configured rules and retain the previous version for reproducible review.

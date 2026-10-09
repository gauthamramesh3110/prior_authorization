---
document_id: HHI-MP-005
policy_id: 70000000-0000-4000-8000-000000000005
version: "1.0.0"
title: "Transthoracic Echocardiography for Cardiac Assessment"
payer_name: "Harbor Health Insurance"
plan_name: "Harbor Choice PPO"
plan_id: "20000000-0000-4000-8000-000000000001"
service_code: "433236007"
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

# HHI-MP-005: Transthoracic Echocardiography for Cardiac Assessment

**Harbor Health Insurance | Harbor Choice PPO**

> FICTIONAL DEMONSTRATION POLICY. Prepared for a synthetic-data software learning project. This is not an actual insurer policy, a clinically validated guideline, or medical advice. Published and effective dates are fictional document metadata.

| Document control | Detail |
| --- | --- |
| Policy number | HHI-MP-005 |
| Policy record ID | 70000000-0000-4000-8000-000000000005 |
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

This document applies to requests mapped to service **433236007**, **Transthoracic echocardiography**, under Harbor Choice PPO. It describes the limited clinical-review policy associated with policy record 70000000-0000-4000-8000-000000000005.

Member eligibility, coverage period, provider organization, network participation, service benefit status, and prior-authorization requirements are determined through the plan's separate benefit configuration. A policy match does not create coverage when those prerequisites are not satisfied. A clinical reference or procedure code alone does not establish entitlement to payment.

Use the applicable document version for review. This document's publication dates do not themselves implement policy version selection in the application. Where applicable-version information is unavailable or inconsistent, disclose that limitation rather than claiming that a different version governs the case.

## 2. Clinical background and review purpose

Transthoracic echocardiography is included in professional heart-failure assessment guidance. The reason for a study and its relationship to existing information should be documented. The two routes below are fictional plan rules for this demonstration, not a complete list of clinically appropriate echocardiography indications. [R1]

The procedure and evidence descriptions in this document follow the supplied synthetic-data terminology. A code in section 9 defines the scope of this demonstration; it is not a complete billing-code crosswalk. Clinical context helps organize an individual assessment but does not silently expand the configured eligibility rules.

## 3. Policy statement and configured criteria

### 3.1 Configured indication routes

The policy's intended indication relationship is **ANY**, meaning either route can establish a configured indication:

| Section | Criterion ID | Evidence | Code | Requirement |
| --- | --- | --- | --- | --- |
| HHI-MP-005-3.1.a | b0000000-0000-4000-8000-000000000010 | Condition | 88805009 | Chronic congestive heart failure (disorder) is present |
| HHI-MP-005-3.1.b | b0000000-0000-4000-8000-000000000011 | Observation | 10230-1 | Left ventricular Ejection fraction observation is present; configured unit % |

Section 3.1.b specifies **presence**, not an ejection-fraction cutoff. There is no <=35% rule for this service. A recorded value of 50% is not numerically disqualifying under this presence route.

### 3.2 Current review pathway

The automated evaluator cannot assess the configured observation-presence route. Accordingly, requests under this policy require **manual review**, including requests that already contain an active heart-failure diagnosis. The intended OR relationship does not bypass an unsupported criterion configuration.

### 3.3 Clinical assessment

The reviewer assesses the supplied diagnosis or observation and the purpose of the requested study. A previously recorded ejection fraction alone does not explain why another study is requested; that clinical question belongs in the supporting narrative, not in an invented numeric rule.

### 3.4 Additional restrictions

No measurement threshold, repeat-study interval, required modality of a prior measurement, or heart-failure severity class is defined by the seed.

## 4. Supporting clinical review considerations

The following questions provide context for individual review. They are **not additional executable criteria** and do not establish unlisted denial thresholds.

### 4.1 Review consideration

Identify the clinical question for the requested transthoracic study and whether it relates to a diagnosis, symptoms, or follow-up described by the clinician.

### 4.2 Review consideration

Summarize available prior echocardiographic reports and whether the record explains the need for reassessment.

### 4.3 Review consideration

Where present, attribute the ejection fraction to its report, date, and measurement context.

### 4.4 Review consideration

Identify conflicting reports or a mismatch between the requested transthoracic study and a narrative describing another cardiac imaging modality.

A reviewer may seek clarification relevant to a consideration. The record should explain why it is needed for this case. Do not assume that an unavailable report, consultation, or treatment history is evidence that the corresponding clinical event did not occur.

## 5. Documentation for review

### 5.1 Evidence supporting the configured pathway

Provide the evidence required by section 3 for any route the submission relies on. Numeric observations require their code, value, unit, and recording time; active-condition routes require their coded condition and dates. For policies with no executable pathway, submit a narrative sufficient for individual review.

### 5.2 Supporting records

The following items form a practical review packet. Items described as available or relevant are supporting context, not new automatic prerequisites:

- The request's clinical summary and reason for the study.
- A coded heart-failure condition with dates when route 3.1.a is used.
- An ejection-fraction observation with its code, value, unit, and date when route 3.1.b is used.
- Where available, prior cardiac imaging and the clinician's explanation of the proposed study's purpose.

### 5.3 Record integrity

Identify the requested service, quantity, requested date, and the source of submitted clinical evidence. Where a report is not part of the submitted record, state that it was not supplied. Do not label a report attached or verified solely because the clinical summary refers to it. Submitted reports are supporting records; this document does not imply that the current API can upload or parse attachments.

## 6. Evidence interpretation and discrepancies

### 6.1 Policy-specific interpretation

The observation-presence criterion has no seeded numeric threshold. The '%'-unit metadata should be shown to the reviewer; the current evaluator does not perform an automated presence or unit assessment for this route. Do not label the route automatically matched, invent a <=35% requirement, or convert a documented observation into a heart-failure diagnosis. Future-dated and conflicting observations should be flagged for manual clarification.

### 6.2 Assessment time and active conditions

For this demonstration, the evidence assessment time is the most recent evidence-update timestamp, or the submission timestamp when no evidence update exists. It is not automatically the requested service date or the current wall-clock time.

Where a condition criterion applies, a start date equal to the assessment time is included; an end date equal to that time is excluded. A missing end date represents an open-ended submitted condition. This active-date rule does not establish whether a historical diagnosis is clinically irrelevant outside the seeded rule.

### 6.3 Measurements and evidence gaps

Where a numeric observation criterion applies, future-dated observations are excluded. There is no configured maximum lookback period in these seed policies. Do not invent a 30-, 90-, or 180-day freshness requirement.

Distinguish three findings: evidence supports the criterion; usable evidence is present but does not meet its threshold; evidence is missing or cannot be interpreted. Unit discrepancies, simultaneous conflicting latest measurements, and unsupported criterion configurations require clarification or manual assessment. Do not silently correct units or choose a favorable result.

## 7. Limitations, related services, and exceptions

This policy is for transthoracic echocardiography. It does not establish approval rules for transesophageal echocardiography, stress studies, strain imaging, or other related services. A configuration limitation is not a clinical contraindication or a final denial. The device policy HHI-MP-002 uses the same observation code for a different criterion and must not be substituted.

No quantity cap, authorization-validity period, repeat-service interval, geographic restriction, statutory entitlement, or appeal deadline is established by this document unless explicitly stated in section 3. Requested quantities and any human-approved quantity or validity dates belong in the case determination.

An exception considered by a human reviewer must describe its factual basis and unresolved questions. It does not alter the seeded rule or retrospectively create another automatic approval route. Material changes to policy criteria require a separately identified document version and consistent configuration.

## 8. Determination and requests for additional evidence

### 8.1 Assessment sequence

Confirm the service and applicable policy; inspect the evidence; apply the exact criterion relationship in section 3; identify data-quality limitations; and determine whether an automatic pathway is available or individual review is required.

An automated match may support approval only where a supported configured automatic pathway exists and the separate plan checks have passed. Missing evidence, unsupported configuration, and a nonmatching numeric result are distinct reasons for further review. A nonmatch is not an automatic final rejection.

### 8.2 Individual determination

Identify which route is supported by the submitted record, the purpose of the study, and any material missing context. Cite section 3.2 when explaining why the case remains manual despite the AUTO_APPROVAL_ELIGIBLE label. A reviewer may evaluate an observation-presence route; an assistant may describe that evidence but cannot claim the evaluator has approved it.

When additional evidence is needed, identify the item and the criterion or clinical question it would resolve. Where the reviewer reaches a final decision, document the facts supporting it, any relevant limitations, and the authorized service details. If clarification or reconsideration is sought, preserve the prior rationale and identify what new material was reviewed; this document does not specify a legal appeal process or deadline.

### 8.3 AI-assisted assessment

Generated summaries and criterion assessments are drafts for verification against submitted evidence and the cited policy section. The assistant must not invent reports, introduce another service's threshold, infer missing clinical facts, or submit the decision. The reviewer's recorded determination and the configured workflow remain authoritative.

## 9. Applicable terminology

| Role | Code system / category | Code | Seeded description |
| --- | --- | --- | --- |
| Requested service | SNOMED-CT / PROCEDURE | 433236007 | Transthoracic echocardiography |

Evidence codes and exact descriptions appear in section 3 where configured. Condition terminology is sourced from the repository's Synthea condition dataset; observation terminology is sourced from its observation dataset and uses LOINC. These are project mappings, not an assertion that every real payer uses these codes for authorization or reimbursement. No CPT, HCPCS, or ICD-10 crosswalk is asserted.

## 10. Review record and traceability

Record the case identifier, the policy number and document version consulted, the relevant section references, and the evidence used for each assessment. Preserve the selected measurement's value, unit, and recording time where relevant. A reviewer should identify which portions of a draft were verified and explain material unresolved discrepancies.

Store the human decision, reason, reviewer identity, approved quantity, and any chosen validity dates through the existing review workflow. These documentation expectations do not assert that document-version or citation persistence has already been implemented.

## 11. Sources and provenance

### 11.1 Authoritative source for demonstration rules

The operative criteria, operators, thresholds, units, and policy identifiers were transcribed from [policy.json](../data/policy.json). The service mapping and benefit flags were taken from [plan.json](../data/plan.json); payer identity was taken from [payer.json](../data/payer.json). The current processing behavior was checked against PolicyEvaluationService and ReviewService in this repository.

### 11.2 Background references

- **R1.** [NICE NG106: Chronic heart failure in adults: diagnosis and management](https://www.nice.org.uk/guidance/ng106/chapter/Recommendations). Related heart-failure assessment guidance; its testing pathways and time targets are not adopted as plan requirements.

External sources inform the document's structure or limited background discussion. They do not endorse Harbor Health Insurance, validate the seeded thresholds, or incorporate their complete clinical recommendations into this fictional plan. The operational rule remains the limited rule explicitly stated in section 3.

## 12. Document history

| Version | Published | Demonstration effective date | Summary |
| --- | --- | --- | --- |
| 1.0.0 | 2026-10-06 | 2026-10-01 | Initial fictional narrative document aligned with seeded policy 5; clinical context and limitations documented. |

No earlier narrative document is asserted. A future revision should identify changes to applicability, evidence requirements, interpretation, or configured rules and retain the previous version for reproducible review.

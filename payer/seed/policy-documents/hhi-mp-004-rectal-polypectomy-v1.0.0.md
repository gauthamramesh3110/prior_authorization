---
document_id: HHI-MP-004
policy_id: 70000000-0000-4000-8000-000000000004
version: "1.0.0"
title: "Rectal Polypectomy: Interim Individual Review Policy"
payer_name: "Harbor Health Insurance"
plan_name: "Harbor Choice PPO"
plan_id: "20000000-0000-4000-8000-000000000001"
service_code: "274031008"
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

# HHI-MP-004: Rectal Polypectomy: Interim Individual Review Policy

**Harbor Health Insurance | Harbor Choice PPO**

> FICTIONAL DEMONSTRATION POLICY. Prepared for a synthetic-data software learning project. This is not an actual insurer policy, a clinically validated guideline, or medical advice. Published and effective dates are fictional document metadata.

| Document control | Detail |
| --- | --- |
| Policy number | HHI-MP-004 |
| Policy record ID | 70000000-0000-4000-8000-000000000004 |
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

This document applies to requests mapped to service **274031008**, **Rectal polypectomy**, under Harbor Choice PPO. It describes the limited clinical-review policy associated with policy record 70000000-0000-4000-8000-000000000004.

Member eligibility, coverage period, provider organization, network participation, service benefit status, and prior-authorization requirements are determined through the plan's separate benefit configuration. A policy match does not create coverage when those prerequisites are not satisfied. A clinical reference or procedure code alone does not establish entitlement to payment.

Use the applicable document version for review. This document's publication dates do not themselves implement policy version selection in the application. Where applicable-version information is unavailable or inconsistent, disclose that limitation rather than claiming that a different version governs the case.

## 2. Clinical background and review purpose

Rectal polypectomy requests can contain a procedural indication, lesion description, prior endoscopic findings, and a proposed approach. Those details give a reviewer more useful context than a procedure code alone. Professional endoscopy guidance discusses colorectal lesion management; this document uses that subject as a clinical-review context and does not adopt technique or lesion-size thresholds. [R1]

The procedure and evidence descriptions in this document follow the supplied synthetic-data terminology. A code in section 9 defines the scope of this demonstration; it is not a complete billing-code crosswalk. Clinical context helps organize an individual assessment but does not silently expand the configured eligibility rules.

## 3. Policy statement and configured criteria

### 3.1 Interim review pathway

The plan covers the mapped rectal polypectomy service subject to its benefit configuration. **No clinical criteria have yet been configured** for this policy. Therefore, every request requires individual manual review under this interim document.

Although the policy record is designated AUTO_APPROVAL_ELIGIBLE, that designation is not a completed automatic pathway. An empty ALL criteria list is not treated as a passing evaluation.

### 3.2 Assessment under this version

Organize the review around the documented lesion, indication, source findings, proposed procedure, and unresolved questions in section 4. These considerations do not create a new set of mandatory numeric eligibility criteria.

### 3.3 Configuration gap

The absence of a configured criterion is a policy configuration issue, not evidence that the member failed a criterion. Do not classify every request as unsupported, experimental, or excluded merely because the configuration is incomplete.

### 3.4 Transition to a future policy

A later version may introduce explicit criteria after clinical and implementation review. Such criteria must be published and mapped consistently before use; they must not be silently inferred from this interim document.

## 4. Supporting clinical review considerations

The following questions provide context for individual review. They are **not additional executable criteria** and do not establish unlisted denial thresholds.

### 4.1 Review consideration

Identify the documented rectal lesion and the source of that finding.

### 4.2 Review consideration

Where reported, summarize location, dimensions, morphology, symptoms, and relevant prior findings without inventing absent characteristics.

### 4.3 Review consideration

Explain whether the request concerns initial removal, residual tissue, recurrence, or another circumstance.

### 4.4 Review consideration

Review the proposed approach and the clinician's explanation where it differs from an available prior recommendation.

### 4.5 Review consideration

Identify whether pathology is already available or is expected after removal. Do not assume a pre-removal specimen exists.

A reviewer may seek clarification relevant to a consideration. The record should explain why it is needed for this case. Do not assume that an unavailable report, consultation, or treatment history is evidence that the corresponding clinical event did not occur.

## 5. Documentation for review

### 5.1 Evidence supporting the configured pathway

Provide the evidence required by section 3 for any route the submission relies on. Numeric observations require their code, value, unit, and recording time; active-condition routes require their coded condition and dates. For policies with no executable pathway, submit a narrative sufficient for individual review.

### 5.2 Supporting records

The following items form a practical review packet. Items described as available or relevant are supporting context, not new automatic prerequisites:

- A clinician's assessment or referral explaining why rectal polypectomy is requested.
- Where available, endoscopic reports, photographs described in the submitted record, or other reports establishing the lesion.
- The proposed procedure and relevant clinical history, including prior attempted removal if documented.
- Existing pathology when relevant and available; absence of preprocedure pathology alone is not a seeded rejection criterion.

### 5.3 Record integrity

Identify the requested service, quantity, requested date, and the source of submitted clinical evidence. Where a report is not part of the submitted record, state that it was not supplied. Do not label a report attached or verified solely because the clinical summary refers to it. Submitted reports are supporting records; this document does not imply that the current API can upload or parse attachments.

## 6. Evidence interpretation and discrepancies

### 6.1 Policy-specific interpretation

A narrative description should be attributed to its report and date. Do not substitute a diagnosis of malignancy for a polyp, infer a benign lesion from an absent pathology result, or treat an unrelated colon finding as a documented rectal lesion. Resolve discrepancies in location, procedure, or report identity through manual review. An incomplete policy rule cannot be repaired by an LLM-generated threshold.

### 6.2 Assessment time and active conditions

For this demonstration, the evidence assessment time is the most recent evidence-update timestamp, or the submission timestamp when no evidence update exists. It is not automatically the requested service date or the current wall-clock time.

Where a condition criterion applies, a start date equal to the assessment time is included; an end date equal to that time is excluded. A missing end date represents an open-ended submitted condition. This active-date rule does not establish whether a historical diagnosis is clinically irrelevant outside the seeded rule.

### 6.3 Measurements and evidence gaps

Where a numeric observation criterion applies, future-dated observations are excluded. There is no configured maximum lookback period in these seed policies. Do not invent a 30-, 90-, or 180-day freshness requirement.

Distinguish three findings: evidence supports the criterion; usable evidence is present but does not meet its threshold; evidence is missing or cannot be interpreted. Unit discrepancies, simultaneous conflicting latest measurements, and unsupported criterion configurations require clarification or manual assessment. Do not silently correct units or choose a favorable result.

## 7. Limitations, related services, and exceptions

This policy does not define a lesion-size cutoff, mandatory technique, number-of-polyps threshold, screening age, surveillance interval, or repeat-procedure frequency. It does not authorize colonoscopy or a different resection procedure merely because those services may be clinically related. The service and benefit mappings remain separate.

No quantity cap, authorization-validity period, repeat-service interval, geographic restriction, statutory entitlement, or appeal deadline is established by this document unless explicitly stated in section 3. Requested quantities and any human-approved quantity or validity dates belong in the case determination.

An exception considered by a human reviewer must describe its factual basis and unresolved questions. It does not alter the seeded rule or retrospectively create another automatic approval route. Material changes to policy criteria require a separately identified document version and consistent configuration.

## 8. Determination and requests for additional evidence

### 8.1 Assessment sequence

Confirm the service and applicable policy; inspect the evidence; apply the exact criterion relationship in section 3; identify data-quality limitations; and determine whether an automatic pathway is available or individual review is required.

An automated match may support approval only where a supported configured automatic pathway exists and the separate plan checks have passed. Missing evidence, unsupported configuration, and a nonmatching numeric result are distinct reasons for further review. A nonmatch is not an automatic final rejection.

### 8.2 Individual determination

Describe the indication and available supporting record. Explicitly state that the case follows the interim manual pathway because no executable criteria are configured. Record any needed clarification and the case-specific human rationale. Do not report 'all criteria met' when the list is empty.

When additional evidence is needed, identify the item and the criterion or clinical question it would resolve. Where the reviewer reaches a final decision, document the facts supporting it, any relevant limitations, and the authorized service details. If clarification or reconsideration is sought, preserve the prior rationale and identify what new material was reviewed; this document does not specify a legal appeal process or deadline.

### 8.3 AI-assisted assessment

Generated summaries and criterion assessments are drafts for verification against submitted evidence and the cited policy section. The assistant must not invent reports, introduce another service's threshold, infer missing clinical facts, or submit the decision. The reviewer's recorded determination and the configured workflow remain authoritative.

## 9. Applicable terminology

| Role | Code system / category | Code | Seeded description |
| --- | --- | --- | --- |
| Requested service | SNOMED-CT / PROCEDURE | 274031008 | Rectal polypectomy |

Evidence codes and exact descriptions appear in section 3 where configured. Condition terminology is sourced from the repository's Synthea condition dataset; observation terminology is sourced from its observation dataset and uses LOINC. These are project mappings, not an assertion that every real payer uses these codes for authorization or reimbursement. No CPT, HCPCS, or ICD-10 crosswalk is asserted.

## 10. Review record and traceability

Record the case identifier, the policy number and document version consulted, the relevant section references, and the evidence used for each assessment. Preserve the selected measurement's value, unit, and recording time where relevant. A reviewer should identify which portions of a draft were verified and explain material unresolved discrepancies.

Store the human decision, reason, reviewer identity, approved quantity, and any chosen validity dates through the existing review workflow. These documentation expectations do not assert that document-version or citation persistence has already been implemented.

## 11. Sources and provenance

### 11.1 Authoritative source for demonstration rules

The operative criteria, operators, thresholds, units, and policy identifiers were transcribed from [policy.json](../data/policy.json). The service mapping and benefit flags were taken from [plan.json](../data/plan.json); payer identity was taken from [payer.json](../data/payer.json). The current processing behavior was checked against PolicyEvaluationService and ReviewService in this repository.

### 11.2 Background references

- **R1.** [ASGE / US Multi-Society Task Force: Endoscopic Removal of Colorectal Lesions (2020)](https://www.asge.org/docs/default-source/guidelines/endoscopic-removal-of-colorectal-lesions-recommendations-by-the-us-multi-society-task-force-on-colorectal-cancer-2020-march-gie.pdf). Related professional guideline for background reading; its detailed recommendations are not incorporated into this interim policy.

External sources inform the document's structure or limited background discussion. They do not endorse Harbor Health Insurance, validate the seeded thresholds, or incorporate their complete clinical recommendations into this fictional plan. The operational rule remains the limited rule explicitly stated in section 3.

## 12. Document history

| Version | Published | Demonstration effective date | Summary |
| --- | --- | --- | --- |
| 1.0.0 | 2026-10-06 | 2026-10-01 | Initial fictional narrative document aligned with seeded policy 4; clinical context and limitations documented. |

No earlier narrative document is asserted. A future revision should identify changes to applicability, evidence requirements, interpretation, or configured rules and retain the previous version for reproducible review.

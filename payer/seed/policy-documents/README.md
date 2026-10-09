# Harbor Health Insurance demonstration policy documents

Six original fictional payer-style documents support the six existing seeded policies. They are Markdown source artifacts for reading and future RAG ingestion. They are not clinically validated insurer policies.

## Policy index

| Document | Service | Seed policy ID suffix | Configured rule | Current evaluator outcome |
| --- | --- | --- | --- | --- |
| [HHI-MP-001: Bone Density Measurement for Osteoporosis](hhi-mp-001-bone-density-scan-v1.0.0.md) | 312681000 | 000000000001 | AUTO_APPROVAL_ELIGIBLE; ANY | Automatic match possible; other outcomes depend on evidence |
| [HHI-MP-002: Biventricular Implantable Cardioverter Defibrillator](hhi-mp-002-biventricular-icd-v1.0.0.md) | 447365002 | 000000000002 | AUTO_APPROVAL_ELIGIBLE; ALL | Automatic match possible; other outcomes depend on evidence |
| [HHI-MP-003: Coronary Artery Bypass Grafting: Individual Clinical Review](hhi-mp-003-coronary-artery-bypass-grafting-v1.0.0.md) | 232717009 | 000000000003 | MANUAL_REVIEW; ALL; empty criteria | Manual by policy mode |
| [HHI-MP-004: Rectal Polypectomy: Interim Individual Review Policy](hhi-mp-004-rectal-polypectomy-v1.0.0.md) | 274031008 | 000000000004 | AUTO_APPROVAL_ELIGIBLE; ALL; empty criteria | Manual: empty criteria |
| [HHI-MP-005: Transthoracic Echocardiography for Cardiac Assessment](hhi-mp-005-transthoracic-echocardiography-v1.0.0.md) | 433236007 | 000000000005 | AUTO_APPROVAL_ELIGIBLE; ANY | Manual: observation PRESENT is unsupported |
| [HHI-MP-006: Spirometry for Pulmonary Assessment and Follow-up](hhi-mp-006-spirometry-v1.0.0.md) | 127783003 | 000000000006 | AUTO_APPROVAL_ELIGIBLE; ANY | Automatic match possible; other outcomes depend on evidence |

Full IDs and file metadata are in [manifest.json](manifest.json). Each document is standalone with numbered sections, provenance, background references, and a revision history.

## Authority and realism

The document style resembles public payer policies, but the clinical rules belong to this project's curated synthetic seed data. Source-linked clinical background does not make these complete real-world medical-necessity policies. No copied insurer text, actual insurer branding, invented clinical approval, or claimed external endorsement is included.

The additional questions and suggested supporting reports in section 4 and section 5.2 are reviewer context. They are not new seeded criteria. A production policy would require clinical governance, plan-specific legal review, authoritative coding review, and much more evidence than these limited examples. This collection intentionally documents those limits rather than fabricating a production deployment.

The fictional payer and plan names exactly match the seed. Versions and dates are new document metadata: version 1.0.0, publication October 6, 2026, demonstration effective date October 1, 2026, and review due October 6, 2027. They do not alter coverage dates, policy routing, or runtime policy applicability.

## Important behavior preserved

1. Bone-density scan: ANY active osteoporosis condition or active osteoporosis-related pathological-fracture condition.
2. Biventricular ICD: ALL active chronic congestive heart failure and latest usable ejection fraction <=35% with exact unit %.
3. CABG: MANUAL_REVIEW with no executable criteria. No invented surgery thresholds.
4. Rectal polypectomy: AUTO_APPROVAL_ELIGIBLE with empty criteria routes to manual review. It is documented as an interim policy, not an unconditional approval.
5. Transthoracic echocardiography: ANY heart-failure condition or ejection-fraction observation PRESENT. Observation PRESENT is unsupported by the current evaluator; manual fallback takes precedence even if the diagnosis matches. There is no ejection-fraction cutoff for this service.
6. Spirometry: ANY latest usable FEV1/FVC <70% or arterial oxygen saturation <=88%, both with exact unit %. Unsupported or conflicting data can take precedence over a matching route.

The assessment timestamp is evidenceUpdatedAt or, if absent, submittedAt. Active conditions have inclusive start and exclusive end boundaries. Numeric observations use the latest recording at or before that timestamp, reject conflicting latest same-time results and unrecognized units, and have no seeded maximum age. The seed contains no 'resting' observation field or universal freshness requirement.

## Using these documents for RAG

Start with one applicable document's full text before implementing chunk retrieval. Keep its policy ID, version, and numbered section labels alongside every retrieved passage. Store document text separately from case evidence.

The manifest is a file-level mapping only. These files have not been attached in the database, added to seed.py, embedded, indexed, or supplied to ReviewerAssistantService. The existing Policy entity has no document-attachment field. Adding that capability is a separate implementation step.

When indexing, search within the selected policy ID and version. Preserve headings and tables. Do not retrieve HHI-MP-002's <=35% device threshold for HHI-MP-005's observation-presence rule merely because both mention ejection fraction. Preserve ALL/ANY scope, exceptions, and unsupported-configuration qualifications with their criteria.

Every file repeats its fiction label and authority boundary so a standalone document or extracted chunk does not misleadingly imply a real insurer determination. If metadata, references, or implementation notes are excluded from searchable passages, keep their authority and version information available through retrieval metadata.

## Quick review examples

| Case | Expected interpretation |
| --- | --- |
| Policy 1: active osteoporosis; no fracture | The ANY indication can be met by osteoporosis alone |
| Policy 2: heart failure and latest EF 35% | Both seeded criteria can match |
| Policy 2: heart failure and latest EF 35.1% | Numeric criterion not met; manual review, not automatic denial |
| Policy 2: qualifying old EF but a later EF 45% | Use latest usable EF; do not select a favorable old measurement |
| Policy 3: empty submitted evidence | Manual pathway; missing clinical context is not a fabricated failed numeric rule |
| Policy 4: no configured criteria | Manual fallback; do not interpret empty ALL as success |
| Policy 5: heart failure and EF 50% | Manual fallback; no <=35% requirement exists |
| Policy 6: FEV1/FVC 70% and saturation 89% | Neither numeric route meets its threshold |
| Policy 6: FEV1/FVC 69.9%; alternate observation missing | ANY can match if there is no manual-review override |
| Policy 6: latest same-time conflicting observations; alternate route matches | Manual-review override takes precedence |
| Any numeric policy: result dated after assessment time | Future evidence is not used |
| Any condition policy: condition ends exactly at assessment time | Condition is not active |

These are explanatory review cases, not added executable tests.

## References and source role

Each document links its own background reference in section 11. The public references are for structural or subject-matter context. Their clinical rules are not adopted as the fictional plan's criteria.

The ASGE and NICE references were discoverable in official-source search results, but direct full-text retrieval was unavailable during authoring; neither full guideline is represented as fully reviewed or incorporated. The spirometry reference is the 2019 ATS/ERS technical statement, not a claimed latest guideline. Reference pages were consulted or discovered on October 6, 2026.

Use the original clinical source for actual practice guidance. Use policy.json, plan.json, and current evaluator code to reproduce this project's behavior.

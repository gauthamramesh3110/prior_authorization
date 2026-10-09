# Curated payer seed

Review the JSON files in `data/`, then run `seed.py` after Flyway migrations V1-V9. Plans contain their `plan_services`, and policies contain their `criteria`. Other files contain arrays of database rows. Keys use database column names, including `code_description`; the Java objects and review-details API use `codeDescription`.

The script truncates all tables in the payer database's public schema except flyway_schema_history, then inserts the seed rows in the same transaction. Existing reference data, authorization requests, reviews, and vector embeddings are deleted on a successful run. Owned sequences are reset. Flyway history, schema definitions, and extensions are preserved. It only unpacks nested services and criteria and adds their parent IDs. UUIDs, dates, codes, descriptions, and thresholds are explicit in JSON. It does not generate clinical evidence or transform raw data during ingestion. A failure rolls back both the truncation and the seed inserts, restoring the previous data. Authorization requests and reviews are created through the application.

Stop the payer application before reseeding so its schedulers cannot write during the reset. From the repository root, with the database running:

```powershell
python -m pip install -r payer/seed/requirements.txt
python payer/seed/seed.py
```

The default connection is `postgresql://payer:payer@localhost:5434/payer_db`, matching `docker-compose.yaml`. To use another database:

```powershell
$env:PAYER_DATABASE_URL = "postgresql://user:password@localhost:5432/database"
python payer/seed/seed.py
```

| File | Contains |
| --- | --- |
| `data/payer.json` | Insurer |
| `data/plan.json` | PPO plan and its services |
| `data/organization.json` | Provider organizations |
| `data/provider.json` | Ordering providers |
| `data/patient.json` | Ten patients |
| `data/coverage.json` | Patient enrollment periods |
| `data/network_participation.json` | Organization network agreements |
| `data/policy.json` | Policies, document filenames, ingestion statuses, and criteria |

Child rows omit `plan_id` or `policy_id` because their parent supplies it. Their own IDs remain explicit. JSON values are passed as query parameters; [Psycopg](https://www.psycopg.org/psycopg3/docs/basic/usage.html) handles database communication and the transaction.

Every successful run starts from empty application tables, so removing rows from JSON also removes them from the reseeded database. Requests and reviews remain empty until recreated through the application. Restart the payer application after seeding to ingest the policy documents again. The seed gives exactly the counts below.

## Dataset

The fictional PPO setup contains 10 patients, one payer, one plan, three organizations, three ordering providers, nine coverages, three network agreements, six policies, eight criteria, and nine plan services. Conditions and observations belong in API request bodies because the current schema stores clinical evidence on authorization requests.

The dates are fixed in 2026 so you can reproduce cutoff and boundary cases. Seven patients have active coverage for ordinary scenarios. Liam has terminated coverage, Ava has future enrollment, and Lucas has no enrollment. Daniel has open-ended coverage. Riverside is in network, North Valley has a March-June contract, and Lakeview is explicitly out of network.

Identifiers use `X0000000-0000-4000-8000-NNNNNNNNNNNN`, where the leading character identifies the table and the final number identifies the row.

| Leading character | Table |
| --- | --- |
| 1 | payer |
| 2 | plan |
| 3 | organization |
| 4 | provider |
| 5 | patient |
| 6 | coverage |
| 7 | policy |
| b | policy_criterion |
| c | plan_service |

The payer and plan both use final number 1. Organization and provider final numbers correspond. Generate a new request UUID for every submission. Use any reviewer UUID in manual APIs, for example `a0000000-0000-4000-8000-000000000001`; there is no reviewer table.

| Patient number | Name | Scenario role |
| --- | --- | --- |
| 1 | Amelia Hart | Osteoporosis, screening, and benefit rules |
| 2 | Daniel Reed | Pulmonary evidence and service configuration |
| 3 | Sofia Morris | Matching cardiac criteria and date boundaries |
| 4 | Noah Bennett | Failed clinical criteria |
| 5 | Maya Patel | Missing and subsequently submitted evidence |
| 6 | Ethan Brooks | Conflicting measurements and unsupported policy handling |
| 7 | Olivia Chen | Manual cardiac review and network participation |
| 8 | Liam Walker | Coverage ended at 2026-07-01 00:00 UTC |
| 9 | Ava Thompson | Coverage starts at 2026-11-01 00:00 UTC |
| 10 | Lucas Garcia | No coverage |

## Policies and services

The authoritative terminology source is the repository's Synthea dataset:

- [procedures.csv](../../raw_data/procedures.csv) supplies every plan-service code and `code_description`.
- [conditions.csv](../../raw_data/conditions.csv) supplies condition-criterion codes and descriptions.
- [observations.csv](../../raw_data/observations.csv) supplies observation-criterion codes, descriptions, and units.

Descriptions are copied exactly from each CSV's `DESCRIPTION` column. Procedure and condition codes use SNOMED-CT; the seeded observation codes use LOINC. There are no invented procedure or observation codes. The patients and organizations remain a small curated setup.

Synthea contains clinical data rather than payer policies. Harbor Choice's coverage flags, prior-authorization requirements, and thresholds are curated rules using that terminology. They represent a limited set of clinical indications supported by the current evaluator, rather than a complete insurer coverage policy.

| Service code | Synthea procedure | Policy / behavior |
| --- | --- | --- |
| 73761001 | Colonoscopy | Covered, no prior authorization |
| 698354004 | Magnetic resonance imaging for measurement of brain volume (procedure) | Excluded benefit under this plan |
| 312681000 | Bone density scan (procedure) | Policy 1: ANY osteoporosis 64859006 or pathological fracture due to osteoporosis 443165006 |
| 447365002 | Insertion of biventricular implantable cardioverter defibrillator | Policy 2: ALL chronic congestive heart failure 88805009 and left ventricular ejection fraction 10230-1 <= 35% |
| 232717009 | Coronary artery bypass grafting | Policy 3: manual review of surgical indications and supporting reports |
| 274031008 | Rectal polypectomy | Policy 4: criteria awaiting configuration; manual fallback |
| 241615005 | Magnetic resonance imaging of breast (procedure) | No assigned policy; manual fallback |
| 433236007 | Transthoracic echocardiography | Policy 5: ANY heart failure or an ejection-fraction observation with PRESENT; the evaluator does not support observation-presence checks, so manual fallback overrides a matching condition |
| 127783003 | Spirometry (procedure) | Policy 6: ANY documented airflow limitation, FEV1/FVC 19926-5 < 70%, or resting arterial oxygen saturation 2708-6 <= 88% |

Submit `16335031000119103`, High resolution computed tomography of chest without contrast (procedure), to exercise the unconfigured-service fallback. This code exists in Synthea but is intentionally absent from the plan.

The cardiac rule uses the available heart-failure diagnosis and ejection-fraction evidence. Therapy duration, NYHA class, QRS findings, and shared-decision documentation are not available as numeric observations in this raw dataset and are not fabricated. A complete cardiac-device policy would need more evidence than this limited rule. The spirometry rule supports pulmonary follow-up for airflow limitation or hypoxemia; it is a plan-specific example, not a diagnostic definition or a complete guideline.

## Create requests through the application

Use `POST /api/v1/requests`. Replace `requestId` with a new UUID. This cardiac example matches the configured policy:

```json
{
  "requestId": "80000000-0000-4000-8000-000000000001",
  "submittedAt": "2026-10-01T12:00:00Z",
  "patientId": "50000000-0000-4000-8000-000000000003",
  "providerId": "40000000-0000-4000-8000-000000000001",
  "organizationId": "30000000-0000-4000-8000-000000000001",
  "planId": "20000000-0000-4000-8000-000000000001",
  "requestedService": {
    "code": "447365002",
    "codeSystem": "SNOMED-CT",
    "description": "Insertion of biventricular implantable cardioverter defibrillator",
    "requestedDate": "2026-10-01",
    "quantity": 1
  },
  "clinicalJustification": {
    "summary": "Chronic congestive heart failure with reduced left ventricular ejection fraction. Echocardiogram attached.",
    "conditions": [
      {
        "code": "88805009",
        "description": "Chronic congestive heart failure (disorder)",
        "startDate": "2025-06-15",
        "endDate": null
      }
    ],
    "observations": [
      {
        "code": "10230-1",
        "value": 35,
        "units": "%",
        "recordedAt": "2026-09-30T12:00:00Z"
      }
    ]
  }
}
```

For routine, excluded, unlisted, or manual services, a clinical justification with a summary and empty conditions/observations is sufficient. For bone density, use either qualifying condition. For spirometry, use `19926-5` (FEV1/FVC) or `2708-6` (oxygen saturation), both with units `%` as stored in Synthea.

Wait for the schedulers, then use `GET /api/v1/requests/{id}?providerId=...` to inspect the request and discover its review ID. Reviews receive IDs generated by the application. Use `GET /api/v1/reviews?status=PENDING_MANUAL_REVIEW` for the manual queue and `GET /api/v1/reviews/{id}` for details.

## Scenario variations

Use the patient and service below, and vary the cardiac example where relevant. Unless stated otherwise, use provider/organization 1 and submission time 2026-10-01 12:00 UTC. For the January coverage-start case, date all observations before that submission. For North Valley cases, use provider/organization 2. For Lakeview, use provider/organization 3. Each variation is a separate API submission with a new request ID.

| Scenario | Patient | Service | Expected request reason | Expected review status |
| --- | --- | --- | --- | --- |
| Covered service without prior authorization | 1 | 73761001 | PRIOR_AUTH_NOT_REQUIRED | None |
| Excluded brain-volumetry MRI benefit | 2 | 698354004 | SERVICE_EXCLUDED | None |
| Patient without enrollment | 10 | 73761001 | NOT_COVERED | None |
| Expired coverage | 8 | 73761001 | COVERAGE_INACTIVE | None |
| Coverage has not started | 9 | 73761001 | COVERAGE_INACTIVE | None |
| Coverage end is exclusive | 8 | 73761001 | COVERAGE_INACTIVE | None |
| Coverage start is inclusive | 3 | 447365002 | AUTO_APPROVED | DECIDED |
| Explicitly out-of-network organization | 7 | 232717009 | OUT_OF_NETWORK | None |
| Network contract end is exclusive | 7 | 73761001 | OUT_OF_NETWORK | None |
| Network contract has not started | 7 | 73761001 | OUT_OF_NETWORK | None |
| Network contract start is inclusive | 7 | 73761001 | PRIOR_AUTH_NOT_REQUIRED | None |
| Service is not configured | 2 | 16335031000119103 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Heart failure and ejection fraction match at the LTE boundary | 3 | 447365002 | AUTO_APPROVED | DECIDED |
| Ejection fraction above the threshold | 4 | 447365002 | CRITERIA_NOT_MET | PENDING_MANUAL_REVIEW |
| Missing ejection fraction | 5 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Missing qualifying diagnosis | 3 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Missing diagnosis and measurement | 5 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Measurement has incompatible units | 6 | 447365002 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Conflicting measurements at the latest timestamp | 6 | 447365002 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Conflicting units at the latest timestamp | 6 | 447365002 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Equivalent decimal measurements are accepted | 6 | 447365002 | AUTO_APPROVED | DECIDED |
| Latest eligible measurement wins | 3 | 447365002 | AUTO_APPROVED | DECIDED |
| Only measurement is after the evaluation cutoff | 5 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Later report is ignored when earlier eligible evidence exists | 3 | 447365002 | AUTO_APPROVED | DECIDED |
| Condition begins after submission | 3 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Condition end is exclusive | 3 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Condition start is inclusive | 3 | 447365002 | AUTO_APPROVED | DECIDED |
| Condition remains active after submission | 3 | 447365002 | AUTO_APPROVED | DECIDED |
| Condition list is absent | 5 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Observation list is absent | 5 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Both structured evidence lists are absent | 5 | 447365002 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| Policy requires human review | 7 | 232717009 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Policy has no configured criteria | 1 | 274031008 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Service has no assigned policy | 2 | 241615005 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| Unsupported criterion overrides a matched ANY criterion | 6 | 433236007 | MANUAL_REVIEW_REQUIRED | PENDING_MANUAL_REVIEW |
| ANY policy matches osteoporosis | 1 | 312681000 | AUTO_APPROVED | DECIDED |
| ANY policy matches the alternative diagnosis | 1 | 312681000 | AUTO_APPROVED | DECIDED |
| ANY policy has neither qualifying diagnosis | 2 | 312681000 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| ALL failure takes precedence over missing evidence | 4 | 447365002 | CRITERIA_NOT_MET | PENDING_MANUAL_REVIEW |
| ANY numeric match takes precedence over missing evidence | 2 | 127783003 | AUTO_APPROVED | DECIDED |
| ANY oxygen-saturation criterion matches at its LTE boundary | 2 | 127783003 | AUTO_APPROVED | DECIDED |
| ANY numeric criteria all fail | 2 | 127783003 | CRITERIA_NOT_MET | PENDING_MANUAL_REVIEW |
| ANY failure plus missing evidence requests evidence | 2 | 127783003 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |
| ANY numeric policy has no measurements | 2 | 127783003 | AWAITING_EVIDENCE | AWAITING_EVIDENCE |

For the cardiac variations, set ejection fraction to 35.1 to fail the criterion, remove the measurement or diagnosis to request evidence, or use units `ratio` to force manual review. Add same-code observations at the same latest timestamp with different values or units to force manual review; equivalent values 35 and 35.0 with identical units are accepted. Put an older observation before the current one to exercise latest-report selection, and date a future report 2026-10-02 to exercise the original cutoff. A condition can start/end at the exact submission timestamp to exercise inclusive start/exclusive end.

Coverage boundaries are 2026-01-01 00:00 UTC for active members and 2026-07-01 00:00 UTC for Liam's termination. North Valley's network boundaries are 2026-03-01 00:00 UTC and 2026-07-01 00:00 UTC. Ava's ordinary October request precedes her November enrollment.

For spirometry ANY cases, use FEV1/FVC 65% alone to match, or FEV1/FVC 75% plus oxygen saturation 88% to match the alternative criterion. FEV1/FVC 75% plus oxygen saturation 95% fails both. FEV1/FVC 75% without oxygen saturation requests more evidence. FEV1/FVC exactly 70% fails its exclusive LT boundary. An empty observation list also requests evidence. The osteoporosis alternatives demonstrate a matched ANY condition taking precedence over another missing condition.

GT, GTE, and EQ remain supported and covered by the evaluator's unit tests. The seed does not add unrelated clinical requirements just to exercise those operators.

## Manual decisions and evidence

Create a coronary-bypass request to reach manual review. Submit `POST /api/v1/reviews/{id}/decision` with reviewer ID, `decision`, `decisionReason`, and approved quantity 1 for approval. Rejection must omit approved quantity. Approval quantity 0, a negative quantity, or quantity 2 is rejected. A second decision returns a conflict and preserves the first decision. These calls create completed manual approval/rejection records through the normal code path.

Use `POST /api/v1/reviews/{id}/evidence-requests` on a manual review. Its body contains reviewer ID and a structured evidence request:

```json
{
  "reviewerId": "a0000000-0000-4000-8000-000000000001",
  "evidenceRequest": {
    "summary": "Provide the recent echocardiogram and coronary angiography report.",
    "requestedConditions": [],
    "requestedObservations": ["10230-1"],
    "otherEvidence": "Coronary angiography report and current treatment plan"
  }
}
```

Use `PATCH /api/v1/requests/{id}/evidence` with the original provider ID and a clinical justification containing the additional evidence. A system-requested missing-echo case returns to automatic evaluation; a reviewer-requested case returns to manual review. Evidence is merged, `evidenceUpdatedAt` is exposed, and the new timestamp becomes the evaluation cutoff. Submit an observation recorded after the original submission but before the new evidence submission to exercise that behavior. A summary-only submission is accepted, as are condition-only and observation-only additions. An empty evidence submission is rejected. You can replace an evidence request while a review is awaiting evidence.

## API errors and defensive branches

Use an existing request UUID for duplicate submission. Unknown reference IDs, missing required JSON fields, invalid UUIDs/enums, and malformed JSON exercise submission errors. Use provider 2 with organization 1 for a known-provider organization mismatch. Submit evidence using a different provider to exercise ownership rejection. Evidence submission for a request without a review, a review not awaiting evidence, or a completed request returns a conflict. Decisions or evidence requests outside the allowed review states return a conflict.

Query with provider 2 or 3 and a status filter they have no requests for to get an empty result. Unknown provider/review/request identifiers and invalid query parameters exercise query errors. Reprocessing a completed request or non-pending review exercises scheduler guards.

Some defensive branches cannot be represented by valid seed rows: a missing plan on an existing request is prevented by the request foreign key; missing policy enums and null clinical justification are prevented by NOT NULL; invalid enum values are not valid entity data. Those remain unit-test scenarios. The seed supports all persisted workflow outcomes and meaningful policy/evidence variations through API calls without creating invalid database records.

## Policy document ingestion

Each policy explicitly includes source_file_name and ingestion_status in data/policy.json. The existing generic loader inserts and updates both fields. Filenames resolve against payer.policy-documents.base-directory in application.yaml. All six policies start as NOT_INGESTED. Rerunning the seed resets them to NOT_INGESTED and schedules reingestion; existing vector rows are cleared by the reset.

The policy ingestion scheduler uses payer.scheduler.policy-ingestion-delay (5000 milliseconds by default) for both its initial delay and fixed delay between completed runs. It queries NOT_INGESTED policies in ID order and continues after individual failures. Successfully ingested policies are skipped on later runs. Failed policies remain pending and are retried on the next run.

Run the seed loader unit tests without connecting to PostgreSQL:

```powershell
python -m unittest discover -s payer/seed -p test_seed.py
```

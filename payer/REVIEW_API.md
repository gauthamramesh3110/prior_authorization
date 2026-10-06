# Review APIs

## Review queue

```http
GET /api/v1/reviews
GET /api/v1/reviews?status=PENDING_MANUAL_REVIEW
```

Both return the manual review queue. The optional `status` filter also accepts `PENDING_EVALUATION`, `AWAITING_EVIDENCE`, and `DECIDED`.

The response is a JSON array, ordered by `lastUpdated` ascending, then review `id` ascending when timestamps match. An empty queue returns `200 OK` with `[]`. All matching reviews are returned.

Each item contains `id`, `requestId`, `reviewStatus`, `requestStatus`, `requestStatusReason`, patient/provider/organization/plan IDs, `requestedService`, `submittedAt`, and `lastUpdated`. Clinical evidence is included in the details response.

## Review details

```http
GET /api/v1/reviews/{id}
```

Returns `200 OK` for a review in any status. The response contains:

- Review ID, status, last-updated time, decision, reason, decision date, actor, reviewer ID, validity dates, and approved quantity.
- `request`: request ID, patient/provider/organization/plan IDs, request status/reason, requested service, clinical justification, original `submittedAt`, and nullable `evidenceUpdatedAt`.
- `evidenceRequest`: the latest structured request for evidence, or `null` before one is recorded.
- `policy`: policy ID, review mode, match rule, and criteria with evidence type, code, operator, threshold value, and unit.

`policy` is `null` when the plan service or policy is unconfigured. It reflects the current policy configuration; the schema does not currently store the policy version used for an earlier evaluation. Decision fields are `null` before a decision is made.

An unknown review ID returns `404 Not Found`. An invalid UUID or status returns `400 Bad Request`. Reading either endpoint leaves workflow state unchanged.


## Manual decision

```http
POST /api/v1/reviews/{id}/decision
Content-Type: application/json
```

Approval example:

```json
{
  "reviewerId": "7b4c27ca-7dac-4c92-a619-640d3dbb6e96",
  "decision": "APPROVED",
  "decisionReason": "Clinical review supports the requested service",
  "approvedQuantity": 3
}
```

Rejection example:

```json
{
  "reviewerId": "7b4c27ca-7dac-4c92-a619-640d3dbb6e96",
  "decision": "REJECTED",
  "decisionReason": "Submitted evidence does not support the requested service"
}
```

`reviewerId`, `decision`, and a nonblank `decisionReason` are required. Approval requires a positive `approvedQuantity` no greater than the requested quantity. Rejection must omit `approvedQuantity` or set it to `null`.

Only reviews in `PENDING_MANUAL_REVIEW` with a `PENDING` authorization request can be decided. A successful submission returns `200 OK` with the review/request IDs, resulting statuses, decision, reason, reviewer, decision date, approved quantity, and validity dates.

Approval changes the request to `APPROVED` / `MANUAL_APPROVED`. Rejection changes it to `REJECTED` / `MANUAL_REJECTED`. Both change the review to `DECIDED` and set `decidedBy` to `REVIEWER`.

The server supplies the decision time. Approval is valid from that time for 30 days, matching auto-approval. Rejection has no approved quantity or validity dates. Reviewer identity is supplied in the request body.

The service saves the request and review within one transaction. The review stores the current decision, reviewer, reason, decision time, quantity, and validity dates.

An unknown review returns `404 Not Found`. A review outside the manual queue, an authorization request that is no longer pending, or a repeated decision returns `409 Conflict`. Invalid input or quantity returns `400 Bad Request`.


## Request evidence

```http
POST /api/v1/reviews/{id}/evidence-requests
Content-Type: application/json
```

Example:

```json
{
  "reviewerId": "7b4c27ca-7dac-4c92-a619-640d3dbb6e96",
  "evidenceRequest": {
    "summary": "Please provide an ejection fraction result and its supporting report",
    "requestedConditions": [],
    "requestedObservations": ["10230-1"],
    "otherEvidence": "Echocardiogram report"
  }
}
```

`reviewerId`, `evidenceRequest`, and a nonblank `evidenceRequest.summary` are required. `requestedConditions` and `requestedObservations` are simple lists of codes or text. Omitted lists are stored as empty lists; supplied items must be nonblank. `otherEvidence` is optional text. Summary-only requests are allowed.

The review must be `PENDING_MANUAL_REVIEW` or `AWAITING_EVIDENCE`, and its authorization request must be `PENDING`. The service changes the review to `AWAITING_EVIDENCE`, sets the request reason to `AWAITING_EVIDENCE`, and records the requesting reviewer and current time. Original submission time and clinical evidence are preserved; decision fields remain unset.

The request and review are saved within one transaction. Another request while awaiting evidence replaces the current `evidenceRequest` on the review and updates the reviewer and timestamp.

A successful call returns `200 OK` with `reviewId`, `requestId`, `reviewerId`, `evidenceRequest`, `requestedAt`, `reviewStatus`, `requestStatus`, and `requestStatusReason`.

The current request is stored in `review.evidence_request` as JSONB. Apply Flyway migration `V5__review_evidence_request.sql`; earlier rows initially have `null`. Both reviewer details and provider request details expose it. Automatic evaluation fills the lists with missing condition and observation codes, while manual reviewers supply the fields above. The latest request remains available after evidence submission or a decision; `reviewStatus` indicates whether evidence is still awaited.

An unknown review returns `404 Not Found`. A completed review, a review still pending automatic evaluation, or an authorization request that is no longer pending returns `409 Conflict`. Invalid input returns `400 Bad Request`.


## Submit evidence

```http
PATCH /api/v1/requests/{id}/evidence
Content-Type: application/json
```

Use the authorization request ID in the path. Example:

```json
{
  "providerId": "6f72bfe4-1839-4b22-98c3-3f995074aa63",
  "clinicalJustification": {
    "summary": "Updated echocardiogram results",
    "observations": [
      {
        "code": "10230-1",
        "value": 35.1,
        "units": "%",
        "description": "Left ventricular ejection fraction",
        "recordedAt": "2026-10-05T10:00:00Z"
      }
    ]
  }
}
```

`providerId` and `clinicalJustification` are required. The provider must match the original request. Identity is supplied in the body, consistent with the reviewer endpoints; this is an ownership check, not authentication.

Submit at least one condition, observation, or nonblank summary. Conditions and observations use the same fields and validation as the original authorization request. Omitted lists add nothing. Evidence items cannot be null.

The review must be `AWAITING_EVIDENCE`, and the authorization request must be `PENDING`. New conditions and observations are appended to the existing lists. New summary text is appended with a blank line between entries. Earlier evidence, request IDs, the requested service, and original `submittedAt` are preserved.

If a reviewer requested the evidence, the review returns to `PENDING_MANUAL_REVIEW` and retains its reviewer ID. Evidence awaited by the automated evaluator returns to `PENDING_EVALUATION` for the existing scheduler. The authorization request remains `PENDING` with reason `EVIDENCE_UPDATED`. Submission does not make a decision or verify that every requested item has been supplied; the next review evaluates the combined evidence.

The server records `evidenceUpdatedAt`. Automatic evaluation uses that timestamp as the clinical evidence cutoff, so results collected after the original request can be considered. Initial reviews continue to use `submittedAt`. Coverage and network checks keep the original submission date. Apply Flyway migration `V4__evidence_updated_at.sql` for this new nullable column.

The request and review are saved in one transaction. The request stores the combined evidence and its latest update timestamp. The latest `evidenceRequest` remains available on the review.

A successful call returns `200 OK` with `requestId`, `reviewId`, `providerId`, `reviewStatus`, `requestStatus`, `requestStatusReason`, and `evidenceUpdatedAt`. Updated combined evidence is available through review details.

An unknown authorization request returns `404 Not Found`. A provider mismatch returns `403 Forbidden`. A request without a review, a review outside `AWAITING_EVIDENCE`, or a request that is no longer pending returns `409 Conflict`. A repeated submission also returns `409` until the review requests evidence again. Invalid or empty evidence returns `400 Bad Request`.

Apply `V6__remove_review_history.sql` to drop the obsolete `review_history` table. The timeline endpoint has been removed, and evidence responses no longer include a history event `id`. Current requests, reviews, clinical evidence, evidence requests, and decisions are retained.

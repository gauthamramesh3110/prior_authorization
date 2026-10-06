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
- `request`: request ID, patient/provider/organization/plan IDs, request status/reason, requested service, clinical justification, and original submission time.
- `policy`: policy ID, review mode, match rule, and criteria with evidence type, code, operator, threshold value, and unit.

`policy` is `null` when the plan service or policy is unconfigured. It reflects the current policy configuration; the schema does not currently store the policy version used for an earlier evaluation. Decision fields are `null` before a decision is made.

An unknown review ID returns `404 Not Found`. An invalid UUID or status returns `400 Bad Request`. Reading either endpoint leaves workflow state and history unchanged.


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

The service saves the request, review, and history within one transaction. The history event has source `REVIEWER` and type `MANUAL_APPROVED` or `MANUAL_REJECTED`, with the reviewer, reason, decision time, quantity, and validity dates in its snapshot.

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
  "message": "Please provide an ejection fraction result and its supporting report",
  "requestedEvidence": [
    "EF observation with value, units, and recorded date",
    "Echocardiogram report"
  ]
}
```

All fields are required. `message` must be nonblank, and `requestedEvidence` must contain at least one nonblank item.

The review must be `PENDING_MANUAL_REVIEW` or `AWAITING_EVIDENCE`, and its authorization request must be `PENDING`. The service changes the review to `AWAITING_EVIDENCE`, sets the request reason to `AWAITING_EVIDENCE`, and records the requesting reviewer and current time. Original submission time and clinical evidence are preserved; decision fields remain unset.

The request, review, and history are saved within one transaction. The history event has source `REVIEWER`, type `EVIDENCE_REQUESTED`, and a snapshot containing the message, requested items, reviewer ID, and resulting statuses. Another request while awaiting evidence appends a new event and preserves earlier events.

A successful call returns `200 OK` with `id` (the history event ID), `reviewId`, `requestId`, `reviewerId`, `message`, `requestedEvidence`, `requestedAt`, `reviewStatus`, `requestStatus`, and `requestStatusReason`.

An unknown review returns `404 Not Found`. A completed review, a review still pending automatic evaluation, or an authorization request that is no longer pending returns `409 Conflict`. Invalid input returns `400 Bad Request`.

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

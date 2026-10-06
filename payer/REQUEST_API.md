# Provider request tracking APIs

## Provider request list

```http
GET /api/v1/requests?providerId={providerId}
GET /api/v1/requests?providerId={providerId}&status=PENDING
```

`providerId` is required. The optional `status` filter accepts `SUBMITTED`, `PENDING`, `APPROVED`, and `REJECTED`. Filtering always applies to the selected provider.

The response is a JSON array of all matching requests, ordered by original `submittedAt` descending, then request `id` ascending when timestamps match. Each item contains:

- `id`, `patientId`, `providerId`, `organizationId`, and `planId`.
- `requestStatus`, `requestStatusReason`, and `requestedService`.
- Original `submittedAt` and nullable `evidenceUpdatedAt`.
- Nullable `reviewId` and `reviewStatus`.

Requests are included before review creation and when no review is needed. Their review fields are `null`. A pending request can have reason `AWAITING_EVIDENCE` or `EVIDENCE_UPDATED`; its review status shows whether it awaits evidence, automatic evaluation, or manual review. Clinical evidence is available through request details.

A known provider with no matching requests receives `200 OK` with `[]`. An unknown provider receives `404 Not Found`. Missing or invalid provider IDs and invalid status values return `400 Bad Request`.

## Provider request details

```http
GET /api/v1/requests/{id}?providerId={providerId}
```

Use the authorization request ID in the path and its original provider ID in the required query parameter. The response contains:

- `request`: the same fields as a request-list item.
- `clinicalJustification`: current combined clinical evidence.
- `review`: review ID/status, `lastUpdated`, decision/reason/date, decision actor, reviewer ID, approved quantity, and validity dates.

`review` is `null` when no review exists. Decision fields are `null` until a decision is made. Intake rejections and approvals that do not require prior authorization remain trackable without a review.

An unknown request returns `404 Not Found`. A provider ID that does not match the request returns `403 Forbidden`. Missing or invalid UUIDs return `400 Bad Request`.

Both endpoints only read data. Provider identity is supplied through `providerId`; these checks do not authenticate the caller.

## Evidence requests and review timeline

When a request has a `reviewId`, use:

```http
GET /api/v1/reviews/{reviewId}/history
```

`EVIDENCE_REQUESTED` events include the reviewer's message and requested items. `UPDATED_EVIDENCE` events show submitted additions. The request details contain the merged current evidence. History is ordered oldest first and preserves earlier snapshots. See [review history](REVIEW_API.md#review-history).

Submit additional evidence through `PATCH /api/v1/requests/{id}/evidence`, as described in [submit evidence](REVIEW_API.md#submit-evidence).

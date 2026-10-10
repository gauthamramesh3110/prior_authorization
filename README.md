# Prior Authorization with Spring AI

A local prior-authorization project using Spring Boot, Spring AI, PostgreSQL with pgvector, and Ollama. The AI assists manual review using submitted evidence and retrieved policy passages. Cases use synthetic data, and a reviewer makes the final decision.

## Docker setup

Requirements: Docker Desktop, JDK 26 for the payer application, and Python 3 for seeding.
Run the following commands from the repository root. Quit native Ollama first if it is using port 11434.

```powershell
docker compose up -d
docker compose exec ollama ollama pull phi4-mini
docker compose exec ollama ollama pull nomic-embed-text:v1.5
docker compose ps
docker compose exec ollama ollama list
```

Compose starts PostgreSQL on `localhost:5434` and Ollama on `localhost:11434`.
Database data and models persist in named Docker volumes. The payer application runs separately on the host.

Start [PayerApplication.java](payer/src/main/java/com/lifeforce/payer/PayerApplication.java) from your IDE with the **repository root as the working directory**. The API listens on `http://localhost:8082`.

For the first setup, start the application once to apply Flyway migrations, then stop it and load the reference data:

```powershell
python -m pip install -r payer/seed/requirements.txt
python payer/seed/seed.py
```

The seed script resets existing application data, including requests, reviews, and policy embeddings. Restart the payer afterward and allow policy ingestion to finish before requesting an AI summary. See [seed instructions](payer/seed/README.md) and [policy setup](payer/seed/policy-documents/README.md) for details.

## Manual-review AI workflow

Open the **Payer** Postman collection and send these three requests in order:

| Step | Postman request | Endpoint | Purpose |
| --- | --- | --- | --- |
| 1 | Prior Auth Request - Manual Review (Bone Density Screening) | `POST http://localhost:8082/api/v1/requests` | Submit the saved bone-density screening case. Expect `201` with `responseStatus: SUBMITTED`. |
| 2 | Get All Manual Reviews | `GET http://localhost:8082/api/v1/reviews` | List reviews with status `PENDING_MANUAL_REVIEW`, the endpoint's default filter. |
| 3 | Review - Generate Assistant Summary | `POST http://localhost:8082/api/v1/reviews/{{reviewId}}/assistant-summary` | Generate a draft assessment. Expect `200` with `reviewId` and `summary`. No request body is required. |

The first request's pre-request script generates a fresh request UUID and dates. Its response script saves the submitted UUID as `lastManualReviewRequestId`.

Processing is asynchronous. After step 1, allow a few seconds for the schedulers to create the review, then refresh step 2 if needed. Find the review whose `requestId` matches `lastManualReviewRequestId`, and copy that review's **`id`** into the Postman **`reviewId`** variable before step 3. A review ID is different from the submitted request ID.

The screening policy is configured for `MANUAL_REVIEW`, so the case requires human review even when its clinical evidence supports an indication. Generating an AI summary does not submit an authorization decision.

CPU inference can take several minutes. Allow a request timeout of `600000` milliseconds (10 minutes) in your client where supported.

### Example assistant response

The following response was captured from a local run of step 3. Your review ID, dates, and generated wording will vary.

This illustrates the response format, rather than a validated assessment. The claim that requirements 1 through 14 are all met is a known model interpretation error: alternative qualifying routes should not be treated as one combined checklist. The reviewer must verify the draft against the policy and submitted evidence.

```json
{
    "reviewId": "34fd64a7-ed0e-4595-9ab1-aef5513f9c50",
    "summary": "1. Requested service: Bone density scan (procedure) - SNOMED-CT 312681000, requested date: Thu Oct 15 20:00:00 EDT 2026, quantity: 1\n\n2. Criteria assessment:\n   - Met: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 (All requirements are met based on the submitted clinical narrative and evidence)\n   - Not met: N/A\n   - Insufficient evidence: N/A\n   - Not applicable: N/A\n   - Supporting case facts: The patient is a 68-year-old postmenopausal woman with no prior DXA or bone mineral density study, and the scan is for initial screening to establish baseline bone mineral density, inform fracture-risk assessment, and guide whether osteoporosis treatment evaluation is needed.\n   - Policy file and section number: hhi-mp-001-bone-density-scan.md, Section 2 (Medical-necessity decision structure and qualifying routes)\n\n3. Evidence sufficiency: All applicable clinical requirements can be assessed. No specific required facts are still missing.\n\n4. Draft recommendation: Approve. The requested bone density scan meets the medical-necessity criteria for an initial screening examination in a member age 18 years and older."
}
```

## Stop the containers

```powershell
docker compose stop
```

For Ollama-specific commands, see [OLLAMA.md](OLLAMA.md).

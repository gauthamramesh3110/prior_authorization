# Harbor Choice PPO policy document

The sole clinical source is [HHI-MP-001: Bone Mineral Density Measurement](hhi-mp-001-bone-density-scan.md), version 1.0.0, effective October 9, 2026. It contains 31 numbered sections covering adult screening, diagnostic assessment, defined secondary causes, medication exposure, treatment monitoring, methods, repeat intervals, documentation, and physician exceptions.

## Application configuration

Policy `70000000-0000-4000-8000-000000000001` maps service `312681000` to this document. Its review mode is `MANUAL_REVIEW`, with no executable condition criteria. The clinical requirements live in the document; an empty criteria array does not mean unconditional approval. A clinically complete qualifying request remains `PENDING_MANUAL_REVIEW` until the reviewer records a decision.

The other five policy records and their service rules remain in seed data, but their `source_file_name` values are null. They have no clinical document to retrieve. There are no other policy-document files or prior file versions in this directory.

## Fresh seed and ingestion

Stop the payer application and run `python payer/seed/seed.py` against the local payer database. The loader checks referenced document files before resetting data. A successful run clears application data, including requests, reviews, and vectors, and reloads the curated reference rows. Flyway history and schema are retained.

Start the payer application with Ollama available. The ingestion scheduler will index this policy's 31 root sections and mark it `INGESTED`. Policies with null source filenames are skipped. Search metadata includes the policy ID, source filename, section ID, heading, and chunk index.

The file uses the current ingestion heading format. Keep the complete operative passage with its filename and heading when returning references. Three retrieved sections are not a guarantee that every applicable indication, method, and interval rule was supplied to the model. Review the actual returned citations when assessing a generated summary.

## Passing clinical request

The [initial screening request](../examples/bone-density-screening-request.json) describes a 68-year-old postmenopausal woman requesting one initial DXA of the lumbar spine and hip, with no prior BMD study. It supports section 4.A without an osteoporosis diagnosis or fracture. The [Postman pre-request script](../examples/bone-density-screening-pre-request.js) generates a fresh request ID and dates for each submission.

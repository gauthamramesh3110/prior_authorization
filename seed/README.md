# Synthea seed

Run Flyway migrations V1-V3 before seeding. From the repository root:

```powershell
python -m pip install -r seed/requirements.txt
python seed/seed.py --dry-run
python seed/seed.py
```

The default database is `postgresql://payer:payer@localhost:5434/payer_db`.
Use `--database-url` or `DATABASE_URL` to select another database.

The script selects Humana patients from `raw_data`, then loads their providers and organizations from encounters. Synthea UUIDs are preserved. Generated plan, coverage, policy, criterion, and service IDs are stable across runs.

Dates are not shifted. Coverage years are inclusive in the source: 2018-2020 becomes [2018-01-01 UTC, 2021-01-01 UTC). Use historical request submission dates within the printed coverage range.

`configuration.json` contains a fictional plan and clinical examples and explicit fallback scenarios. Descriptions help identify codes and are not database columns. Clinical examples are simplified from published guidance. Benefit and PA choices remain fictional. See [POLICIES.md](POLICIES.md) for sources, limitations, and a complete scenario matrix.

For the demo, organizations seen in Humana-paid encounters are marked in-network. Other related organizations are marked out-of-network. Each row starts at the earliest selected coverage year and has no end date. This is a fixed demo assumption, not historical contract data.

Only reference and configuration tables are loaded. Requests, reviews, and history are preserved. Conditions and observations remain in the CSVs for building request evidence later.

All inserts run in one transaction. Repeating the seed skips existing reference rows and refreshes policy, criterion, and service configuration. Removed configuration rows are not automatically deleted; use a new policy name when removing criteria from an existing policy. Python exceptions are shown directly rather than handled by custom recovery logic.

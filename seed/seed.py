import argparse
import csv
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5


SEED_DIRECTORY = Path(__file__).resolve().parent
RAW_DATA_DIRECTORY = SEED_DIRECTORY / "raw_data"


def read_csv(filename):
    with (RAW_DATA_DIRECTORY / filename).open(encoding="utf-8", newline="") as csv_file:
        yield from csv.DictReader(csv_file)


def create_seed_id(name):
    return uuid5(NAMESPACE_URL, f"prior-authorization-seed:{name}")


def start_of_year(year):
    return datetime(int(year), 1, 1, tzinfo=timezone.utc)


def build_reference_data(configuration):
    payer_id = configuration["payer_id"]
    plan_id = create_seed_id(f"plan:{payer_id}")
    coverage_spans = [
        row for row in read_csv("payer_transitions.csv") if row["PAYER"] == payer_id
    ]
    patient_ids = {row["PATIENT"] for row in coverage_spans}
    encounters = [
        row for row in read_csv("encounters.csv") if row["PATIENT"] in patient_ids
    ]
    provider_ids = {row["PROVIDER"] for row in encounters}
    providers = [
        row for row in read_csv("providers.csv") if row["Id"] in provider_ids
    ]
    organization_ids = {row["ORGANIZATION"] for row in encounters}
    organization_ids.update(row["ORGANIZATION"] for row in providers)
    in_network_organization_ids = {
        row["ORGANIZATION"] for row in encounters if row["PAYER"] == payer_id
    }
    network_start = start_of_year(min(int(row["START_YEAR"]) for row in coverage_spans))

    return {
        "payer": [
            (row["Id"], row["NAME"])
            for row in read_csv("payers.csv")
            if row["Id"] == payer_id
        ],
        "plan": [(plan_id, payer_id, configuration["plan_name"])],
        "organization": [
            (row["Id"], row["NAME"])
            for row in read_csv("organizations.csv")
            if row["Id"] in organization_ids
        ],
        "provider": [
            (row["Id"], row["ORGANIZATION"], row["NAME"]) for row in providers
        ],
        "patient": [
            (row["Id"], f'{row["FIRST"]} {row["LAST"]}')
            for row in read_csv("patients.csv")
            if row["Id"] in patient_ids
        ],
        "coverage": [
            (
                create_seed_id(
                    f'coverage:{plan_id}:{row["PATIENT"]}:{row["START_YEAR"]}:{row["END_YEAR"]}'
                ),
                row["PATIENT"],
                plan_id,
                start_of_year(row["START_YEAR"]),
                start_of_year(int(row["END_YEAR"]) + 1),
            )
            for row in coverage_spans
        ],
        "network_participation": [
            (
                payer_id,
                organization_id,
                organization_id in in_network_organization_ids,
                network_start,
                None,
            )
            for organization_id in sorted(organization_ids)
        ],
    }


def build_policy_data(configuration):
    plan_id = create_seed_id(f'plan:{configuration["payer_id"]}')
    policies = []
    criteria = []
    plan_services = []

    for policy in configuration["policies"]:
        policy_id = create_seed_id(f'policy:{policy["name"]}')
        policies.append((policy_id, policy["review_mode"], policy["match"]))

        for index, criterion in enumerate(policy["criteria"]):
            criteria.append(
                (
                    create_seed_id(f'criterion:{policy["name"]}:{index}'),
                    policy_id,
                    criterion["evidence_type"],
                    criterion["code"],
                    criterion["operator"],
                    criterion.get("value"),
                    criterion.get("unit"),
                )
            )

    for service in configuration["services"]:
        policy_name = service["policy"]
        policy_id = create_seed_id(f"policy:{policy_name}") if policy_name else None
        plan_services.append(
            (
                create_seed_id(f'plan-service:{plan_id}:{service["code"]}'),
                plan_id,
                service["code"],
                "PROCEDURE",
                service["benefit_status"],
                service["prior_authorization_required"],
                policy_id,
            )
        )

    return {
        "policy": policies,
        "policy_criterion": criteria,
        "plan_service": plan_services,
    }


def load_data(database_url, data):
    import psycopg

    insert_statements = {
        "payer": "INSERT INTO payer (id, name) VALUES (%s, %s) ON CONFLICT DO NOTHING",
        "plan": "INSERT INTO plan (id, payer_id, name) VALUES (%s, %s, %s) ON CONFLICT DO NOTHING",
        "organization": "INSERT INTO organization (id, name) VALUES (%s, %s) ON CONFLICT DO NOTHING",
        "provider": "INSERT INTO provider (id, organization_id, name) VALUES (%s, %s, %s) ON CONFLICT DO NOTHING",
        "patient": "INSERT INTO patient (id, name) VALUES (%s, %s) ON CONFLICT DO NOTHING",
        "coverage": """
            INSERT INTO coverage (id, patient_id, plan_id, start, "end")
            VALUES (%s, %s, %s, %s, %s) ON CONFLICT DO NOTHING
        """,
        "network_participation": """
            INSERT INTO network_participation
                (payer_id, organization_id, in_network, effective_from, effective_to)
            VALUES (%s, %s, %s, %s, %s) ON CONFLICT DO NOTHING
        """,
        "policy": """
            INSERT INTO policy (id, review_mode, match)
            VALUES (%s, %s, %s) ON CONFLICT (id) DO UPDATE
            SET review_mode = EXCLUDED.review_mode, match = EXCLUDED.match
        """,
        "policy_criterion": """
            INSERT INTO policy_criterion
                (id, policy_id, evidence_type, code, operator, value, unit)
            VALUES (%s, %s, %s, %s, %s, %s, %s) ON CONFLICT (id) DO UPDATE
            SET evidence_type = EXCLUDED.evidence_type, code = EXCLUDED.code,
                operator = EXCLUDED.operator, value = EXCLUDED.value, unit = EXCLUDED.unit
        """,
        "plan_service": """
            INSERT INTO plan_service
                (id, plan_id, code, code_type, benefit_status, prior_authorization_required, policy_id)
            VALUES (%s, %s, %s, %s, %s, %s, %s) ON CONFLICT (plan_id, code_type, code) DO UPDATE
            SET benefit_status = EXCLUDED.benefit_status,
                prior_authorization_required = EXCLUDED.prior_authorization_required,
                policy_id = EXCLUDED.policy_id
        """,
    }

    with psycopg.connect(database_url) as connection:
        with connection.cursor() as cursor:
            for table_name, rows in data.items():
                cursor.executemany(insert_statements[table_name], rows)


def main():
    argument_parser = argparse.ArgumentParser(description="Seed historical Synthea reference data.")
    argument_parser.add_argument("--dry-run", action="store_true")
    argument_parser.add_argument(
        "--database-url",
        default=os.getenv("DATABASE_URL", "postgresql://payer:payer@localhost:5434/payer_db"),
    )
    arguments = argument_parser.parse_args()

    configuration = json.loads((SEED_DIRECTORY / "configuration.json").read_text(encoding="utf-8"))
    data = build_reference_data(configuration)
    data.update(build_policy_data(configuration))

    print(f'Plan: {configuration["plan_name"]} ({data["plan"][0][0]})')
    for table_name, rows in data.items():
        print(f"{table_name}: {len(rows)} source rows")
    coverage_start = min(row[3] for row in data["coverage"])
    coverage_end = max(row[4] for row in data["coverage"])
    print(f"Coverage dates: {coverage_start.date()} to {coverage_end.date()} (exclusive)")

    if arguments.dry_run:
        print("Dry run complete. No database changes.")
        return

    load_data(arguments.database_url, data)
    print("Seed complete. Policy configuration refreshed; reference and workflow rows preserved.")


if __name__ == "__main__":
    main()

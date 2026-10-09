import json
import os
from pathlib import Path

import psycopg
from psycopg import sql


SEED_DIRECTORY = Path(__file__).resolve().parent
DATA_DIRECTORY = SEED_DIRECTORY / "data"
POLICY_DOCUMENT_DIRECTORY = SEED_DIRECTORY / "policy-documents"


def read_rows(table_name):
    return json.loads((DATA_DIRECTORY / f"{table_name}.json").read_text(encoding="utf-8"))


def ingest_rows(cursor, table_name, rows):
    columns = list(rows[0])
    conflict_columns = (
        ["payer_id", "organization_id", "effective_from"]
        if table_name == "network_participation"
        else ["id"]
    )
    updates = sql.SQL(", ").join(
        sql.SQL("{column} = EXCLUDED.{column}").format(column=sql.Identifier(column))
        for column in columns
        if column not in conflict_columns
    )
    statement = sql.SQL(
        "INSERT INTO {table} ({columns}) VALUES ({values}) "
        "ON CONFLICT ({conflict_columns}) DO UPDATE SET {updates}"
    ).format(
        table=sql.Identifier(table_name),
        columns=sql.SQL(", ").join(sql.Identifier(column) for column in columns),
        values=sql.SQL(", ").join(sql.Placeholder() for column in columns),
        conflict_columns=sql.SQL(", ").join(sql.Identifier(column) for column in conflict_columns),
        updates=updates,
    )
    cursor.executemany(statement, [[row[column] for column in columns] for row in rows])


def validate_policy_documents(policies):
    for policy in policies:
        source_file_name = policy.get("source_file_name")
        if source_file_name is not None:
            document = POLICY_DOCUMENT_DIRECTORY / source_file_name
            if not document.is_file():
                raise FileNotFoundError(
                    f"Policy {policy['id']} references a missing document: {document}"
                )
            document.read_text(encoding="utf-8")


def truncate_tables(cursor):
    cursor.execute(
        "SELECT schemaname, tablename FROM pg_catalog.pg_tables "
        "WHERE schemaname = %s AND tablename <> %s ORDER BY tablename",
        ("public", "flyway_schema_history"),
    )
    tables = cursor.fetchall()
    if tables:
        cursor.execute(
            sql.SQL("TRUNCATE TABLE {} RESTART IDENTITY").format(
                sql.SQL(", ").join(sql.Identifier(schema, table) for schema, table in tables)
            )
        )


def main():
    policies = read_rows("policy")
    validate_policy_documents(policies)
    database_url = os.environ.get(
        "PAYER_DATABASE_URL", "postgresql://payer:payer@localhost:5434/payer_db"
    )
    with psycopg.connect(database_url) as connection:
        with connection.cursor() as cursor:
            truncate_tables(cursor)
            ingest_rows(cursor, "payer", read_rows("payer"))
            plan_services = []
            for plan in read_rows("plan"):
                services = plan.pop("plan_services")
                ingest_rows(cursor, "plan", [plan])
                for service in services:
                    service["plan_id"] = plan["id"]
                    plan_services.append(service)
            for table_name in ["organization", "provider", "patient", "coverage", "network_participation"]:
                ingest_rows(cursor, table_name, read_rows(table_name))
            for policy in policies:
                criteria = policy.pop("criteria")
                ingest_rows(cursor, "policy", [policy])
                for criterion in criteria:
                    criterion["policy_id"] = policy["id"]
                if criteria:
                    ingest_rows(cursor, "policy_criterion", criteria)
            if plan_services:
                ingest_rows(cursor, "plan_service", plan_services)
    document_count = sum(policy.get("source_file_name") is not None for policy in policies)
    print(
        f"Payer tables reset and seed data ingested: {len(policies)} policies, "
        f"{document_count} policy document(s). Requests, reviews, and vectors are empty."
    )


if __name__ == "__main__":
    main()

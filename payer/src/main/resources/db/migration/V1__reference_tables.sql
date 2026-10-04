CREATE TABLE payer (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE plan (
    id UUID PRIMARY KEY,
    payer_id UUID NOT NULL REFERENCES payer (id),
    name TEXT NOT NULL
);

CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE provider (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization (id),
    name TEXT NOT NULL
);

CREATE TABLE patient (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE coverage (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient (id),
    plan_id UUID NOT NULL REFERENCES plan (id),
    start TIMESTAMPTZ NOT NULL,
    "end" TIMESTAMPTZ
);

CREATE TABLE network_participation (
    payer_id UUID NOT NULL REFERENCES payer (id),
    organization_id UUID NOT NULL REFERENCES organization (id),
    in_network BOOLEAN NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    PRIMARY KEY (payer_id, organization_id, effective_from)
);

CREATE INDEX idx_plan_payer ON plan (payer_id);
CREATE INDEX idx_provider_organization ON provider (organization_id);
CREATE INDEX idx_coverage_patient_plan_start ON coverage (patient_id, plan_id, start);
CREATE INDEX idx_coverage_plan ON coverage (plan_id);
CREATE INDEX idx_network_organization ON network_participation (organization_id);

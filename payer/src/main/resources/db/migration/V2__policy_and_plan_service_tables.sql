CREATE TABLE policy (
    id UUID PRIMARY KEY,
    review_mode TEXT NOT NULL,
    match TEXT NOT NULL
);

CREATE TABLE policy_criterion (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL REFERENCES policy (id),
    evidence_type TEXT NOT NULL,
    code TEXT NOT NULL,
    operator TEXT NOT NULL,
    value NUMERIC,
    unit TEXT
);

CREATE TABLE plan_service (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES plan (id),
    code TEXT NOT NULL,
    code_type TEXT NOT NULL,
    benefit_status TEXT NOT NULL,
    prior_authorization_required BOOLEAN NOT NULL,
    policy_id UUID REFERENCES policy (id),
    UNIQUE (plan_id, code_type, code)
);

CREATE INDEX idx_policy_criterion_policy ON policy_criterion (policy_id);
CREATE INDEX idx_plan_service_policy ON plan_service (policy_id);

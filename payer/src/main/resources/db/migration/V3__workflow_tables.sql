CREATE TABLE authorization_request (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient (id),
    provider_id UUID NOT NULL REFERENCES provider (id),
    organization_id UUID NOT NULL REFERENCES organization (id),
    plan_id UUID NOT NULL REFERENCES plan (id),
    status TEXT NOT NULL,
    status_reason TEXT,
    requested_service JSONB NOT NULL,
    clinical_justification JSONB NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE review (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE REFERENCES authorization_request (id),
    status TEXT NOT NULL,
    decision TEXT,
    decision_reason TEXT,
    decision_date TIMESTAMPTZ,
    decided_by TEXT,
    last_updated TIMESTAMPTZ NOT NULL,
    reviewer_id UUID,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    approved_quantity NUMERIC
);

CREATE TABLE review_history (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES review (id),
    event_source TEXT NOT NULL,
    event_payload JSONB NOT NULL,
    event_type TEXT NOT NULL,
    event_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_authorization_request_queue
    ON authorization_request (status, submitted_at, id);
CREATE INDEX idx_authorization_request_provider
    ON authorization_request (provider_id, submitted_at);
CREATE INDEX idx_review_queue
    ON review (status, last_updated, id);
CREATE INDEX idx_review_history_timeline
    ON review_history (review_id, event_at, id);

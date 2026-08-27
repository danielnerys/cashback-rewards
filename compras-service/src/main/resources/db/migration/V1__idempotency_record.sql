CREATE TABLE idempotency_record (
    idempotency_key   TEXT PRIMARY KEY,
    request_hash      TEXT NOT NULL,
    response_body     JSONB NOT NULL,
    response_status   INTEGER NOT NULL,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);

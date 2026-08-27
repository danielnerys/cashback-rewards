CREATE TABLE outbox (
    id              UUID PRIMARY KEY,
    tipo_evento     TEXT NOT NULL,
    payload         JSONB NOT NULL,
    idempotency_key TEXT NOT NULL,
    estado          TEXT NOT NULL DEFAULT 'PENDENTE',
    tentativas      INTEGER NOT NULL DEFAULT 0,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_estado ON outbox (estado);

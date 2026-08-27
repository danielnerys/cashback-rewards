CREATE TABLE saldo_usuario (
    usuario_id     UUID PRIMARY KEY,
    saldo_centavos BIGINT NOT NULL DEFAULT 0,
    atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE movimentacao_extrato (
    id              UUID PRIMARY KEY,
    usuario_id      UUID NOT NULL,
    tipo            TEXT NOT NULL,
    valor_centavos  BIGINT NOT NULL,
    referencia_id   UUID NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movimentacao_extrato_usuario_id ON movimentacao_extrato (usuario_id, criado_em);

-- Principle II (Ledger Integrity, NON-NEGOTIABLE): append-only aplicado pelo
-- próprio banco via trigger, já que REVOKE não restringe o dono da tabela
-- (o mesmo papel usado pela aplicação/Flyway para criar a tabela).
CREATE OR REPLACE FUNCTION recusar_update_delete_movimentacao()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'movimentacao_extrato é append-only: % não é permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_movimentacao_extrato_append_only
    BEFORE UPDATE OR DELETE ON movimentacao_extrato
    FOR EACH ROW EXECUTE FUNCTION recusar_update_delete_movimentacao();

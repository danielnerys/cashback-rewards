CREATE TABLE regra_cashback (
    id                     UUID PRIMARY KEY,
    categoria              TEXT,
    percentual             NUMERIC(5,2) NOT NULL,
    valor_minimo_centavos  BIGINT NOT NULL,
    teto_centavos          BIGINT NOT NULL,
    ativa                  BOOLEAN NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX uq_regra_cashback_categoria_ativa
    ON regra_cashback (categoria)
    WHERE ativa = true;

CREATE UNIQUE INDEX uq_regra_cashback_geral_ativa
    ON regra_cashback ((categoria IS NULL))
    WHERE ativa = true AND categoria IS NULL;

CREATE TABLE credito_cashback (
    id                        UUID PRIMARY KEY,
    compra_id                 UUID NOT NULL,
    usuario_id                UUID NOT NULL,
    valor_calculado_centavos  BIGINT NOT NULL CHECK (valor_calculado_centavos > 0),
    regra_id                  UUID NOT NULL REFERENCES regra_cashback (id),
    estado                    TEXT NOT NULL,
    criado_em                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_credito_cashback_compra_id ON credito_cashback (compra_id);

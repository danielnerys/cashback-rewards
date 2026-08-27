CREATE TABLE compra (
    id                              UUID PRIMARY KEY,
    usuario_id                      UUID NOT NULL,
    valor_centavos                  BIGINT NOT NULL CHECK (valor_centavos > 0),
    categoria                       TEXT NOT NULL,
    data                            TIMESTAMPTZ NOT NULL,
    estado                          TEXT NOT NULL,
    percentual_devolvido_acumulado  NUMERIC(5,2) NOT NULL DEFAULT 0,
    criado_em                       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_compra_usuario_id ON compra (usuario_id);

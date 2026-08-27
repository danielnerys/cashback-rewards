# Data Model: Sistema de Cashback em Compras

Cada entidade pertence a exatamente um serviço (nenhum banco é compartilhado). Valores
monetários são `BIGINT` em centavos. Todo identificador é um UUID gerado no momento da
criação.

## compras-service

### Compra

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | gerado no registro |
| `usuario_id` | UUID | obrigatório (FR-001); identidade fornecida por sistema externo (Assumption) |
| `valor_centavos` | BIGINT | obrigatório, > 0 |
| `categoria` | TEXT | obrigatório |
| `data` | TIMESTAMPTZ | obrigatório |
| `estado` | TEXT ENUM | `ATIVA`, `CANCELADA`, `PARCIALMENTE_DEVOLVIDA`, `TOTALMENTE_DEVOLVIDA` |
| `percentual_devolvido_acumulado` | NUMERIC(5,2) | soma de todas as devoluções parciais; 0–100; MUST NOT exceder 100 (Assumption) |
| `criado_em` | TIMESTAMPTZ | auditoria |

**Transições de estado**: `ATIVA` → `CANCELADA` (cancelamento total, FR-006/FR-007);
`ATIVA` → `PARCIALMENTE_DEVOLVIDA` (primeira devolução parcial, FR-008/FR-009);
`PARCIALMENTE_DEVOLVIDA` → `PARCIALMENTE_DEVOLVIDA` (devolução adicional, ainda < 100%);
`PARCIALMENTE_DEVOLVIDA` → `TOTALMENTE_DEVOLVIDA` (soma atinge 100%, edge case da spec,
tratado como cancelamento total para fins de estorno); qualquer estado devolvido/cancelado
é terminal para novas devoluções.

### Cancelamento

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `compra_id` | UUID (FK Compra) | obrigatório |
| `origem` | TEXT ENUM | `USUARIO`, `ATENDIMENTO` (clarification: ambos podem registrar) |
| `data` | TIMESTAMPTZ | |

### Devolução

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `compra_id` | UUID (FK Compra) | obrigatório |
| `origem` | TEXT ENUM | `USUARIO`, `ATENDIMENTO` |
| `percentual_devolvido` | NUMERIC(5,2) | > 0 e ≤ 100 − percentual já devolvido da compra |
| `data` | TIMESTAMPTZ | |

### Outbox (compras → cashback)

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `tipo_evento` | TEXT ENUM | `COMPRA_REGISTRADA`, `CANCELAMENTO_REGISTRADO`, `DEVOLUCAO_REGISTRADA` |
| `payload` | JSONB | dados do evento (compra/cancelamento/devolução) |
| `idempotency_key` | TEXT | igual ao `id` do evento de origem (compra/cancelamento/devolução) |
| `estado` | TEXT ENUM | `PENDENTE`, `ENVIADO` |
| `tentativas` | INT | contador para backoff |
| `criado_em` / `atualizado_em` | TIMESTAMPTZ | |

## cashback-service

### Regra de Cashback

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `categoria` | TEXT (nullable) | `NULL` representa a regra geral (FR-003) |
| `percentual` | NUMERIC(5,2) | ex.: 5.00 para 5% |
| `valor_minimo_centavos` | BIGINT | piso de elegibilidade (FR-002), configurável (FR-017) |
| `teto_centavos` | BIGINT | teto máximo por compra (FR-004), configurável (FR-017) |
| `ativa` | BOOLEAN | permite desativar uma regra sem apagar histórico |

Restrição: no máximo uma regra ativa por `categoria`, e no máximo uma regra ativa com
`categoria IS NULL` (regra geral).

### Crédito de Cashback

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `compra_id` | UUID | referência à Compra (compras-service) |
| `usuario_id` | UUID | |
| `valor_calculado_centavos` | BIGINT | resultado da regra aplicada, já com teto (FR-003/FR-004) |
| `regra_id` | UUID (FK Regra de Cashback) | regra usada, para auditoria |
| `estado` | TEXT ENUM | `PENDENTE`, `CONCLUIDO` (clarification: estado do crédito) |
| `criado_em` | TIMESTAMPTZ | |

Se a compra não for elegível (valor abaixo do mínimo), nenhum registro de Crédito de
Cashback é criado (FR-002/FR-003).

### Estorno de Cashback

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `credito_id` | UUID (FK Crédito de Cashback) | |
| `origem` | TEXT ENUM | `CANCELAMENTO`, `DEVOLUCAO` |
| `origem_id` | UUID | id do cancelamento ou devolução que originou o estorno |
| `valor_estornado_centavos` | BIGINT | total (cancelamento) ou proporcional (devolução), nunca excede o saldo ainda não estornado do crédito (FR-010) |
| `estado` | TEXT ENUM | `PENDENTE`, `CONCLUIDO` |
| `criado_em` | TIMESTAMPTZ | |

Restrição: `SUM(valor_estornado_centavos)` para um mesmo `credito_id` MUST NOT exceder
`valor_calculado_centavos` do crédito correspondente (FR-010).

### Outbox (cashback → carteira)

Mesma forma da tabela de outbox do compras-service, com `tipo_evento` ∈
`{CREDITO_CALCULADO, ESTORNO_CALCULADO}` e `idempotency_key` = id do Crédito ou Estorno.

## carteira-service

### Saldo do Usuário

| Campo | Tipo | Regras |
|---|---|---|
| `usuario_id` | UUID (PK) | |
| `saldo_centavos` | BIGINT | pode ser negativo (FR-011); é uma projeção materializada, reconciliável por replay da Movimentação de Extrato (Principle II) |
| `atualizado_em` | TIMESTAMPTZ | |

### Movimentação de Extrato

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `usuario_id` | UUID | |
| `tipo` | TEXT ENUM | `CREDITO`, `ESTORNO`, `RESGATE` |
| `valor_centavos` | BIGINT | positivo para `CREDITO`, negativo para `ESTORNO` e `RESGATE` |
| `referencia_id` | UUID | id do Crédito de Cashback, Estorno de Cashback ou Resgate que originou a movimentação |
| `criado_em` | TIMESTAMPTZ | append-only; tabela sem `UPDATE`/`DELETE` (Principle II) |

O saldo é sempre igual a `SUM(valor_centavos)` de todas as movimentações do usuário — a
tabela `Saldo do Usuário` é apenas uma projeção para leitura rápida.

### Resgate

| Campo | Tipo | Regras |
|---|---|---|
| `id` | UUID (PK) | |
| `usuario_id` | UUID | |
| `valor_centavos` | BIGINT | > 0, ≤ saldo disponível no momento da solicitação (FR-014) |
| `criado_em` | TIMESTAMPTZ | |

Um Resgate só é aceito se `saldo_centavos > 0` e `valor_centavos ≤ saldo_centavos` no
momento da solicitação (FR-013/FR-014); não há bloqueio preventivo por compras pendentes de
cancelamento/devolução (FR-015).

## Tabela de idempotência (todos os serviços)

| Campo | Tipo | Regras |
|---|---|---|
| `idempotency_key` | TEXT (PK) | fornecida pelo chamador no header `Idempotency-Key` |
| `request_hash` | TEXT | hash do corpo da requisição, para detectar reuso indevido da chave |
| `response_body` | JSONB | resposta original, devolvida em caso de repetição |
| `response_status` | INT | status HTTP original |
| `criado_em` | TIMESTAMPTZ | |

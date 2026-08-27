# Quickstart: Sistema de Cashback em Compras

Guia para validar, de ponta a ponta, que as 4 histórias de usuário da spec funcionam.
Consulte [data-model.md](./data-model.md) para os campos das entidades e
[contracts/](./contracts) para os endpoints exatos — este guia não duplica os schemas.

## Pré-requisitos

- JDK 21
- Maven (usa o wrapper do reactor, se disponível)
- Docker (para Testcontainers/PostgreSQL local, ou `docker compose` com 3 instâncias/bancos
  PostgreSQL, um por serviço, conforme Structure Decision em `plan.md`)

## Build e testes

```bash
mvn -B clean verify
```

Isso deve compilar os 3 módulos (`compras-service`, `cashback-service`,
`carteira-service`), rodar as migrações Flyway de cada um contra o banco de teste e
executar as suítes unit/contract/integration.

## Subir os 3 serviços localmente

```bash
docker compose up -d postgres-compras postgres-cashback postgres-carteira
mvn -pl carteira-service spring-boot:run &
mvn -pl cashback-service spring-boot:run &
mvn -pl compras-service spring-boot:run &
```

## Cenário 1 — Ganhar cashback em uma compra elegível (User Story 1, P1)

1. Registrar uma compra acima do valor mínimo, em uma categoria com regra de 5%:

   ```bash
   curl -X POST http://localhost:8081/v1/compras \
     -H "Idempotency-Key: compra-001" \
     -H "Content-Type: application/json" \
     -d '{"usuarioId":"<uuid>","valorCentavos":10000,"categoria":"restaurantes","data":"2026-08-27T12:00:00Z"}'
   ```

   Esperado: `201`, resposta imediata com `estado: ATIVA` — mesmo que
   cashback-service esteja fora do ar no momento (FR-018).

2. Aguardar alguns segundos (SC-008) e consultar o saldo:

   ```bash
   curl http://localhost:8083/v1/usuarios/<uuid>/saldo
   ```

   Esperado: `saldoCentavos: 500` (5% de R$100,00).

3. Repetir a compra abaixo do valor mínimo configurado (ex.: R$10,00) e confirmar que o
   saldo não muda — nenhum crédito é gerado (Acceptance Scenario 3 da User Story 1).

## Cenário 2 — Resgatar o saldo acumulado (User Story 2, P2)

1. Com o saldo de R$5,00 do cenário anterior, solicitar resgate total:

   ```bash
   curl -X POST http://localhost:8083/v1/usuarios/<uuid>/resgates \
     -H "Idempotency-Key: resgate-001" \
     -H "Content-Type: application/json" \
     -d '{"valorCentavos":500}'
   ```

   Esperado: `201`, saldo passa a `0` em uma única interação (SC-005).

2. Consultar o extrato e confirmar que o crédito e o resgate aparecem em ordem
   cronológica (`GET /v1/usuarios/{id}/extrato`).

3. Tentar um novo resgate com saldo zero: esperado `409` (FR-014, SC-006).

## Cenário 3 — Estorno por cancelamento total (User Story 3, P3)

1. Registrar uma nova compra elegível e aguardar o crédito (como no Cenário 1).
2. Resgatar o saldo dessa compra (como no Cenário 2) — saldo volta a `0`.
3. Cancelar a compra:

   ```bash
   curl -X POST http://localhost:8081/v1/compras/<compraId>/cancelamentos \
     -H "Idempotency-Key: cancelamento-001" \
     -H "Content-Type: application/json" \
     -d '{"origem":"USUARIO"}'
   ```

4. Consultar o saldo: esperado um valor **negativo** igual ao cashback já resgatado
   (Acceptance Scenario 2 da User Story 3), e o extrato deve mostrar a dívida.

## Cenário 4 — Estorno proporcional por devolução parcial (User Story 4, P4)

1. Registrar uma compra elegível de R$100,00 (5% → R$5,00 de cashback) e aguardar o
   crédito.
2. Registrar uma devolução parcial de 30%:

   ```bash
   curl -X POST http://localhost:8081/v1/compras/<compraId>/devolucoes \
     -H "Idempotency-Key: devolucao-001" \
     -H "Content-Type: application/json" \
     -d '{"origem":"USUARIO","percentualDevolvido":30}'
   ```

3. Consultar o saldo: esperado débito de R$1,50 (30% de R$5,00).
4. Registrar uma segunda devolução de mais 80% (excedendo o saldo de 100% da compra):
   esperado `409` — a soma dos percentuais não pode ultrapassar 100% (edge case da spec).

## Validação de resiliência (FR-018/FR-019)

1. Derrubar `cashback-service` (`docker stop` ou parar o processo).
2. Registrar uma compra elegível em `compras-service`: esperado `201` imediato mesmo com
   `cashback-service` indisponível.
3. Subir `cashback-service` novamente e, dentro de alguns ciclos do job de outbox,
   confirmar que o crédito aparece no saldo sem nenhuma ação manual (SC-007).

## Validação de idempotência

Repetir exatamente a mesma requisição de registro de compra do Cenário 1 com o mesmo
header `Idempotency-Key: compra-001`: esperado a mesma resposta (mesmo `id`), sem gerar uma
segunda compra nem um segundo crédito.

# Implementation Plan: Sistema de Cashback em Compras

**Branch**: `001-cashback-purchase-flow` | **Date**: 2026-08-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-cashback-purchase-flow/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Três microserviços Spring Boot cooperam para creditar e estornar cashback de compras:
`compras-service` registra compras, cancelamentos e devoluções; `cashback-service` calcula
o cashback por regra de categoria (piso/teto) e calcula estornos totais ou proporcionais;
`carteira-service` mantém o saldo (podendo ficar negativo), o extrato e processa resgates.
Toda chamada entre serviços é REST síncrono direto (sem padrão 202/accept-and-acknowledge),
com idempotency key, timeout e retry, conforme a constitution. Para não violar o FR-018 da
spec (registro de compra/cancelamento/devolução não pode falhar nem bloquear por
indisponibilidade do próximo serviço), a propagação para o serviço seguinte usa um outbox
transacional lido por um job agendado: cada chamada real ao próximo serviço continua sendo
uma requisição/resposta síncrona, mas o *momento* de fazer essa chamada é decidido pelo
outbox, não pelo caminho de resposta ao cliente original.

## Technical Context

**Language/Version**: Java 21 (fixado pela constitution; `maven.compiler.release=21`)

**Primary Dependencies**: Spring Boot 3.3.x (web, validation, data-jpa), Spring Boot
`RestClient` (chamadas HTTP síncronas entre serviços), Spring Retry (backoff/jitter),
Flyway (migrações), springdoc-openapi (validação do contrato em runtime, opcional)

**Storage**: PostgreSQL 16, um banco por serviço (`compras`, `cashback`, `carteira`) — sem
banco compartilhado entre serviços

**Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL real em toda integração,
conforme constitution), MockMvc/WebTestClient para contrato dos endpoints HTTP

**Target Platform**: Linux server (containers), execução via `mvn -B clean verify` na raiz
do reactor

**Project Type**: Backend multi-serviço (3 microserviços Maven, sem frontend)

**Performance Goals**: Cashback creditado no saldo em poucos segundos após a compra sob
operação normal (SC-008); nenhum requisito de throughput numérico foi definido para o MVP

**Constraints**: Toda chamada entre serviços MUST ter timeout de conexão e leitura
explícitos; nenhuma chamada síncrona pode ficar bloqueada indefinidamente aguardando outro
serviço; registro de compra/cancelamento/devolução MUST NOT falhar por indisponibilidade do
serviço seguinte (FR-018)

**Scale/Scope**: 3 microserviços, 4 histórias de usuário (ganhar cashback, resgatar saldo,
estorno por cancelamento, estorno por devolução parcial); sem meta numérica de usuários
concorrentes definida para o MVP

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate (Constitution) | Status | Como o plano atende |
|---|---|---|
| I. Contract-First API | PASS | `contracts/*.yaml` (OpenAPI 3.0) para os 3 serviços, escritos antes de qualquer código, cobrindo endpoints públicos e endpoints internos entre serviços |
| II. Ledger Integrity (NON-NEGOTIABLE) | PASS | `carteira-service` usa tabela de movimentações append-only; valores monetários em `BIGINT` (centavos); saldo é derivável por replay das movimentações (ver data-model.md) |
| III. Idempotent State Changes | PASS | Todo endpoint que muda estado exige header `Idempotency-Key`; cada serviço persiste uma tabela de deduplicação; retries do outbox reusam a mesma chave |
| IV. Test-First (NON-NEGOTIABLE) | PASS | Tasks (`/speckit-tasks`) MUST seguir TDD; integração roda contra PostgreSQL real via Testcontainers, nunca H2/in-memory |
| V. Observability & Auditability | PASS | Filtro de correlação (`X-Correlation-Id`) propagado via `RestClient` interceptor; logs estruturados; cada crédito/estorno rastreável até a movimentação de saldo |
| Tech Stack (Java 21 / Spring Boot 3 / Maven / PostgreSQL / Flyway) | PASS | Reactor Maven com 3 módulos Spring Boot; Flyway por serviço; `maven.compiler.release=21` |
| REST síncrono direto (sem 202) | PASS | Toda chamada HTTP entre serviços é request/response direto; o desacoplamento exigido pelo FR-018 é feito por outbox + job agendado decidindo *quando* chamar, não pelo formato da resposta HTTP |
| Cadeia de 3+ serviços em um único caminho de requisição | N/A — não ocorre | Por causa do outbox, nenhuma requisição síncrona de cliente atravessa mais de 2 serviços (compras→cashback e, separadamente, cashback→carteira); não há caminho único de 3 saltos síncronos a justificar |
| Migrações Flyway obrigatórias | PASS | Cada módulo tem seu próprio `src/main/resources/db/migration` |

Nenhuma violação não justificada. A tabela de Complexity Tracking permanece vazia.

## Project Structure

### Documentation (this feature)

```text
specs/001-cashback-purchase-flow/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── compras-service.yaml
│   ├── cashback-service.yaml
│   └── carteira-service.yaml
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
pom.xml                              # Parent POM: dependencyManagement (Spring Boot BOM),
                                      # maven.compiler.release=21, common plugin config

compras-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/.../compras/
    │   │   ├── api/                 # Controllers REST (contrato compras-service.yaml)
    │   │   ├── domain/               # Compra, Cancelamento, Devolução
    │   │   ├── outbox/                # Outbox transacional + job agendado para chamar cashback-service
    │   │   └── client/                # RestClient para cashback-service (idempotency key, timeout, retry)
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/          # Flyway (V1__..., V2__...)
    └── test/
        ├── contract/                  # Testes de contrato dos endpoints públicos
        ├── integration/                # Testcontainers PostgreSQL
        └── unit/

cashback-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/.../cashback/
    │   │   ├── api/                   # Endpoint interno chamado por compras-service
    │   │   ├── domain/                 # RegraDeCashback, CreditoDeCashback, EstornoDeCashback
    │   │   ├── outbox/                  # Outbox + job agendado para chamar carteira-service
    │   │   └── client/                  # RestClient para carteira-service
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    └── test/
        ├── contract/
        ├── integration/
        └── unit/

carteira-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/.../carteira/
    │   │   ├── api/                    # Endpoints de saldo, extrato e resgate
    │   │   └── domain/                  # SaldoDoUsuario, MovimentacaoDeExtrato, Resgate
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    └── test/
        ├── contract/
        ├── integration/
        └── unit/
```

**Structure Decision**: Reactor Maven com 3 módulos independentes (`compras-service`,
`cashback-service`, `carteira-service`), cada um um Spring Boot app completo com seu
próprio banco PostgreSQL e suas próprias migrações Flyway — sem esquema compartilhado.
Não há módulo de frontend nesta feature (sistema headless, consumido via API). Cada
serviço que chama o próximo tem um pacote `outbox` (persistência transacional do evento a
propagar) e `client` (chamada REST síncrona real, com timeout/retry/idempotency key),
mantendo a chamada HTTP em si síncrona e direta conforme a constitution, enquanto atende ao
FR-018/FR-019 da spec (registro não bloqueia nem falha por indisponibilidade do próximo
serviço).

## Complexity Tracking

> Nenhuma violação da constitution foi identificada nesta fase; esta tabela não se aplica.

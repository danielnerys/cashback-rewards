---

description: "Task list template for feature implementation"
---

# Tasks: Sistema de Cashback em Compras

**Input**: Design documents from `/specs/001-cashback-purchase-flow/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: A constitution (Principle IV, Test-First, NON-NEGOTIABLE) exige testes escritos
antes da implementação, observados falhando, e integração contra PostgreSQL real
(Testcontainers). Portanto as tarefas de teste abaixo são obrigatórias, não opcionais.

**Organization**: Tarefas agrupadas por história de usuário (spec.md) para permitir
implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual história de usuário a tarefa pertence (US1, US2, US3, US4)
- Caminhos de arquivo exatos em cada descrição, conforme a Project Structure de `plan.md`

## Path Conventions

Reactor Maven com 3 módulos (ver `plan.md` → Project Structure):
`compras-service/`, `cashback-service/`, `carteira-service/`, cada um com
`src/main/java/com/cashbackrewards/<servico>/{api,domain,outbox,client}`,
`src/main/resources/{application.yml,db/migration}`, e
`src/test/{contract,integration,unit}`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do reactor Maven e dos 3 módulos

- [X] T001 Criar `pom.xml` reactor na raiz do repositório: `dependencyManagement` com o
      Spring Boot BOM, `maven.compiler.release=21`, módulos `compras-service`,
      `cashback-service`, `carteira-service`, e configuração de Surefire (testes unitários)
      + Failsafe (testes de integração) para `mvn -B clean verify`
- [X] T002 [P] Criar módulo `compras-service`: `compras-service/pom.xml` (starters web,
      data-jpa, validation, flyway-core, driver postgresql) e
      `compras-service/src/main/java/com/cashbackrewards/compras/ComprasServiceApplication.java`
- [X] T003 [P] Criar módulo `cashback-service`: `cashback-service/pom.xml` e
      `cashback-service/src/main/java/com/cashbackrewards/cashback/CashbackServiceApplication.java`
- [X] T004 [P] Criar módulo `carteira-service`: `carteira-service/pom.xml` e
      `carteira-service/src/main/java/com/cashbackrewards/carteira/CarteiraServiceApplication.java`
- [X] T005 [P] Criar `compras-service/src/main/resources/application.yml` (porta 8081,
      datasource PostgreSQL "compras", Flyway habilitado)
- [X] T006 [P] Criar `cashback-service/src/main/resources/application.yml` (porta 8082,
      datasource PostgreSQL "cashback", Flyway habilitado)
- [X] T007 [P] Criar `carteira-service/src/main/resources/application.yml` (porta 8083,
      datasource PostgreSQL "carteira", Flyway habilitado)
- [X] T008 [P] Criar `docker-compose.yml` na raiz com 3 instâncias/bancos PostgreSQL 16
      (compras, cashback, carteira) para desenvolvimento local, conforme `quickstart.md`

**Checkpoint**: `mvn -B clean verify` compila os 3 módulos vazios com sucesso.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura transversal exigida pela constitution e por FR-016/FR-018/FR-019,
usada por todas as histórias de usuário

**⚠️ CRITICAL**: Nenhuma história de usuário pode começar antes desta fase estar completa

### Idempotência (Principle III, FR-016)

- [X] T009 [P] Migração Flyway `compras-service/src/main/resources/db/migration/V1__idempotency_record.sql`
      (tabela `idempotency_record`: `idempotency_key` PK, `request_hash`, `response_body`
      jsonb, `response_status`, `criado_em`)
- [X] T010 [P] Migração Flyway equivalente em
      `cashback-service/src/main/resources/db/migration/V1__idempotency_record.sql`
- [X] T011 [P] Migração Flyway equivalente em
      `carteira-service/src/main/resources/db/migration/V1__idempotency_record.sql`
- [X] T012 [P] Implementar `IdempotencyInterceptor` em
      `compras-service/src/main/java/com/cashbackrewards/compras/api/IdempotencyInterceptor.java`
      exigindo o header `Idempotency-Key` em endpoints que mudam estado e devolvendo a
      resposta original em caso de repetição
- [X] T013 [P] Implementar `IdempotencyInterceptor` equivalente em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/api/IdempotencyInterceptor.java`
- [X] T014 [P] Implementar `IdempotencyInterceptor` equivalente em
      `carteira-service/src/main/java/com/cashbackrewards/carteira/api/IdempotencyInterceptor.java`

### Correlação e observabilidade (Principle V)

- [X] T015 [P] Implementar `CorrelationIdFilter` (lê/gera `X-Correlation-Id`, popula MDC) em
      `compras-service/src/main/java/com/cashbackrewards/compras/api/CorrelationIdFilter.java`
- [X] T016 [P] Implementar `CorrelationIdFilter` equivalente em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/api/CorrelationIdFilter.java`
- [X] T017 [P] Implementar `CorrelationIdFilter` equivalente em
      `carteira-service/src/main/java/com/cashbackrewards/carteira/api/CorrelationIdFilter.java`
- [X] T018 [P] Configurar logging estruturado (JSON, incluindo `correlationId` do MDC) em
      `compras-service/src/main/resources/logback-spring.xml`
- [X] T019 [P] Configurar logging estruturado equivalente em
      `cashback-service/src/main/resources/logback-spring.xml`
- [X] T020 [P] Configurar logging estruturado equivalente em
      `carteira-service/src/main/resources/logback-spring.xml`

### Cliente REST síncrono, retry e outbox (research.md #1–#3; FR-018, FR-019)

- [X] T021 [P] Configurar bean `RestClient` com timeout de conexão/leitura explícitos e
      interceptor que propaga `X-Correlation-Id`/`Idempotency-Key` em
      `compras-service/src/main/java/com/cashbackrewards/compras/client/RestClientConfig.java`
- [X] T022 [P] Configurar bean `RestClient` equivalente em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/client/RestClientConfig.java`
- [X] T023 [P] Configurar Spring Retry (`@EnableRetry`, backoff exponencial com jitter) em
      `compras-service/src/main/java/com/cashbackrewards/compras/client/RetryConfig.java`
- [X] T024 [P] Configurar Spring Retry equivalente em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/client/RetryConfig.java`
- [X] T025 [P] Migração Flyway `compras-service/src/main/resources/db/migration/V2__outbox.sql`
      (tabela outbox: `id`, `tipo_evento`, `payload` jsonb, `idempotency_key`, `estado`,
      `tentativas`, `criado_em`, `atualizado_em`)
- [X] T026 [P] Migração Flyway equivalente em
      `cashback-service/src/main/resources/db/migration/V2__outbox.sql`
- [X] T027 [P] Implementar entidade JPA + repositório `OutboxEntry`/`OutboxRepository` em
      `compras-service/src/main/java/com/cashbackrewards/compras/outbox/`
- [X] T028 [P] Implementar entidade JPA + repositório `OutboxEntry`/`OutboxRepository`
      equivalente em `cashback-service/src/main/java/com/cashbackrewards/cashback/outbox/`
- [X] T029 Implementar `OutboxDispatcher` (`@Scheduled`) em
      `compras-service/src/main/java/com/cashbackrewards/compras/outbox/OutboxDispatcher.java`
      que lê entradas `PENDENTE`, chama `cashback-service` via `RestClient` com
      `Idempotency-Key`/`X-Correlation-Id`, marca `ENVIADO` no sucesso e incrementa
      `tentativas` na falha (depends on T021, T023, T027)
- [X] T030 Implementar `OutboxDispatcher` equivalente em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/outbox/OutboxDispatcher.java`
      chamando `carteira-service` (depends on T022, T024, T028)

### Tratamento de erros

- [X] T031 [P] `GlobalExceptionHandler` (`@ControllerAdvice`) mapeando validação/não
      encontrado/conflito para status HTTP corretos em
      `compras-service/src/main/java/com/cashbackrewards/compras/api/GlobalExceptionHandler.java`
- [X] T032 [P] `GlobalExceptionHandler` equivalente em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/api/GlobalExceptionHandler.java`
- [X] T033 [P] `GlobalExceptionHandler` equivalente em
      `carteira-service/src/main/java/com/cashbackrewards/carteira/api/GlobalExceptionHandler.java`

**Checkpoint**: Fundação pronta — idempotência, correlação, outbox/retry e tratamento de
erro existem nos 3 serviços. Implementação das histórias de usuário pode começar.

---

## Phase 3: User Story 1 - Ganhar cashback em uma compra elegível (Priority: P1) 🎯 MVP

**Goal**: Uma compra elegível registrada em `compras-service` resulta, em poucos segundos,
em cashback creditado no saldo do usuário em `carteira-service`, aplicando a regra de
categoria/geral com piso e teto (FR-001 a FR-005).

**Independent Test**: `quickstart.md` → Cenário 1. Cada serviço também é testável
isoladamente: `compras-service` persiste a compra e enfileira o evento; `cashback-service`
calcula o valor correto dado piso/teto/regra; `carteira-service` aplica um crédito ao saldo.

### Tests for User Story 1 ⚠️ escrever e observar falhando antes de implementar

- [X] T034 [P] [US1] Teste de contrato para `POST /v1/compras` (conforme
      `contracts/compras-service.yaml`) em
      `compras-service/src/test/contract/RegistrarCompraContractTest.java`
- [X] T035 [P] [US1] Teste de integração (Testcontainers PostgreSQL): registrar compra
      elegível persiste `Compra` com estado `ATIVA` e cria `OutboxEntry` `PENDENTE` do tipo
      `COMPRA_REGISTRADA` em
      `compras-service/src/test/integration/RegistrarCompraIntegrationTest.java`
- [X] T036 [P] [US1] Teste de integração (Testcontainers + WireMock): `OutboxDispatcher` do
      `compras-service` chama `POST /v1/creditos` do `cashback-service` com
      `Idempotency-Key`/`X-Correlation-Id` e marca `ENVIADO` no sucesso em
      `compras-service/src/test/integration/OutboxDispatcherIntegrationTest.java`
- [X] T037 [P] [US1] Teste de contrato para `POST /v1/creditos` em
      `cashback-service/src/test/contract/CalcularCreditoContractTest.java`
- [X] T038 [P] [US1] Teste de integração (Testcontainers): cálculo de cashback cobrindo os
      4 Acceptance Scenarios da User Story 1 (5% restaurantes, 2% geral com teto de R$50,
      abaixo do piso não gera crédito, categoria sem regra usa a regra geral) em
      `cashback-service/src/test/integration/CalculoCreditoIntegrationTest.java`
- [X] T039 [P] [US1] Teste de integração (Testcontainers + WireMock): `OutboxDispatcher` do
      `cashback-service` chama `POST /v1/usuarios/{id}/creditos` do `carteira-service` em
      `cashback-service/src/test/integration/OutboxDispatcherIntegrationTest.java`
- [X] T040 [P] [US1] Teste de contrato para `POST /v1/usuarios/{id}/creditos` em
      `carteira-service/src/test/contract/AplicarCreditoContractTest.java`
- [X] T041 [P] [US1] Teste de integração (Testcontainers): aplicar crédito atualiza o saldo
      e cria `MovimentacaoDeExtrato` do tipo `CREDITO` em
      `carteira-service/src/test/integration/AplicarCreditoIntegrationTest.java`

### Implementation for User Story 1

- [X] T042 [P] [US1] Entidade JPA + repositório `Compra`/`CompraRepository` em
      `compras-service/src/main/java/com/cashbackrewards/compras/domain/`
- [X] T043 [US1] Migração Flyway `compras-service/src/main/resources/db/migration/V3__compra.sql`
      (tabela `compra`: valor, categoria, data, estado, percentual_devolvido_acumulado)
- [X] T044 [US1] Implementar `ComprasController` (`POST /v1/compras`) e `ComprasService`
      persistindo a `Compra` e enfileirando o `OutboxEntry` (`COMPRA_REGISTRADA`) na mesma
      transação em
      `compras-service/src/main/java/com/cashbackrewards/compras/api/ComprasController.java`
      e `compras-service/src/main/java/com/cashbackrewards/compras/domain/ComprasService.java`
      (depends on T042, T043)
- [X] T045 [P] [US1] Entidades JPA + repositórios `RegraDeCashback`, `CreditoDeCashback` em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/domain/`
- [X] T046 [US1] Migração Flyway
      `cashback-service/src/main/resources/db/migration/V3__regra_e_credito.sql`
- [X] T047 [US1] Implementar `CashbackController` (`POST /v1/creditos`) e
      `CashbackCalculoService` aplicando regra de categoria/geral, piso (FR-002) e teto
      (FR-004), persistindo `CreditoDeCashback` (estado `PENDENTE`) e enfileirando o outbox,
      em `cashback-service/src/main/java/com/cashbackrewards/cashback/api/CashbackController.java`
      e `cashback-service/src/main/java/com/cashbackrewards/cashback/domain/CashbackCalculoService.java`
      (depends on T045, T046)
- [X] T048 [P] [US1] Entidades JPA + repositórios `SaldoDoUsuario`, `MovimentacaoDeExtrato`
      em `carteira-service/src/main/java/com/cashbackrewards/carteira/domain/`
- [X] T049 [US1] Migração Flyway
      `carteira-service/src/main/resources/db/migration/V3__saldo_e_movimentacao.sql`
      (tabela `movimentacao_extrato` com `REVOKE UPDATE, DELETE`, conforme Principle II)
- [X] T050 [US1] Implementar `CarteiraController` (`POST /v1/usuarios/{id}/creditos`) e
      `SaldoService` atualizando o saldo e registrando a movimentação `CREDITO` em
      `carteira-service/src/main/java/com/cashbackrewards/carteira/api/CarteiraController.java`
      e `carteira-service/src/main/java/com/cashbackrewards/carteira/domain/SaldoService.java`
      (depends on T048, T049)

**Checkpoint**: User Story 1 completa e testável de forma independente (`quickstart.md`
Cenário 1).

---

## Phase 4: User Story 2 - Resgatar o saldo de cashback acumulado (Priority: P2)

**Goal**: Usuário consulta saldo/extrato e resgata o saldo disponível em uma única
interação, sem aprovação manual (FR-012 a FR-015).

**Independent Test**: `quickstart.md` → Cenário 2, a partir de um saldo positivo já
existente (da User Story 1).

### Tests for User Story 2 ⚠️ escrever e observar falhando antes de implementar

- [ ] T051 [P] [US2] Teste de contrato para `GET /v1/usuarios/{id}/saldo` em
      `carteira-service/src/test/contract/ConsultarSaldoContractTest.java`
- [ ] T052 [P] [US2] Teste de contrato para `GET /v1/usuarios/{id}/extrato` em
      `carteira-service/src/test/contract/ConsultarExtratoContractTest.java`
- [ ] T053 [P] [US2] Teste de contrato para `POST /v1/usuarios/{id}/resgates` em
      `carteira-service/src/test/contract/SolicitarResgateContractTest.java`
- [ ] T054 [P] [US2] Teste de integração (Testcontainers): resgate com saldo positivo debita
      o saldo e registra `MovimentacaoDeExtrato` `RESGATE` (Acceptance Scenario 1); resgate
      com saldo zero ou negativo é recusado com `409` (Acceptance Scenario 3, SC-006); e o
      extrato retorna créditos/estornos/resgates em ordem cronológica (Acceptance Scenario 2)
      em `carteira-service/src/test/integration/ResgateEExtratoIntegrationTest.java`

### Implementation for User Story 2

- [ ] T055 [P] [US2] Entidade JPA + repositório `Resgate`/`ResgateRepository` em
      `carteira-service/src/main/java/com/cashbackrewards/carteira/domain/`
- [ ] T056 [US2] Migração Flyway
      `carteira-service/src/main/resources/db/migration/V4__resgate.sql`
- [ ] T057 [US2] Implementar `GET /v1/usuarios/{id}/saldo` em `CarteiraController`
      (depends on T048)
- [ ] T058 [US2] Implementar `GET /v1/usuarios/{id}/extrato` (ordem cronológica, FR-012,
      SC-004) em `CarteiraController` (depends on T048)
- [ ] T059 [US2] Implementar `POST /v1/usuarios/{id}/resgates` em `CarteiraController` e
      `SaldoService`, validando saldo disponível (FR-013, FR-014) sem bloqueio preventivo
      por compras pendentes (FR-015) (depends on T055, T056)

**Checkpoint**: User Story 1 e 2 funcionam de forma independente.

---

## Phase 5: User Story 3 - Estorno de cashback por cancelamento total (Priority: P3)

**Goal**: Cancelar totalmente uma compra estorna o cashback já creditado, podendo deixar o
saldo negativo se já tiver sido resgatado (FR-006, FR-007, FR-010, FR-011).

**Independent Test**: `quickstart.md` → Cenário 3, a partir de uma compra creditada e
resgatada (Users Stories 1 e 2).

### Tests for User Story 3 ⚠️ escrever e observar falhando antes de implementar

- [ ] T060 [P] [US3] Teste de contrato para `POST /v1/compras/{id}/cancelamentos` em
      `compras-service/src/test/contract/RegistrarCancelamentoContractTest.java`
- [ ] T061 [P] [US3] Teste de integração (Testcontainers): registrar cancelamento atualiza o
      estado da `Compra` para `CANCELADA` e cria `OutboxEntry` `CANCELAMENTO_REGISTRADO`; uma
      compra sem cashback creditado não gera estorno (Acceptance Scenario 3) em
      `compras-service/src/test/integration/RegistrarCancelamentoIntegrationTest.java`
- [ ] T062 [P] [US3] Teste de contrato para `POST /v1/estornos` em
      `cashback-service/src/test/contract/CalcularEstornoContractTest.java`
- [ ] T063 [P] [US3] Teste de integração (Testcontainers): estorno por cancelamento total
      calcula o valor ainda não estornado do crédito e nunca excede o valor originalmente
      creditado (FR-010) em
      `cashback-service/src/test/integration/CalculoEstornoIntegrationTest.java`
- [ ] T064 [P] [US3] Teste de contrato para `POST /v1/usuarios/{id}/estornos` em
      `carteira-service/src/test/contract/AplicarEstornoContractTest.java`
- [ ] T065 [P] [US3] Teste de integração (Testcontainers): aplicar estorno após o resgate já
      ter ocorrido deixa o saldo negativo e a dívida aparece no extrato (Acceptance Scenario
      2, FR-011) em `carteira-service/src/test/integration/AplicarEstornoIntegrationTest.java`

### Implementation for User Story 3

- [ ] T066 [P] [US3] Entidade JPA + repositório `Cancelamento`/`CancelamentoRepository` em
      `compras-service/src/main/java/com/cashbackrewards/compras/domain/`
- [ ] T067 [US3] Migração Flyway
      `compras-service/src/main/resources/db/migration/V4__cancelamento.sql`
- [ ] T068 [US3] Implementar `POST /v1/compras/{id}/cancelamentos` em `ComprasController` e
      `ComprasService`, atualizando o estado da compra e enfileirando o outbox (depends on
      T066, T067)
- [ ] T069 [P] [US3] Entidade JPA + repositório `EstornoDeCashback`/
      `EstornoDeCashbackRepository` em
      `cashback-service/src/main/java/com/cashbackrewards/cashback/domain/`
- [ ] T070 [US3] Migração Flyway
      `cashback-service/src/main/resources/db/migration/V4__estorno.sql`
- [ ] T071 [US3] Implementar `POST /v1/estornos` em `CashbackController` e
      `CashbackEstornoService`, calculando o valor total ainda não estornado do crédito
      referenciado e enfileirando o outbox (depends on T069, T070)
- [ ] T072 [US3] Implementar `POST /v1/usuarios/{id}/estornos` em `CarteiraController` e
      `SaldoService`, debitando o saldo (podendo ficar negativo, FR-011) e registrando a
      movimentação `ESTORNO` (depends on T048, T049)

**Checkpoint**: User Stories 1, 2 e 3 funcionam de forma independente.

---

## Phase 6: User Story 4 - Estorno proporcional de cashback por devolução parcial (Priority: P4)

**Goal**: Uma devolução parcial estorna a parcela proporcional do cashback, e a soma de
todas as devoluções de uma compra nunca reverte mais do que o total creditado (FR-008,
FR-009, FR-010).

**Independent Test**: `quickstart.md` → Cenário 4.

### Tests for User Story 4 ⚠️ escrever e observar falhando antes de implementar

- [ ] T073 [P] [US4] Teste de contrato para `POST /v1/compras/{id}/devolucoes` em
      `compras-service/src/test/contract/RegistrarDevolucaoContractTest.java`
- [ ] T074 [P] [US4] Teste de integração (Testcontainers): devolução parcial atualiza
      `percentual_devolvido_acumulado`, transiciona o estado da compra
      (`ATIVA`→`PARCIALMENTE_DEVOLVIDA`→`TOTALMENTE_DEVOLVIDA` ao atingir 100%) e rejeita
      com `409` uma devolução que excederia 100% em
      `compras-service/src/test/integration/RegistrarDevolucaoIntegrationTest.java`
- [ ] T075 [P] [US4] Teste de integração (Testcontainers): estorno proporcional a múltiplas
      devoluções da mesma compra nunca excede, somado, o total originalmente creditado
      (Acceptance Scenario 2 da User Story 4, FR-010) em
      `cashback-service/src/test/integration/CalculoEstornoIntegrationTest.java`

### Implementation for User Story 4

- [ ] T076 [P] [US4] Entidade JPA + repositório `Devolucao`/`DevolucaoRepository` em
      `compras-service/src/main/java/com/cashbackrewards/compras/domain/`
- [ ] T077 [US4] Migração Flyway
      `compras-service/src/main/resources/db/migration/V5__devolucao.sql`
- [ ] T078 [US4] Implementar `POST /v1/compras/{id}/devolucoes` em `ComprasController` e
      `ComprasService`, validando que a soma dos percentuais não excede 100%, atualizando o
      estado da compra e enfileirando o outbox (depends on T076, T077)
- [ ] T079 [US4] Estender `CashbackEstornoService` para calcular o estorno proporcional ao
      percentual devolvido quando `origem = DEVOLUCAO`, reusando o endpoint `POST
      /v1/estornos` da User Story 3 (depends on T071)

**Checkpoint**: As 4 histórias de usuário funcionam de forma independente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validação end-to-end e robustez transversal

- [ ] T080 [P] Rodar `quickstart.md` de ponta a ponta com os 3 serviços via
      `docker-compose`, validando os 4 cenários, a validação de resiliência (FR-018/FR-019,
      SC-007) e a validação de idempotência
- [ ] T081 [P] Testes unitários dos limites de regra (valor exatamente no piso, valor que
      atinge exatamente o teto, categoria sem regra) em
      `cashback-service/src/test/unit/CashbackCalculoServiceTest.java`
- [ ] T082 [P] Expor OpenAPI em runtime (springdoc-openapi) validando o contrato de cada
      serviço, adicionando a dependência em `compras-service/pom.xml`,
      `cashback-service/pom.xml` e `carteira-service/pom.xml`
- [ ] T083 Verificar, durante uma execução do `quickstart.md`, que um único
      `X-Correlation-Id` aparece nos logs estruturados dos 3 serviços para uma mesma compra
      (Principle V)
- [ ] T084 [P] Adicionar `README.md` de execução local em `compras-service/README.md`,
      `cashback-service/README.md` e `carteira-service/README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — pode começar imediatamente
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as histórias de usuário
- **User Stories (Phase 3-6)**: todas dependem da Fundação (Phase 2)
  - US1 (P1) não depende de nenhuma outra história
  - US2 (P2) depende de US1 já ter criado o saldo a resgatar (para teste independente
    completo), mas seu código (`SaldoService`, `CarteiraController`) só depende da Fundação
  - US3 (P3) depende de US1 (precisa de um crédito para estornar); reusa entidades e
    controllers de US1
  - US4 (P4) depende de US3 (reusa `CashbackEstornoService` e o endpoint `POST /v1/estornos`
    criados em US3)
- **Polish (Phase 7)**: depende de todas as histórias desejadas estarem completas

### Within Each User Story

- Testes de contrato e integração são escritos e observados falhando antes da implementação
  (Principle IV, NON-NEGOTIABLE)
- Entidades/migrações antes de controllers/services
- US1: compras-service → cashback-service → carteira-service (ordem do fluxo de negócio)
- US3 reusa os controllers de compras-service e carteira-service criados em US1, adicionando
  os endpoints de cancelamento/estorno
- US4 reusa o endpoint de estorno de US3, estendendo o cálculo para o caso proporcional

### Parallel Opportunities

- Todas as tarefas `[P]` do Setup podem rodar em paralelo
- Dentro da Fundação, as tarefas de idempotência, correlação e outbox de serviços
  diferentes são `[P]` entre si (mesma camada, arquivos diferentes)
- Testes de contrato/integração de serviços diferentes dentro da mesma história são `[P]`
- Uma vez completa a Fundação, US1 pode começar; US2 pode ser desenvolvida em paralelo por
  outra pessoa assim que os endpoints de `carteira-service` de US1 (T048-T050) existirem

---

## Parallel Example: User Story 1

```bash
# Testes de contrato e integração de User Story 1 (arquivos/serviços diferentes):
Task: "Contract test POST /v1/compras em compras-service/src/test/contract/RegistrarCompraContractTest.java"
Task: "Contract test POST /v1/creditos em cashback-service/src/test/contract/CalcularCreditoContractTest.java"
Task: "Contract test POST /v1/usuarios/{id}/creditos em carteira-service/src/test/contract/AplicarCreditoContractTest.java"

# Entidades de User Story 1 (módulos diferentes):
Task: "Entidade Compra em compras-service/src/main/java/com/cashbackrewards/compras/domain/Compra.java"
Task: "Entidades RegraDeCashback/CreditoDeCashback em cashback-service/.../domain/"
Task: "Entidades SaldoDoUsuario/MovimentacaoDeExtrato em carteira-service/.../domain/"
```

---

## Implementation Strategy

### MVP First (User Story 1 apenas)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (CRÍTICO — bloqueia todas as histórias)
3. Completar Phase 3: User Story 1
4. **PARAR e VALIDAR**: rodar `quickstart.md` Cenário 1 de forma independente
5. Esse já é um MVP demonstrável: compra → cashback creditado automaticamente

### Incremental Delivery

1. Setup + Foundational → fundação pronta
2. US1 → validar independentemente → MVP
3. US2 → validar independentemente (saldo consultável e resgatável)
4. US3 → validar independentemente (estorno por cancelamento, incluindo saldo negativo)
5. US4 → validar independentemente (estorno proporcional por devolução parcial)

### Parallel Team Strategy

Com 3 desenvolvedores após a Fundação: cada um pode focar em um serviço
(`compras-service`, `cashback-service`, `carteira-service`) para uma mesma história de
usuário, coordenando pelos contratos OpenAPI já publicados em `contracts/`.

---

## Notes

- `[P]` = arquivos diferentes, sem dependências pendentes entre si
- `[Story]` mapeia a tarefa à história de usuário correspondente para rastreabilidade
- Testes MUST ser escritos e observados falhando antes da implementação (Principle IV)
- Nenhum teste de integração usa banco embutido/in-memory — sempre Testcontainers
  PostgreSQL (Development Workflow & Quality Gates da constitution)
- Cada história de usuário deve ser completável e testável de forma independente
- Fazer commit após cada tarefa ou grupo lógico de tarefas
- Parar em qualquer checkpoint para validar uma história isoladamente

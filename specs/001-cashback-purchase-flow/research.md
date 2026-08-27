# Research: Sistema de Cashback em Compras

Todos os itens do Technical Context já estavam determinados pela constitution
(`.specify/memory/constitution.md`, v1.2.1) ou pelas clarifications da spec, exceto a
forma concreta de conciliar "REST síncrono direto" com "registro nunca bloqueia nem falha
por indisponibilidade do próximo serviço" (FR-018/FR-019). Este documento resolve esse
ponto e registra as decisões de biblioteca/padrão necessárias para o desenho.

## 1. Como reconciliar REST síncrono com registro não bloqueante

**Decision**: Cada serviço que precisa notificar o próximo (compras→cashback,
cashback→carteira) persiste, na mesma transação em que grava seu próprio registro
(compra/cancelamento/devolução, ou crédito/estorno), uma linha em uma tabela de outbox
local. Um job agendado (`@Scheduled`) lê o outbox e faz a chamada HTTP real ao próximo
serviço — essa chamada é uma requisição/resposta síncrona comum (sem 202, sem callback),
com timeout, idempotency key e retry com backoff/jitter. Se a chamada falhar, a linha do
outbox permanece pendente e é tentada novamente no próximo ciclo do job.

**Rationale**: A constitution proíbe apenas o *padrão de resposta* accept-and-acknowledge
(retornar 202 e o cliente fazer polling/callback) nas chamadas entre serviços — ela não
exige que o momento de disparar uma chamada síncrona seja o mesmo em que a requisição do
cliente original é respondida. Separar "quando chamar" (decidido pelo outbox) de "como
chamar" (sempre síncrono, direto) satisfaz as duas exigências ao mesmo tempo: nenhuma
chamada HTTP usa o padrão proibido, e nenhum registro do domínio fica esperando uma
chamada de rede para responder ao seu próprio chamador.

**Alternatives considered**:
- *Chamada síncrona bloqueante direto no fluxo da requisição* (compras-service chama
  cashback-service, que chama carteira-service, tudo antes de responder ao cliente):
  rejeitada porque viola FR-018 — uma indisponibilidade de carteira-service faria a compra
  inteira falhar, mesmo a compra em si sendo uma operação local válida.
- *HTTP 202 accept-and-acknowledge com polling*: rejeitada explicitamente pela constitution
  (Technology Stack & Service Communication, v1.2.0).
- *Message broker / fila*: fora de escopo do MVP pela constitution; reintroduziria
  infraestrutura que o projeto decidiu adiar deliberadamente.

## 2. Cliente HTTP síncrono entre serviços

**Decision**: Spring `RestClient` (Spring Framework 6.1 / Boot 3.2+), configurado com
`ClientHttpRequestFactory` de timeout de conexão e leitura explícitos, e um
`ClientHttpRequestInterceptor` que propaga `X-Correlation-Id` e injeta `Idempotency-Key`.

**Rationale**: É a API síncrona recomendada pelo Spring Boot 3.x atual (sucessora do
`RestTemplate`, que está em manutenção), com suporte nativo a interceptors e timeouts, sem
introduzir programação reativa (que não é necessária aqui — todas as chamadas são
request/response direto).

**Alternatives considered**: `RestTemplate` (funcional, mas em modo de manutenção pelo
Spring); `WebClient` reativo (adequado, mas adiciona um modelo de programação reativo sem
necessidade, já que a constitution já decidiu por chamadas síncronas simples).

## 3. Retry das chamadas entre serviços

**Decision**: Spring Retry (`@Retryable`/`RetryTemplate`) com backoff exponencial e
jitter, número máximo de tentativas configurável, envolvendo a chamada do `RestClient` no
job do outbox.

**Rationale**: Integra-se nativamente ao Spring Boot, evita reimplementar backoff manual, e
é suficiente para o volume de chamadas do MVP (sem necessidade de um circuit breaker
dedicado nesta fase).

**Alternatives considered**: Resilience4j (mais completo — circuit breaker, bulkhead,
rate limiter — mas excede o que o MVP precisa; pode ser adotado depois sem mudar o
contrato).

## 4. Idempotência

**Decision**: Todo endpoint que muda estado exige o header `Idempotency-Key` (fornecido
pelo chamador). Cada serviço persiste uma tabela `idempotency_record` (chave, hash da
requisição, resposta serializada, timestamp) e devolve a resposta original em caso de
repetição da mesma chave.

**Rationale**: Exigido pela Principle III (NON-NEGOTIABLE) e por FR-016 da spec
(não aplicar o mesmo estorno duas vezes mesmo com reenvio). Guardar a resposta original
evita que uma reentrega produza um efeito colateral diferente do original.

## 5. Correlação e observabilidade

**Decision**: Um `OncePerRequestFilter` em cada serviço lê ou gera `X-Correlation-Id`,
coloca em MDC para os logs estruturados, e o `RestClient` interceptor o propaga em toda
chamada de saída (incluindo as disparadas pelo job do outbox).

**Rationale**: Exigido pela Principle V; permite rastrear uma compra da API pública até a
movimentação de saldo final, mesmo atravessando o outbox e o retry assíncrono do job.

## 6. Modelagem de dinheiro

**Decision**: Todo valor monetário é armazenado como `BIGINT` em centavos (long em Java),
nunca `float`/`double`.

**Rationale**: A constitution permite `BIGINT` em unidades mínimas ou `NUMERIC` com
precisão explícita; `BIGINT`/`long` foi escolhido por ser aritmética inteira exata sem
depender de configuração de escala, e por já ser suficiente (a moeda do domínio não usa
frações menores que centavos).

## 7. Persistência e migrações

**Decision**: Um banco PostgreSQL por serviço (sem esquema compartilhado); migrações
Flyway versionadas dentro de cada módulo (`src/main/resources/db/migration`).

**Rationale**: Exigido pela constitution (Flyway obrigatório em todo microserviço) e pela
Principle II (ledger append-only, sem `UPDATE`/`DELETE` na tabela de movimentações —
aplicado via `REVOKE UPDATE, DELETE` na migração inicial da tabela).

## 8. Testes de integração

**Decision**: Testcontainers com PostgreSQL real para todo teste de integração;
`mvn -B clean verify` roda as migrações Flyway contra o banco de teste antes dos testes.

**Rationale**: Exigido pela constitution (Development Workflow & Quality Gates) — um banco
embutido/in-memory não exercitaria as mesmas constraints e tipos do PostgreSQL de produção.

## 9. Formato do contrato

**Decision**: OpenAPI 3.0, um arquivo YAML por serviço em `contracts/`, escrito antes da
implementação (Contract-First, Principle I), cobrindo tanto os endpoints públicos quanto os
endpoints internos chamados por outro serviço.

**Rationale**: OpenAPI é o formato citado como exemplo pela própria constitution e é
suportado nativamente pelo ecossistema Spring (springdoc-openapi pode validar/gerar a
documentação viva a partir do contrato depois).

## Estado final

Nenhum item do Technical Context permanece como NEEDS CLARIFICATION.

<!--
SYNC IMPACT REPORT
==================
Version change: 1.1.0 → 1.2.0 → 1.2.1
Rationale (1.1.0 → 1.2.0, MINOR): The inter-service communication rule in Technology
Stack & Service Communication was reversed: the MVP now mandates synchronous REST
(direct request/response) and forbids the accept-and-acknowledge pattern that v1.1.0
required. Idempotency keys, timeouts, and bounded retries are retained as required
practice. No Core Principle was removed or redefined, and the versioning policy in
Governance scopes MAJOR to principle-level redefinition, so this lands as MINOR despite
reversing a rule. Nothing had yet been implemented under the v1.1.0 rule.
Rationale (1.2.0 → 1.2.1, PATCH): The migration tool, already required generically
("a versioned migration tool checked into the repository"), is now named concretely as
Flyway, applied uniformly across every microservice. This clarifies an existing
obligation rather than adding a new one, so it lands as PATCH.

Modified principles: none (I–V unchanged, wording identical to v1.0.0)

Modified sections:
  Technology Stack & Service Communication — asynchronous REST replaced by synchronous
  REST; HTTP 202 accept-and-acknowledge now explicitly forbidden in the MVP; the ban on
  three-or-more-service chains relaxed to a justification requirement; migration tool
  named as Flyway, mandatory in every microservice.
  Development Workflow & Quality Gates — generic "versioned migration tool" replaced by
  Flyway specifically, with CI now required to run Flyway migrations before tests.

Added sections: none

Removed sections: none

Resolved deferred items:
  TODO(TECH_STACK) — closed in v1.1.0. Stack: Java 21, Spring Boot 3, Maven, PostgreSQL.
  Inter-service communication for the MVP: synchronous REST, no message broker.
  Migration tool: Flyway, mandatory in every microservice.

Follow-up TODOs: none
-->

# Cashback Rewards Constitution

Cashback Rewards is a headless rewards backend. It owns earn/accrual, the balance ledger,
and redemption, and it is consumed exclusively through its APIs by clients it does not
control. Every principle below follows from that position: the service is the system of
record for money-like balances, and callers cannot be trusted to compensate for its
mistakes.

## Core Principles

### I. Contract-First API

Every endpoint MUST have a machine-readable contract (OpenAPI or equivalent) committed
before its implementation. Contracts are versioned; a breaking change to a published
contract MUST ship under a new version path while the prior version remains supported for
a documented deprecation window. Consumers MUST NOT be required to read server source to
integrate. Contract tests MUST fail when implementation and contract diverge.

Rationale: Clients are outside this repository and cannot be fixed in lockstep, so the
contract is the only enforceable boundary.

### II. Ledger Integrity (NON-NEGOTIABLE)

The balance ledger MUST be append-only. Entries are never updated or deleted; corrections
MUST be issued as new compensating entries that reference the entry they reverse. Every
balance MUST be derivable by replaying ledger entries, and any cached or materialized
balance MUST be reconcilable against that replay. Every entry MUST record actor, reason,
source event, and timestamp. Monetary amounts MUST be stored as integer minor units or an
exact decimal type; binary floating point is forbidden for money.

Rationale: A rewards balance is a liability. Destructive mutation makes disputes
unanswerable and reconciliation impossible.

### III. Idempotent State Changes

Every state-changing endpoint MUST accept a caller-supplied idempotency key and MUST
return the original result for a repeated key rather than applying the effect twice. Earn
and redemption operations MUST be safe to retry under network failure, timeout, or
duplicate upstream event delivery. Idempotency records MUST be persisted with the same
durability guarantees as the ledger entries they protect.

Rationale: Clients retry. Without exactly-once effects, a retry mints or burns balance
that was never earned or spent.

### IV. Test-First (NON-NEGOTIABLE)

Tests MUST be written before the implementation they cover, MUST be observed failing, and
only then made to pass. Contract changes, ledger operations, accrual rules, and redemption
paths MUST have integration tests exercising real persistence, not mocks of the data layer.
A pull request that adds or changes behavior without an accompanying failing-then-passing
test MUST NOT merge.

Rationale: Accrual and redemption bugs are discovered by users as missing money, and are
expensive to unwind after the fact.

### V. Observability & Auditability

All logs MUST be structured and carry a correlation identifier that spans the full request
and any events it emits. Every balance change MUST be traceable from an API call to its
ledger entries and back. Logs, traces, and error payloads MUST NOT contain primary account
numbers, full card data, credentials, or unmasked personal identifiers. Health and
reconciliation status MUST be exposed as queryable endpoints or metrics.

Rationale: Support and finance need to answer "why is this balance what it is" without
attaching a debugger to production.

## Technology Stack & Service Communication

The stack is fixed for all services in this system and MUST NOT diverge per service without
an amendment to this constitution:

- **Language/runtime**: Java 21. The language level MUST be 21 and MUST be pinned in the
  Maven build via `maven.compiler.release`, so the build fails rather than silently
  targeting another JDK.
- **Framework**: Spring Boot 3.x. The Spring Boot parent or BOM MUST be the single source of
  dependency versions; ad-hoc version overrides for managed dependencies MUST be justified
  in the pull request.
- **Build**: Maven. One reactor build; every module MUST build and test from the repository
  root with a single command. Build logic MUST live in the POM, not in developer-local
  scripts.
- **Database**: PostgreSQL. It is the system of record. Schema changes MUST ship as Flyway
  migrations (see Development Workflow & Quality Gates).
- **Database migrations**: Flyway, in every microservice without exception. A service MUST
  NOT substitute another migration tool.

Persistence rules that follow from Principle II: monetary columns MUST be `BIGINT` holding
integer minor units, or `NUMERIC` with explicit precision and scale. `REAL`, `DOUBLE
PRECISION`, and Java `float`/`double` MUST NOT be used for money. Ledger tables MUST reject
`UPDATE` and `DELETE` at the database level where practicable, so append-only is enforced by
the engine and not only by application code.

Inter-service communication for the MVP MUST be synchronous REST over HTTP: a direct
request/response call in which the caller issues the request and consumes the result
inline. The pattern is deliberately plain, and the following rules constrain it:

- Calls are direct request/response. Accept-and-acknowledge indirection — returning HTTP
  `202` and completing the work out of band via polling or callbacks — MUST NOT be
  introduced in the MVP.
- Every inter-service call MUST set an explicit connect timeout and read timeout. An
  unbounded wait on another service is forbidden.
- A failed call MAY be retried with exponential backoff and jitter, under a bounded attempt
  count. Retries MUST carry the same idempotency key as the original attempt, per
  Principle III — retrying without one is a defect, not a fallback.
- A state-changing inter-service call MUST carry an idempotency key, so that a retry after a
  timeout or a partial failure cannot apply the effect twice.
- The correlation identifier required by Principle V MUST be propagated as an HTTP header on
  every inter-service call and adopted by the receiving service.
- Synchronous call chains couple availability and add latency at every hop. A flow that
  requires three or more services in a single request path MUST be justified in the pull
  request description.
- A message broker and asynchronous messaging are deliberately out of scope for the MVP.
  Introducing either is an amendment to this section, not an implementation detail.

Rationale: Direct request/response is the simplest thing that works, and it keeps the MVP
debuggable — one call, one response, one stack trace. The safeguards above are cheap and
prevent duplicate balance changes, while the structural complexity of asynchronous delivery
is deferred until something actually demands it.

## Security & Data Protection

Secrets MUST come from a managed secret store or injected environment; they MUST NOT be
committed to the repository in any form. Data MUST be encrypted in transit (TLS) and at
rest. Access to production data MUST follow least privilege and be auditable. Personal data
MUST be collected only where a documented purpose requires it, and retention limits MUST be
defined for each category stored. Any dependency with a known critical vulnerability MUST
be patched or mitigated before the next production deploy.

## Development Workflow & Quality Gates

Work follows the Spec Kit flow: specify, plan, tasks, implement. Every change MUST arrive
as a reviewed pull request; self-merge without review is not permitted for changes touching
ledger, accrual, redemption, or authentication. Complexity that violates a principle MUST be
justified in the pull request description or refactored away before merge.

CI MUST run, at minimum, `mvn -B clean verify` from the repository root, which MUST compile
against Java 21, execute unit tests, execute integration tests, and fail the build on any
static-analysis or test failure. A failing gate MUST block merge — gates are never advisory.
Warnings promoted to errors MUST NOT be downgraded to get a build green.

Integration tests required by Principle IV MUST run against a real PostgreSQL instance
provisioned by the test run itself (for example via Testcontainers). An in-memory or
embedded database substituted for PostgreSQL does not satisfy Principle IV, because it does
not exercise the constraints, types, and transactional behavior the ledger depends on.

Database migrations MUST be Flyway migrations, versioned and checked into the repository
under each service's own migration path. Migrations MUST be forward-only: a migration that
has been applied to any shared environment MUST NOT be edited in place, and its effect is
reversed by a new follow-up migration, never by editing or deleting the applied file.
`mvn -B clean verify` MUST run Flyway migrations against the test database before tests
execute, so schema drift between a service's migrations and its code fails CI rather than
surfacing in a shared environment.

## Governance

This constitution supersedes conflicting practices, conventions, and prior agreements. When
a principle and a convenience conflict, the principle wins.

Amendments MUST be proposed as a pull request that modifies this file, states the rationale,
and identifies the version bump. Amendments touching a NON-NEGOTIABLE principle require
explicit approval from the project owner.

Versioning follows semantic versioning. MAJOR covers removing or redefining a principle in a
backward-incompatible way; MINOR covers adding a principle or section, or materially
expanding guidance; PATCH covers clarifications and wording that do not change meaning.

Compliance is reviewed at every pull request: reviewers MUST confirm the change is
consistent with these principles, and MUST block on unjustified violations. Any principle
that is repeatedly waived MUST be amended or removed rather than quietly ignored.

**Version**: 1.2.1 | **Ratified**: 2026-08-27 | **Last Amended**: 2026-08-27

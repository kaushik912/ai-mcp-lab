---
status: approved
---

# Plan: Spring AI Multi-Service MCP System

## Stack detection

Greenfield repo, no existing build files. Ticket (`stack_notes`) and repo convention
(`my-claude-lib/.claude/rules/spring.md`) mandate Spring Boot + the `spring` CLI.
Confirmed locally:

- `spring` CLI v4.1.0 available (`command -v spring`).
- Java 17.0.19, Maven 3.9.12 available.
- Sibling local projects `mcp-quote-client` / `mcp-quote-server` (same machine, same
  MCP-over-SSE shape) pin `spring-boot-starter-parent` **4.1.0** and
  `spring-ai-bom` **2.0.0** — used as the version baseline here instead of guessing.
  Under Spring AI 2.0.0 the MCP starters are BOM-managed (no explicit `<version>`)
  and use the current artifact names, not the blog post's 1.0.0-M6-era names:
  - Server (WebFlux/SSE): `org.springframework.ai:spring-ai-starter-mcp-server-webflux`
    (blog's `spring-ai-mcp-server-webflux-spring-boot-starter` is the old M6 name;
    confirmed current name exists in Maven Central under the new starter naming
    scheme — [mvnrepository](https://mvnrepository.com/artifact/org.springframework.ai/spring-ai-starter-mcp-server-webflux)).
  - Client (SSE): `org.springframework.ai:spring-ai-starter-mcp-client-webflux`
    (cached locally, used as-is in `mcp-quote-client`).
  - Chat model: `org.springframework.ai:spring-ai-starter-model-openai` (cached locally),
    pointed at **OpenRouter** rather than OpenAI itself — OpenRouter exposes an
    OpenAI-compatible chat completions API, so this is a config swap
    (`base-url`, `api-key`, `model`), not a different starter. Same pattern
    already used in `mcp-quote-client` for Gemini's OpenAI-compatible
    endpoint.

## Architecture

Three independent Maven projects (no aggregator/parent-of-parents — matches both the
spec's "independent applications" requirement and the local reference projects),
scaffolded via `spring init`:

```
person-mcp-service/     port 8060, MCP server (WebFlux/SSE), H2 + JPA
account-mcp-service/    port 8040, MCP server (WebFlux/SSE), H2 + JPA
sample-client/          port 8080, MCP client + REST API + OpenAI chat model
```

### person-mcp-service
- `Person` JPA entity + `Gender` enum (`MALE`, `FEMALE`).
- `PersonRepository extends JpaRepository<Person, Long>` + `findByNationality(String)`.
- `PersonToolService` with `@Tool` methods `getPersonById` / `getPersonsByNationality`.
- `@Bean ToolCallbackProvider` via `MethodToolCallbackProvider.builder()`.
- `application.yml`: `server.port=8060`, `spring.ai.mcp.server.name=person-mcp-server`,
  `spring.ai.mcp.server.version=1.0.0`, H2 `create-drop`, `import.sql` seed data.

### account-mcp-service
- `Account` JPA entity (`id`, `number`, `balance`, `personId`).
- `AccountRepository extends JpaRepository<Account, Long>` + `findByPersonId(Long)`.
- `AccountToolService` with `@Tool` method `getAccountsByPersonId`.
- `@Bean ToolCallbackProvider`.
- `application.yml`: `server.port=8040`, `spring.ai.mcp.server.name=account-mcp-server`,
  `spring.ai.mcp.server.version=1.0.0`, H2 `create-drop`, `import.sql` seed data
  (account `personId` values must line up with `person-mcp-service`'s seeded person
  IDs so the cross-service acceptance test resolves).

### sample-client
- `application.yml`: `spring.ai.mcp.client.sse.connections.person-mcp-server.url=http://localhost:8060`,
  `...account-mcp-server.url=http://localhost:8040`;
  `spring.ai.openai.base-url=https://openrouter.ai/api/v1`,
  `spring.ai.openai.api-key=${OPENROUTER_API_KEY}`,
  `spring.ai.openai.chat.options.model=${OPENROUTER_MODEL:openai/gpt-4o-mini}`
  — key from `OPENROUTER_API_KEY` env var, never hardcoded (repo security rule).
- `ChatClientConfig`: `@Bean ChatClient` built from `ChatClient.Builder.defaultTools(tools)`
  where `tools` is the auto-configured `ToolCallbackProvider` (aggregates both
  connected MCP servers' tools).
- `PersonController` (`/persons`), `AccountController` (`/accounts`) — each method
  builds a natural-language prompt per the spec's four REST endpoints and calls
  `chatClient.prompt(...).call().content()`.
- springdoc-openapi: `springdoc-openapi-starter-webmvc-ui` dependency, `@Tag` on both
  controllers, `@Operation` on each endpoint method. No custom `springdoc:` config —
  default paths `/swagger-ui.html` and `/v3/api-docs`.

## Key decisions / tradeoffs

- **No aggregator POM**: keeps the three services genuinely independent (matches
  spec + reference projects), at the cost of duplicating `<properties>`/BOM import
  across three POMs. Acceptable for a 3-service demo; not worth a parent POM's
  coupling for this scope.
- **`import.sql` over a JPA `CommandLineRunner`**: simplest, matches spec wording
  exactly ("provide `import.sql` files"), and Hibernate's `create-drop` +
  `import.sql` combo is a well-known Spring Boot/H2 pattern needing no extra code.
- **Prompts as string templates in the controller methods**, not injected config:
  the spec pins the exact prompt text per endpoint; hardcoding them inline keeps the
  intent traceable 1:1 to the spec instead of hiding it behind indirection.
- **Real OpenRouter calls in the end-to-end test**, not a mocked `ChatClient`: the
  spec's own verification section requires proving actual multi-tool orchestration
  through the LLM — mocking `ChatClient` would test the plumbing but not the thing
  the spec cares about. This test is opt-in (`OPENROUTER_API_KEY`-gated, see Testing
  seam) rather than part of the default fast suite.
- **`openai/gpt-4o-mini` as the default OpenRouter model**: chosen over a free-tier
  model for reliable native tool-calling — the multi-tool balance endpoint is the
  spec's hardest acceptance criterion and free OpenRouter models have inconsistent
  tool-calling support. Overridable via `OPENROUTER_MODEL`.

## Risks

- **Artifact-name drift**: current Spring AI (2.0.0-line) uses `spring-ai-starter-*`
  naming; the blog's `*-spring-boot-starter` names are the older 1.0.0-M6 line and
  won't resolve as-is. Mitigated by pinning to the same BOM/parent versions already
  proven to resolve in `mcp-quote-client`/`mcp-quote-server` on this machine.
- **Non-determinism of LLM tool-call responses**: assertions on `sample-client`
  endpoints must check for presence of expected values (person name, nationality,
  numeric balance) in the response text/structure, not exact wording.
- **Cross-service seed data coupling**: the multi-tool balance endpoint only proves
  correct if `account-mcp-service`'s seeded `personId` values match
  `person-mcp-service`'s seeded person IDs — call this out explicitly in both
  `import.sql` files and in the task that seeds them.
- **Port collisions**: 8060/8040/8080 must be free locally when running the
  three-service manual/integration test sequence.
- **No CI / no OpenRouter key in automated runs** (non-goal): the real end-to-end
  LLM test is manual/local-only, gated on `OPENROUTER_API_KEY` being set.

## Testing seam

**Layered** (per ticket `testing_seam`), Spring-idiomatic:

- `person-mcp-service` / `account-mcp-service`:
  - `@DataJpaTest` for `PersonRepository`/`AccountRepository` custom finder methods.
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` hitting the
    tool layer directly (call the `@Tool`-annotated service methods / the exposed
    MCP SSE endpoint) to prove the `ToolCallbackProvider` wiring works, with H2
    seeded via `import.sql` for realistic data.
- `sample-client`:
  - Unit tests for controller request-building logic where feasible without a real
    `ChatClient`.
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` integration
    tests for all four REST endpoints, gated behind `OPENROUTER_API_KEY` being
    present in the environment (`@EnabledIfEnvironmentVariable`) since they require a
    live OpenRouter call and the two live MCP servers — these are the tests that
    fulfill the spec's end-to-end acceptance criterion.

## API docs

**springdoc-openapi**, `sample-client` only (it's the only service with a REST API;
`person-mcp-service`/`account-mcp-service` only expose MCP tool/SSE endpoints, no
REST controllers to document):

- Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6`.
- Swagger UI: `/swagger-ui.html`. OpenAPI spec: `/v3/api-docs`. No custom
  `springdoc:` config needed.
- `@Tag` on `PersonController`/`AccountController`, `@Operation` on each of the
  four endpoint methods.

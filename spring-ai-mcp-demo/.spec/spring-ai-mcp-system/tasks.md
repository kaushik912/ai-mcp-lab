---
status: approved
---

# Tasks: Spring AI Multi-Service MCP System

## Scaffolding (non-test-first)

- [x] Scaffold `person-mcp-service` via `spring init` (deps: MCP server WebFlux/SSE
      starter, Spring Data JPA, H2 runtime; Spring Boot parent 4.1.0, spring-ai-bom
      2.0.0 per `plan.md`).
- [x] Scaffold `account-mcp-service` via `spring init` (same starter set as above).
- [x] Scaffold `sample-client` via `spring init` (deps: `spring-boot-starter-web`,
      MCP client WebFlux/SSE starter, `spring-ai-starter-model-openai`).
- [x] Configure `person-mcp-service/application.yml`: `server.port=8060`,
      `spring.ai.mcp.server.name=person-mcp-server`,
      `spring.ai.mcp.server.version=1.0.0`, `spring.jpa.hibernate.ddl-auto=create-drop`.
- [x] Configure `account-mcp-service/application.yml`: `server.port=8040`,
      `spring.ai.mcp.server.name=account-mcp-server`,
      `spring.ai.mcp.server.version=1.0.0`, `spring.jpa.hibernate.ddl-auto=create-drop`.
- [x] Configure `sample-client/application.yml`:
      `spring.ai.mcp.client.sse.connections.person-mcp-server.url=http://localhost:8060`,
      `...account-mcp-server.url=http://localhost:8040`,
      `spring.ai.openai.base-url=https://openrouter.ai/api/v1`,
      `spring.ai.openai.api-key=${OPENROUTER_API_KEY}`,
      `spring.ai.openai.chat.options.model=${OPENROUTER_MODEL:openai/gpt-4o-mini}`.

## person-mcp-service: domain + repository

- [x] Write failing `@DataJpaTest` for `PersonRepository.findByNationality` asserting
      it returns the seeded persons for a known nationality.
- [x] Implement `Person` entity, `Gender` enum (`MALE`, `FEMALE`), `PersonRepository`
      (`findByNationality`), and `person-mcp-service/src/main/resources/import.sql`
      seed data (include at least one person with id `1`, used by the cross-service
      balance test later) to make the test pass.

## person-mcp-service: tool layer

- [x] Write failing unit tests for `PersonToolService.getPersonById` (found /
      not-found -> null) and `getPersonsByNationality` (delegates to
      `PersonRepository`, mocked).
- [x] Implement `PersonToolService` with `@Tool`-annotated `getPersonById` /
      `getPersonsByNationality` methods to pass the tests.
- [x] Write failing `@SpringBootTest(webEnvironment = RANDOM_PORT)` asserting the
      `ToolCallbackProvider` bean's callbacks include `getPersonById` and
      `getPersonsByNationality`.
- [x] Implement the `ToolCallbackProvider` `@Bean` via
      `MethodToolCallbackProvider.builder()` wiring `PersonToolService` to pass the
      test.

## account-mcp-service: domain + repository

- [x] Write failing `@DataJpaTest` for `AccountRepository.findByPersonId` asserting
      it returns the seeded accounts for person id `1`.
- [x] Implement `Account` entity, `AccountRepository` (`findByPersonId`), and
      `account-mcp-service/src/main/resources/import.sql` seed data — `personId`
      values MUST match the person ids seeded in `person-mcp-service` (person id
      `1` needs >= 2 accounts with distinct balances so the later balance-sum test
      is meaningful) — to make the test pass.

## account-mcp-service: tool layer

- [x] Write failing unit test for `AccountToolService.getAccountsByPersonId`
      (delegates to `AccountRepository`, mocked).
- [x] Implement `AccountToolService` with `@Tool`-annotated `getAccountsByPersonId`
      to pass the test.
- [x] Write failing `@SpringBootTest(webEnvironment = RANDOM_PORT)` asserting the
      `ToolCallbackProvider` bean's callbacks include `getAccountsByPersonId`.
- [x] Implement the `ToolCallbackProvider` `@Bean` for `account-mcp-service` to pass
      the test.

## sample-client: prompts + wiring

- [x] Write failing unit tests for the four prompt-building helper functions,
      asserting exact prompt text per spec (`"Find persons with {nationality}
      nationality."`, `"How many persons come from {nationality}?"`, `"How many
      accounts has person with {personId} ID?"`, and the balance variant with
      "Return person name, nationality and a total balance on his/her accounts.").
- [x] Implement the prompt-building helper functions to pass the tests.
- [x] Implement the `ChatClient` `@Bean` via `ChatClient.Builder.defaultTools(tools)`,
      injecting the auto-configured `ToolCallbackProvider` (non-test-first — wiring
      only, proven by the integration tests below).

## sample-client: REST endpoints (integration, require both MCP servers running + `OPENROUTER_API_KEY`)

- [x] Write failing `@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", ...)`
      `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` test for
      `GET /persons/nationality/{nationality}`, asserting the response contains the
      expected seeded person(s) for that nationality.
      (Used `RestTestClient`/`@AutoConfigureRestTestClient`, Boot 4/Spring 7's
      successor to `WebTestClient` for MVC apps — same intent as plan.md.)
- [x] Implement `PersonController` with `GET /persons/nationality/{nationality}` to
      pass the test.
- [x] Write failing integration test for `GET /persons/count-by-nationality/{nationality}`
      asserting the correct count.
- [x] Add `GET /persons/count-by-nationality/{nationality}` to `PersonController` to
      pass the test.
- [x] Write failing integration test for `GET /accounts/count-by-person-id/{personId}`
      asserting the correct account count for the seeded person.
- [x] Implement `AccountController` with `GET /accounts/count-by-person-id/{personId}`
      to pass the test.
- [x] Write failing integration test for `GET /accounts/balance-by-person-id/{personId}`
      asserting the response contains the correct person name, nationality, and the
      correct total balance summed across the seeded accounts for person id `1` —
      this is the multi-tool, cross-service orchestration criterion.
- [x] Add `GET /accounts/balance-by-person-id/{personId}` to `AccountController` to
      pass the test.

Verified live end-to-end against both running MCP servers + real OpenRouter calls
(`OPENROUTER_API_KEY` was set in the dev environment): all 8 sample-client tests
green, including a correct multi-tool balance response ("John Smith, an American
... total balance ... $3,500"). One transient failure on the very first tool call
of a cold run (empty tool result, self-resolved on retry) — consistent with the
LLM/tool-call non-determinism risk called out in plan.md; reran clean twice.

## API docs (springdoc)

- [x] Add `springdoc-openapi-starter-webmvc-ui` (spring init resolved `3.1.0`,
      newer than plan.md's `2.8.6` guess — took the resolved latest per stack
      notes) to `sample-client`, and `@Tag` (class-level) + `@Operation`
      (method-level) annotations on `PersonController` and `AccountController`.
- [x] Write failing test asserting `GET /swagger-ui.html` and `GET /v3/api-docs`
      both return a success status.
- [x] Fix/verify configuration so the docs test passes.

## Verification

- [x] Write `Makefile` with a `test` target running each module's Maven test suite
      (`mvn -q -f person-mcp-service/pom.xml test`,
      `mvn -q -f account-mcp-service/pom.xml test`,
      `mvn -q -f sample-client/pom.xml test`) — the fast/slice tests always run;
      the `sample-client` integration tests self-skip unless `OPENROUTER_API_KEY`
      is set and both MCP servers are already running on 8060/8040. Document that
      startup order (person-mcp-service, account-mcp-service, then sample-client)
      and the env var in a "Running tests" section of the root `README.md`.
      (Added a separate `test-client` target for the live-gated suite, plus a
      "Running the system" section with the curl/Swagger walkthrough.)

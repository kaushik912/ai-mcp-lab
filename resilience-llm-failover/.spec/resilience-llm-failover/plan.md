---
status: approved
---

# Plan: Gemini → OpenRouter resilience failover with Swagger API

## Stack detection

No existing project in this worktree. Greenfield Spring Boot project, scaffolded via `spring init` (spring-init skill), per ticket `stack_notes`.

## Architecture

- **Spring Boot 3.x** (Maven), Java 17
- **Resilience4j** (`resilience4j-spring-boot3` + `spring-boot-starter-aop`) — `@CircuitBreaker` annotation on the primary-provider call path, wired to a same-class fallback method per Resilience4j's rule (fallback must live in the same class, same params + trailing `Throwable`)
- **LangChain4j** clients for both providers:
  - Gemini: `dev.langchain4j:langchain4j-google-ai-gemini`
  - OpenRouter: `dev.langchain4j:langchain4j-open-ai` pointed at OpenRouter's OpenAI-compatible endpoint (`https://openrouter.ai/api/v1`) with the OpenRouter API key — OpenRouter has no dedicated LangChain4j module but is OpenAI-API-compatible, so the OpenAI client works unmodified against it
- **springdoc-openapi-starter-webmvc-ui** for Swagger UI + OpenAPI spec generation

## Components touched (all new)

- `pom.xml` — dependencies: web, aop, resilience4j-spring-boot3, langchain4j-google-ai-gemini, langchain4j-open-ai, springdoc-openapi-starter-webmvc-ui
- `application.yml` — circuit breaker instance config (`geminiService`), API keys sourced from env vars (`GEMINI_API_KEY`, `OPENROUTER_API_KEY`), springdoc paths
- `config/ModelConfig.java` — beans for `geminiModel` and `openRouterModel` (both typed `ChatLanguageModel`)
- `service/ResilientAiService.java` — `generateResponse(String prompt)` annotated `@CircuitBreaker(name="geminiService", fallbackMethod="fallbackToOpenRouter")`; fallback method routes to OpenRouter; both paths return a result object carrying `{response, provider}` so the controller can report which provider served the request
- `controller/AiController.java` — `POST /api/chat`, Swagger/OpenAPI annotations (`@Operation`, `@Tag`), request/response DTOs

## Key decisions / tradeoffs

- **Rate-limit-only trigger**: circuit breaker `recordExceptions` scoped to the HTTP 429 / rate-limit exception type LangChain4j surfaces for Gemini, not all `RuntimeException` — keeps the circuit from opening on unrelated errors (matches ticket's "mainly rate limited based resilience only").
- **Provider visibility**: response body includes a `provider` field (`GEMINI` or `OPENROUTER`) rather than a response header — simpler to verify manually and to document in Swagger.
- **No health-probe / auto-recovery beyond Resilience4j's built-in half-open state** — explicitly a non-goal.
- **Env-var API keys**, no secrets manager — POC scope.

## Risks

- LangChain4j's Google AI Gemini module is relatively new (beta-stage in early 2025 timelines); exact exception type thrown on Gemini 429 may need verification against the installed version and could require a broader `recordExceptions` fallback (e.g. catching a generic HTTP/rate-limit exception class) if the specific type isn't available.
- OpenRouter via the OpenAI-compatible client requires setting `baseUrl` explicitly — must confirm the LangChain4j `OpenAiChatModel.builder()` exposes a `baseUrl` setter (it does, as of 1.0.0-beta1).

## Testing seam

**None — explicit user request.** User stated this is a POC and does not want automated tests. Verification is manual: run the service, exercise `POST /api/chat` via `curl`, and force a 429 (or temporarily misconfigure the Gemini key) to confirm fallback to OpenRouter. No test-first task pairs in `tasks.md`; the final task documents these manual `curl` verification steps in the README instead of a `make test` target.

## API docs

**springdoc-openapi-starter-webmvc-ui**, default paths:
- Swagger UI: `/swagger-ui.html`
- OpenAPI spec: `/v3/api-docs`

No custom `springdoc` path config needed — defaults satisfy the acceptance criterion.

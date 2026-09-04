# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build/run/test via the Maven wrapper (Java 25 required):

```bash
./mvnw clean package        # build
./mvnw spring-boot:run       # run (listens on port 8081)
./mvnw test                  # run all tests
./mvnw test -Dtest=McpAppApplicationTests#testHttpExchange   # run a single test method
```

Requires the `OPEN_AI_KEY` environment variable set (used as the Groq API key, see below).

## Architecture

Spring Boot demo combining Spring AI's MCP (Model Context Protocol) client with a Groq-hosted LLM, plus a set of Spring HTTP client patterns exercised against `jsonplaceholder.typicode.com`.

- **`AiController`** (`controller/AiController.java`) — exposes `POST /ai/chat` and `POST /ai/groq`, both backed by the same `ChatClient` (query param `query`). The `ChatClient` is built with `defaultToolCallbacks(toolCallbackProvider)`, so tools exposed by the configured MCP server(s) are available to every prompt, plus a `SimpleLoggerAdvisor` for request/response logging.
- **MCP client wiring** — `spring-ai-starter-mcp-client` autoconfigures the `ToolCallbackProvider` from `src/main/resources/servers.json` (stdio transport, configured via `spring.ai.mcp.client.stdio.servers-configuration` in `application.properties`). `servers.json` currently defines a `filesystem` MCP server launched via `npx -y @modelcontextprotocol/server-filesystem <paths>` — the paths are hardcoded to a local Mac directory (`/Users/durgesh_mac/...`) and must be updated for this machine before MCP tool calls will work.
- **LLM backend** — despite using `spring-ai-starter-model-openai` / `spring.ai.openai.*` properties, the base URL is overridden to Groq (`https://api.groq.com/openai`), model `llama-3.1-8b-instant`. Both `/ai/chat` and `/ai/groq` currently hit the same Groq-backed client (no separate OpenAI config exists).
- **`config` package** — unrelated to the AI/MCP flow; it's a self-contained demonstration of three Spring HTTP client styles against the JSONPlaceholder API:
  - `RestTemplate` and `RestClient` beans (classic/modern synchronous clients)
  - `WebClient` bean (reactive client)
  - `PostHttpService` — a declarative `@HttpExchange` interface proxied via `HttpServiceProxyFactory` (`ProjectConfig.postHttpService()`), built on the `RestTemplate` adapter
  - `Post` is the shared record DTO used across all three client styles.
- **Tests** (`McpAppApplicationTests`) are integration-style, hitting the live `jsonplaceholder.typicode.com` API to exercise each HTTP client style (`RestTemplate`, `WebClient`, `RestClient`, `PostHttpService`) — not unit tests with mocks.

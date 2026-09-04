# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Run from this directory (`memory_app/`), using the Maven wrapper:

- Build: `./mvnw clean install`
- Run the app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test: `./mvnw test -Dtest=FirstProjectApplicationTests#contextLoads`

The app requires `OPENAI_API_KEY` in the environment (used by `spring.ai.openai.api-key` in `application.properties`), and a running MySQL instance matching `spring.datasource.url=jdbc:mysql://localhost:3306/spring_ai_yt` (see credentials in `application.properties`). Chat memory JDBC schema is auto-initialized on startup (`spring.ai.chat.memory.repository.jdbc.initialize-schema=ALWAYS`).

Server runs on port `8082` (`server.port`).

## Architecture

Spring Boot 3.5.5 / Java 21 app (base package `com.spring.ai.firstproject`) built on Spring AI 1.0.1, demonstrating a chat endpoint backed by persistent, per-user conversation memory.

- `config/AiConfig` — wires the two central beans:
  - `ChatMemory`: `MessageWindowChatMemory` (max 10 messages) backed by `JdbcChatMemoryRepository` (MySQL-persisted).
  - `ChatClient`: configured with `MessageChatMemoryAdvisor` (injects the chat memory keyed by `ChatMemory.CONVERSATION_ID`), `SimpleLoggerAdvisor`, and `SafeGuardAdvisor` (blocks the sensitive word "games"). Uses OpenAI model `gpt-4o-mini`, temperature 0.3, maxTokens 200 (independent of the `gpt-4.1` model set in `application.properties`, which is the default `spring.ai.openai.chat.options.model`).
- `advisors/TokenPrintAdvisor` — a custom `CallAdvisor`/`StreamAdvisor` that logs request/response content and token usage (prompt/completion/total). Not currently registered on the `ChatClient` in `AiConfig` (only referenced advisors are the three above).
- `service/ChatService` + `ChatServiceImpl` — builds prompts from `.st` template resources (`resources/prompts/system-message.st`, `user-message.st`) via `ChatClient.Builder`'s `system()`/`user()` template binding (`{concept}` placeholder).
  - `chatTemplate(query, userId)` sets `ChatMemory.CONVERSATION_ID` to `userId` via `advisors(spec -> spec.param(...))`, so responses are memory-scoped per user.
  - `streamChat(query)` does **not** set a conversation ID / memory advisor param — streaming responses are not tied to per-user chat memory the way `chatTemplate` is.
- `controllers/ChatController` — exposes:
  - `GET /chat?q={query}` with required header `userId` → calls `chatTemplate`, returns full response.
  - `GET /stream-chat?q={query}` → calls `streamChat`, returns `Flux<String>` (SSE-style streaming).

`schema/schema-h2.sql` is empty/unused; actual persistence is MySQL, not H2, despite the filename.

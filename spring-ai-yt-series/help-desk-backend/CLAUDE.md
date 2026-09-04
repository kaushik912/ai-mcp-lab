# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Spring Boot backend for an AI help-desk assistant ("Liza"). It exposes a chat API (`AiController`) backed by
Spring AI's `ChatClient`, which uses OpenAI (`gpt-4o-mini`) with tool-calling to create/update/look-up support
tickets in MySQL and to "send" notification emails.

Sibling repos in this monorepo: `../helpdesk` is just the raw system-prompt persona text (a duplicate/source of
`src/main/resources/helpdesk-system.st`, loaded by this app at runtime), and `../helpdesk-frontend` is the React
frontend that calls this backend's `/api/v1/helpdesk` endpoints.

## Commands

Build/run via the Maven wrapper (Java 25 toolchain, Spring Boot 3.5.6, Spring AI 1.0.3):

```bash
./mvnw clean install          # build + run tests
./mvnw test                   # run all tests
./mvnw test -Dtest=HelpDeskBackendApplicationTests#contextLoads   # run a single test method
./mvnw spring-boot:run         # run the app locally (port 8081)
```

Runtime requirements before starting the app:
- MySQL running with a `spring_ai_yt` database (connection/creds in `application.yml`; `ddl-auto: update` will
  create the `help_desk_tickets` table and the chat-memory schema automatically).
- `OPENAI_API_KEY` env var set (referenced in `application.yml` as `${OPENAI_API_KEY}`).

## Architecture

Request flow: `AiController` (`/api/v1/helpdesk`, POST for a single response and `/api/v1/helpdesk/stream` for
an SSE/Flux stream) → `AIService` → Spring AI `ChatClient` bean (built in `config/AiConfig.java`) → OpenAI, with
tool-calling into `tools/TicketDatabaseTool` and `tools/EmailTool`.

- **Conversation identity**: every request must include a `ConversationId` header; it's threaded into the
  `ChatClient` as `ChatMemory.CONVERSATION_ID` so memory is scoped per conversation.
- **Chat memory**: `AiConfig` wires a `MessageWindowChatMemory` (last 15 messages) backed by
  `JdbcChatMemoryRepository`, so conversation history persists in MySQL, not in-process.
- **System prompt**: loaded from `src/main/resources/helpdesk-system.st` as a classpath `Resource` and applied
  per-request in `AIService`. This file defines the assistant's persona, ticket-collection workflow, dedup rules
  before creating a ticket, and the ticket field schema — treat it as the source of truth for assistant behavior,
  not just a system message.
- **Tools exposed to the LLM** (via `@Tool` methods, auto-invoked by `ChatClient`):
  - `TicketDatabaseTool`: create/update/get-by-email ticket, plus `getCurrentTime`. Delegates to
    `TicketService` → `TicketRepository` (Spring Data JPA over the `Ticket` entity).
  - `EmailTool`: `sendEmailToSupportTeam` — currently just logs to stdout, not a real email integration.
- **Domain model**: `Ticket` entity (`help_desk_tickets` table) has `Priority` (LOW/MEDIUM/HIGH/URGENT) and
  `Status` (OPEN/CLOSED/RESOLVED) enums, a unique `email` column (one open ticket lookup per user), and
  `@PrePersist`/`@PreUpdate` hooks that stamp `createdOn`/`updatedOn`.

## Notes

- `application.yml` currently has hardcoded local MySQL credentials committed — be careful not to introduce real
  secrets there; prefer env var substitution (as already done for `OPENAI_API_KEY`).
- CORS is hardcoded in `AiController` to `http://localhost:5173` (the `helpdesk-frontend` Vite dev server).

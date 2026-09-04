# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build:
```
./mvnw clean install
```

Run the app (port 8082):
```
export OPENAI_API_KEY=<key>
./mvnw spring-boot:run
```

Run all tests:
```
./mvnw test
```

Run a single test:
```
./mvnw test -Dtest=FirstProjectApplicationTests#saveDataToVectorDatabase
```
Note: `FirstProjectApplicationTests` is a `@SpringBootTest` that requires a live MariaDB instance (see below) and a valid `OPENAI_API_KEY` to run — it is really a one-off data-loading script (loads `Helper.getData()`'s hardcoded Java/Spring facts into the vector store), not a behavioral unit test.

## Runtime dependencies

- MariaDB reachable at `jdbc:mariadb://localhost:3309/springai` (user `root` / `root1234`, see `application.properties`). Used as the Spring AI vector store (`spring-ai-starter-vector-store-mariadb`), auto-initialized (`spring.ai.vectorstore.mariadb.initialize-schema=true`), COSINE distance, 1536 dimensions.
- `OPENAI_API_KEY` env var — the app calls OpenAI directly (`spring-ai-starter-model-openai`).
- `src/main/resources/schema/schema-h2.sql` exists but is empty and unreferenced — the app actually uses MariaDB, not H2; ignore this file.

## Architecture

This is a Spring Boot 3.5.5 / Java 21 / Spring AI 1.0.1 RAG demo, structurally similar to the sibling `advance_rag` project but earlier/WIP — the `QuestionAnswerAdvisor`-based RAG path is present only as commented-out code.

Request flow: `ChatController` → `ChatService` (interface) → `ChatServiceImpl`.

- **`ChatController`** (`controllers/ChatController.java`): two endpoints.
  - `GET /chat?q=...` (requires `userId` header) → `chatTemplate`, non-streaming, RAG-augmented.
  - `GET /stream-chat?q=...` → `streamChat`, streaming, **not** RAG-augmented (no vector retrieval — plain system+user prompt only).
- **`ChatServiceImpl`** (`service/ChatServiceImpl.java`) is the core logic:
  - `chatTemplate`: builds a `RetrievalAugmentationAdvisor` per-request (`VectorStoreDocumentRetriever` topK=3, similarityThreshold=0.5, wrapped in `ContextualQueryAugmenter.allowEmptyContext(true)`) and attaches it via `.advisors(advisor)` on the `ChatClient` prompt. This is the current (spring-ai-rag module) RAG approach.
  - Commented-out in the same method: an older approach using `QuestionAnswerAdvisor` directly against the `VectorStore`, and manual `similaritySearch` + context-string injection into the system prompt template. Left in place for reference/comparison — do not assume it's active.
  - `saveData`: wraps raw strings into `Document`s and writes them to the `VectorStore`. Only invoked from the test class today.
- **`AiConfig`** (`config/AiConfig.java`): defines the `ChatClient` bean.
  - Default advisors: `MessageChatMemoryAdvisor` (chat memory), `SimpleLoggerAdvisor`, `SafeGuardAdvisor(List.of("games"))` (blocks the word "games").
  - `ChatMemory` bean uses `InMemoryChatMemoryRepository` + `MessageWindowChatMemory` (maxMessages=10). A JDBC-backed chat memory repository bean is commented out (dependency for it is also commented out in `pom.xml`).
  - Chat options: model `gpt-4o-mini`, temperature 0.3, maxTokens 200 — overrides the `gpt-4.1` set in `application.properties`.
- **`TokenPrintAdvisor`** (`advisors/TokenPrintAdvisor.java`): custom `CallAdvisor`/`StreamAdvisor` that logs request/response content and token usage. Defined but not currently wired into any advisor chain — add it to `AiConfig`'s `defaultAdvisors(...)` (or per-request `.advisors(...)`) to activate it.
- **Prompt templates** (`resources/prompts/`): `system-message.st` instructs the model to answer only from a `{documents}` placeholder (used by the commented-out manual-context path, not the active `RetrievalAugmentationAdvisor` path); `user-message.st` just injects `{query}`.
- **`Helper.getData()`** (`helper/Helper.java`): static list of ~20 hardcoded Java/Spring trivia strings, used solely to seed the vector store from the test.

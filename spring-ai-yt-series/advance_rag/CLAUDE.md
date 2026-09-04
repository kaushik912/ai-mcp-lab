# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build: `./mvnw clean install`
Run app: `./mvnw spring-boot:run` (starts on port 8082, set in `application.properties`)
Run all tests: `./mvnw test`
Run a single test class: `./mvnw test -Dtest=FirstProjectApplicationTests`
Run a single test method: `./mvnw test -Dtest=FirstProjectApplicationTests#testPdfDataLoader`
Package jar: `./mvnw package`

Requires `OPENAI_API_KEY` env var (used by `spring.ai.openai.api-key` in `application.properties`).

Requires a running MariaDB instance matching `spring.datasource.url=jdbc:mariadb://localhost:3309/springai` (user `root` / `root1234`) — this is the vector store backend (`spring-ai-starter-vector-store-mariadb`, 1536 dims, COSINE distance, schema auto-initialized).

`src/main/resources/schema/schema-h2.sql` exists but is empty — not wired to an active datasource; can be ignored/removed if cleaning up.

## Architecture

Single Spring Boot module, package root `com.spring.ai.firstproject`. Java 25, Spring Boot 3.5.5, Spring AI 1.0.1 (BOM-managed).

Request flow: `ChatController` (`POST /chat?q=...`) → `ChatService`/`ChatServiceImpl` → Spring AI `ChatClient` (OpenAI, `gpt-4o-mini`) with RAG advisors → response string.

**Two distinct advisor pipelines exist and are not currently combined:**
- `AiConfig.chatClient()` bean sets the *default* advisor chain used for the base client: `SimpleLoggerAdvisor` + `SafeGuardAdvisor(List.of("games"))` (blocks queries mentioning "games"), plus default `OpenAiChatOptions` (temp 0.3, maxTokens 200). `TokenPrintAdvisor` (custom `CallAdvisor`/`StreamAdvisor` in `advisors/`, logs prompt/response/token usage) exists but is not currently registered anywhere.
- `ChatServiceImpl.getResponse()` builds a *per-request* `RetrievalAugmentationAdvisor` (from `spring-ai-rag`) on top of that client, chaining:
  1. `RewriteQueryTransformer` (rewrites the query for better retrieval)
  2. `TranslationQueryTransformer` (translates query to Hindi — likely a leftover/experiment)
  3. `MultiQueryExpander` (expands into 3 queries)
  4. `VectorStoreDocumentRetriever` (topK=3, similarityThreshold=0.3, backed by the MariaDB `VectorStore`)
  5. `ConcatenationDocumentJoiner`
  6. `ContextualQueryAugmenter` (injects retrieved docs into the prompt)

  Because the transformer chain rewrites then translates to Hindi before retrieval/expansion, retrieval effectively runs against a Hindi-translated query — check this is intentional before changing retrieval behavior.

Prompt templates live in `src/main/resources/prompts/` (`system-message.st`, `user-message.st`) using `{documents}` / `{query}` placeholders, but `ChatServiceImpl` currently calls `.user(userQuery)` directly rather than loading these `.st` resources — the injected `Resource` fields (`userMessage`, `systemMessage`) are unused. `ContextualQueryAugmenter` handles document injection instead.

**Data ingestion** (not exposed via any HTTP endpoint, only exercised in tests):
- `DataLoader`/`DataLoaderImpl`: reads `sample_data.json` (via `JsonReader`) or `cricket_rules.pdf` (via `PagePdfDocumentReader`) from classpath into `Document` lists.
- `DataTransformer`/`DataTransformerImpl`: splits documents using `TokenTextSplitter(300, 400, 10, 5000, true)`.
- `ChatService.saveData(List<String>)` wraps raw strings into `Document`s and writes to the `VectorStore`.
- `FirstProjectApplicationTests` doubles as the ingestion driver: `testPdfDataLoader()` loads the PDF, transforms it, and writes it into the live MariaDB vector store as a side effect of running the test — not a pure unit test.

`Helper.getData()` supplies a static list of hardcoded Java/Spring sample sentences, seemingly for manual/ad-hoc ingestion testing.

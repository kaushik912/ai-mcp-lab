# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build: `./mvnw clean install`
Run: `./mvnw spring-boot:run` (starts on port 8082, set in `application.properties`)
Run all tests: `./mvnw test`
Run a single test: `./mvnw test -Dtest=FirstProjectApplicationTests`
Run a single test method: `./mvnw test -Dtest=FirstProjectApplicationTests#contextLoads`

Requires `OPENAI_API_KEY` env var (referenced in `application.properties` as `spring.ai.openai.api-key`).

## Architecture

Spring AI "advisor pattern" demo. Base package: `com.spring.ai.firstproject`.

- `config/AiConfig.java` — builds the single `ChatClient` bean. This is where advisors and default chat options are wired: `TokenPrintAdvisor` (custom) and `SafeGuardAdvisor` (built-in, blocks the word "games") are registered via `defaultAdvisors(...)`. Default model is set here (`gpt-4o-mini`, temp 0.3) and overrides the model in `application.properties` (`gpt-4.1`) for actual requests — `application.properties`' model setting is effectively unused because `AiConfig` supplies explicit `OpenAiChatOptions`.
- `advisors/TokenPrintAdvisor.java` — custom advisor implementing both `CallAdvisor` and `StreamAdvisor`. On `adviseCall`, logs the request, the response, and prompt/completion/total token usage from `ChatResponse.getMetadata().getUsage()`. `adviseStream` currently just passes the Flux through with no per-chunk logging (token counts aren't available mid-stream). This is the reference implementation to follow when adding new advisors.
- `controllers/ChatController.java` — two GET endpoints: `/chat?q=` (synchronous, returns `String`) and `/stream-chat?q=` (returns `Flux<String>` for SSE-style streaming). Both delegate straight to `ChatService`.
- `service/ChatService.java` / `ChatServiceImpl.java` — wraps `ChatClient`. Loads system/user prompts from classpath resources (`prompts/system-message.st`, `prompts/user-message.st`) via `PromptTemplate`-style `.st` files rather than inline strings. The user template takes a `concept` param bound to the incoming query string.
- `src/main/resources/prompts/` — externalized prompt templates (Spring AI `.st` StText format). Edit these rather than hardcoding prompt text in Java.

## Notes

- No vector store / RAG in this project — it's purely advisor-pattern focused.
- `logging.level.org.springframework.ai.chat.client.advisor=DEBUG` is enabled in `application.properties`, useful for tracing advisor chain execution.
- Ollama dependency/config is present but commented out in both `pom.xml` and `application.properties` — OpenAI is the only active model provider.

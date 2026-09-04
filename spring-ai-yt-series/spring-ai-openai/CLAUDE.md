# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./mvnw clean install`
- Run: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test: `./mvnw test -Dtest=FirstProjectApplicationTests#contextLoads`

App listens on port `8081` (set in `application.properties`, overriding Spring Boot's default 8080).

### Required environment

- `OPENAI_API_KEY` must be set — `application.properties` reads it via `${OPENAI_API_KEY}` for `spring.ai.openai.api-key`.
- Ollama must be running locally at `http://localhost:11434` with model `codellama:latest` pulled, since the active `/chat` endpoint currently calls the Ollama client (see Architecture below).

## Architecture

Minimal Spring Boot + Spring AI demo (Java 21, Spring Boot 3.5.5, Spring AI 1.0.1 BOM) wiring up **two** parallel `ChatClient` beans — one for OpenAI, one for Ollama — rather than the single auto-configured client.

- `spring.ai.chat.client.enabled=false` in `application.properties` disables Spring AI's default autoconfigured `ChatClient` bean.
- `config/AiConfig.java` manually defines two named `ChatClient` beans instead: `openAiChatClient` (wraps `OpenAiChatModel`) and `ollamaChatClient` (wraps `OllamaChatModel`). Both model beans (`OpenAiChatModel`, `OllamaChatModel`) come from their respective Spring AI starters (`spring-ai-starter-model-openai`, `spring-ai-starter-model-ollama`) which are both on the classpath.
- `controllers/ChatController.java` injects both named clients via `@Qualifier("openAiChatClient")` / `@Qualifier("ollamaChatClient")`, but the single `GET /chat?q=...` endpoint currently calls only `ollamaChatClient` — despite the module being named `spring-ai-openai`. The `openAiChatClient` field is wired but unused. Be aware of this when asked to "use OpenAI" — the endpoint needs to be pointed at `openAiChatClient` explicitly.
- Model/behavior config (model name, temperature) for each provider lives in `application.properties` under `spring.ai.openai.chat.options.*` and `spring.ai.ollama.chat.options.*`, not in code.
- Lombok is on the classpath (annotation processor configured in `pom.xml`) but not currently used in source.

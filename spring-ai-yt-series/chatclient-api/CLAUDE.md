# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build/compile:
```
./mvnw compile
```

Run the app (listens on port 8081):
```
./mvnw spring-boot:run
```
Requires `OPENAI_API_KEY` env var (used by `spring.ai.openai.api-key` in `application.properties`).

Run all tests:
```
./mvnw test
```

Run a single test class/method:
```
./mvnw test -Dtest=FirstProjectApplicationTests
./mvnw test -Dtest=FirstProjectApplicationTests#testTemplateRender
```

Package:
```
./mvnw package
```

## Architecture

Minimal Spring AI demo (`groupId: com.spring.ai.firstproject`, package `com.spring.ai.firstproject`) showing bare `ChatClient` usage — no advisors, memory, or RAG.

- `config/AiConfig.java` — defines the single `ChatClient` bean via `ChatClient.Builder`, setting a default system prompt and default `OpenAiChatOptions` (model `gpt-4o-mini`, temperature 0.3, maxTokens 200). Note: `application.properties` separately configures `spring.ai.openai.chat.options.model=gpt-4.1` — the two model settings currently disagree; the `ChatClient` builder's option takes precedence for calls that don't override it.
- `controllers/ChatController.java` — single `GET /chat?q=...` endpoint, delegates to `ChatService`. Has commented-out alternate constructors exploring direct `ChatClient` injection and multi-model (`openAiChatClient`/`ollamaChatClient` qualifiers) wiring — those are exploratory dead code, not active paths.
- `service/ChatService.java` / `ChatServiceImpl.java` — `chat(query)` builds an ad-hoc `Prompt` and calls `chatClient.prompt().user(...).call().content()`. `chatTemplate()` is a second, test-only method demonstrating `PromptTemplate`-based system/user prompts loaded from classpath resources (`prompts/system-message.st`, `prompts/user-message.st`) with `.st` (StringTemplate/String Template Mustache-style) placeholders.
- `entity/Tut.java` — plain POJO (title/content/createdYear), currently unused by any endpoint logic; likely scaffolding left over from a tutorial series.
- `src/main/resources/prompts/*.st` — externalized prompt templates referenced via `@Value("classpath:/prompts/...")` `Resource` injection.

Much of the codebase (in `ChatServiceImpl`, `ChatController`, `AiConfig`) contains large blocks of commented-out alternative implementations (multi-model Ollama/OpenAI qualifiers, raw `PromptTemplate` rendering, per-call `OpenAiChatOptions`) — these are intentional reference/scratch code from the tutorial series, not to be treated as broken code needing cleanup unless asked.

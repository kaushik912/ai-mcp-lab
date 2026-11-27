# spring-ai-basics
Learning demo: one Spring Boot app, one package per Spring AI concept (basics, memory, structured, rag, tools, workflow). Keep it basic; don't add infrastructure.

## Stack
Java 17, Spring Boot 4.1.1, Spring AI 2.0.1 (`spring-ai-starter-model-openai`, `spring-ai-vector-store`), Maven wrapper. No Docker, no DB.

## Commands
- `./mvnw spring-boot:run` (port 8080); `./mvnw test`
- README has curl commands per concept

## Config
- Env `OPENROUTER_API_KEY` (required to run; also enables `RagGoldenEvalTest`), `OPENROUTER_MODEL` (default `deepseek/deepseek-v4.1-flash`; alternative `qwen/qwen3.7-flash`)
- Embeddings: `openai/text-embedding-3-small` via OpenRouter
- Spring AI 2.x properties have no `.options` segment: `spring.ai.openai.chat.model`, `spring.ai.openai.embedding.model`

## Gotchas
- Fake login: `X-User` header (alice, bob = acme; carol = globex) in `common/CurrentUser`
- SimpleVectorStore is in-memory; startup re-embeds 5 docs (needs network + key)
- `ChatClient.Builder` is prototype-scoped, so each class builds its own client with its own default advisors

# a2a-hello-world
A2A (Agent2Agent) hello-world: LLM-routed orchestrator delegates to joke/quote agents via JSON-RPC 2.0 `message/send`. No A2A SDK, Map-based helpers.

## Stack
Java 17, Spring Boot 3.4.1, Spring AI 1.0.0 (ollama/openai/anthropic starters), Maven.

## Commands
- Maven wrapper: `./mvnw spring-boot:run`
- Test: `./mvnw test` (only spring-boot-starter-test dep; no tests in src)
- Port 8080. `GET /ask?q=...`; agents `POST /joke`, `POST /quote`; cards `/{agent}/.well-known/agent-card.json`

## Config
- `a2a.llm.provider` = ollama (default) | openai | anthropic; override `--a2a.llm.provider=openai`
- Ollama default: http://localhost:11434, model llama3.1 (needs local Ollama)
- Env: `OPENAI_API_KEY`, `ANTHROPIC_API_KEY` (only for those providers)

## Layout (`com.example.a2a`)
- OrchestratorController, JokeAgentController, QuoteAgentController, A2aSupport, AiConfig
- `llm/`: ChatModelFactory + Ollama/OpenAi/Anthropic factories

## Gotchas
- `spring.ai.model.chat=none`: ChatModels built manually by factories, not auto-config
- openai/anthropic api-key default to `not-used` placeholder so boot works w/o keys
- Orchestrator calls agents in same app over HTTP (self-call on 8080)
- No Docker

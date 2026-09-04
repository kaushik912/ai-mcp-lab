# A2A Hello World (Spring Boot)

Minimal **Agent2Agent (A2A)** demo: an LLM-routed orchestrator delegates to
one of two specialist agents over A2A's JSON-RPC 2.0 / HTTP protocol.

## Architecture

```
GET /ask?q=...
      │
      ▼
OrchestratorController  ──(LLM routes)──►  joke-agent | quote-agent
      │  POST message/send (JSON-RPC)              │
      └──────────────────────────────────────────► JokeAgentController  (POST /joke)
                                                     QuoteAgentController (POST /quote)
```

- **OrchestratorController** (`/ask`) — an LLM (temp 0.0) picks `joke-agent`
  or `quote-agent` from the query, then sends an A2A `message/send` request
  to that agent's URL and unwraps the reply.
- **JokeAgentController** (`/joke`) — LLM (temp 0.9) tells a joke; picks a
  random topic for generic asks, respects specific topics otherwise.
- **QuoteAgentController** (`/quote`) — LLM (temp 0.8) returns one
  inspirational quote + author.
- Each agent exposes an A2A discovery card at
  `/{agent}/.well-known/agent-card.json`.
- **A2aSupport** — plain `Map`-based helpers to build/parse A2A JSON-RPC
  request/response bodies (no A2A SDK dependency).

## LLM provider

Pluggable via `a2a.llm.provider` (`ollama` default | `openai` | `anthropic`).
`AiConfig` picks the matching `ChatModelFactory` and builds three
`ChatClient` beans (joke/quote/router) at different temperatures. Spring AI's
chat auto-config is disabled (`spring.ai.model.chat=none`) so unused
providers don't demand API keys.

| Provider  | Config |
|-----------|--------|
| ollama    | `a2a.llm.ollama.base-url`, `a2a.llm.ollama.model` (default `llama3.1`) |
| openai    | `OPENAI_API_KEY` env, `a2a.llm.openai.model` (default `gpt-4o-mini`) |
| anthropic | `ANTHROPIC_API_KEY` env, `a2a.llm.anthropic.model` (default `claude-sonnet-5`) |

## Run

```bash
# default: local Ollama on :11434, model llama3.1
./mvnw spring-boot:run

curl "http://localhost:8080/ask?q=tell+me+a+joke+about+cats"
curl "http://localhost:8080/ask?q=give+me+an+inspiring+quote"
```

Switch provider: `--a2a.llm.provider=openai` (or `anthropic`) + matching API
key env var.

## Stack

Java 17, Spring Boot 3.4.1, Spring AI 1.0.0 (Ollama/OpenAI/Anthropic
starters), Maven.

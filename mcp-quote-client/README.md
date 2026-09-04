# MCP Learning Setup — Quote Server + LLM Client

A minimal, end-to-end **Model Context Protocol (MCP)** playground built with Spring Boot 4.1 + Spring AI 2.0.

- **`mcp-quote-server/`** — an MCP **server** that exposes a `randomQuote` tool (and a plain REST endpoint).
- **`mcp-quote-client/`** (this project) — an MCP **host**: it owns the LLMs (Ollama + Anthropic + Gemini), connects to the server, discovers its tools, and lets the model decide when to call them.

---

## The one mental model

> **MCP is USB for tools.** A *server* advertises capabilities and executes them; a *host* with an LLM discovers and calls them over a standard wire. Tool selection is the LLM's job, driven by the tool **description** — never keyword matching.

---

## Architecture

```
 ┌──────────────────┐  SSE (tools/list)  ┌──────────────────┐
 │ mcp-quote-client │ ───────────────►  │ mcp-quote-server  │
 │      (host)      │ ◄─────────────── │  (MCP server,     │
 └────────┬─────────┘  tool call/result │    :8080)         │
          │                             └──────────────────┘
       ├── ChatClient(Ollama)     — local model, real
       ├── ChatClient(Anthropic)  — cloud model, needs real API key
       └── ChatClient(Gemini)     — cloud model, free tier, needs real API key
```

- On startup, Spring AI's MCP client autoconfig connects to `mcp-quote-server` over SSE and runs `tools/list`. The discovered tools are wrapped into a `ToolCallbackProvider` — the client never hardcodes the tool name.
- `ChatController` builds three `ChatClient`s (Ollama, Anthropic, Gemini), each given the **same** `mcpTools`. Only the underlying model ("brain") differs; tool availability is identical. Gemini is wired through Spring AI's OpenAI starter, pointed at Google's OpenAI-compatible endpoint.
- The LLM decides per-request whether to call `randomQuote`, based on the tool's description and the user's prompt.

## Endpoints

| Method | Path              | Description                                      |
|--------|-------------------|---------------------------------------------------|
| GET    | `/chat/ollama?q=`     | Prompt the local Ollama model (tool-enabled)   |
| GET    | `/chat/anthropic?q=`  | Prompt Claude (tool-enabled)                   |
| GET    | `/chat/gemini?q=`     | Prompt Gemini, free tier (tool-enabled)        |
| GET    | `/tools`              | Debug: list MCP tools discovered at startup    |

## Configuration (`application.properties`)

- Runs on port `8081` (mcp-quote-server owns `8080`).
- `spring.ai.mcp.client.sse.connections.quotes.url` — points at the mcp-quote-server.
- `spring.ai.ollama.*` — expects Ollama running locally at `11434`, model `mistral`.
- `spring.ai.anthropic.api-key` — defaults to a dummy key (app boots fine; `/chat/anthropic` 401s until you export `ANTHROPIC_API_KEY`).
- `spring.ai.openai.base-url` — points at Google's OpenAI-compatible Gemini endpoint (`https://generativelanguage.googleapis.com/v1beta/openai/`), model `gemini-flash-lite-latest`.
- `spring.ai.openai.api-key` — defaults to a dummy key (app boots fine; `/chat/gemini` 401s until you export `GEMINI_API_KEY`, free from [Google AI Studio](https://aistudio.google.com/apikey)).

## Running

**`mcp-quote-server` (:8080) must be running before you start this app — startup fails otherwise, even for `/chat/ollama`.**

```bash
# 1. start the MCP server first (mcp-quote-server, :8080)
# 2. start Ollama locally, ensure `mistral` model is pulled
# 3. run this client
export ANTHROPIC_API_KEY=sk-...   # optional, for /chat/anthropic
export GEMINI_API_KEY=...         # optional, free tier, for /chat/gemini
./mvnw spring-boot:run
```

```bash
curl "localhost:8081/tools"
curl "localhost:8081/chat/ollama?q=give+me+an+inspiring+quote"
curl "localhost:8081/chat/anthropic?q=give+me+an+inspiring+quote"
curl "localhost:8081/chat/gemini?q=give+me+an+inspiring+quote"
```

## Stack

Spring Boot 4.1 · Spring AI 2.0 · Java 17 · Spring AI MCP client (WebFlux/SSE transport) · Ollama + Anthropic + Gemini (via OpenAI-compatible endpoint) chat models

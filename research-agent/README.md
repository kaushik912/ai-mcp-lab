# Research Agent (Gemini)

End-to-end Java AI agent, following the [dev.to article's](https://dev.to/ashish_sharda_a540db2e50e/i-built-an-ai-agent-in-java-no-python-no-hype-just-code-1p2l)
architecture (ChatClient + MCP tool calling + RAG + session memory), built
on **Gemini only** (no Anthropic/OpenAI in this build).

## Architecture

```
POST /agent/knowledge  ─► SimpleVectorStore (in-memory, Gemini embeddings)
GET  /agent/ask?question=...&sessionId=...
      │
      ▼
 ChatClient (Gemini chat, via OpenAI-compatible endpoint)
   ├─ MessageChatMemoryAdvisor   — per-sessionId conversation history
   ├─ QuestionAnswerAdvisor      — RAG: injects relevant ingested docs
   └─ MCP tool callbacks         — discovered from research-agent-news-tool at startup
                                    (JSON-RPC/SSE, POST-free — model decides
                                    when to call searchNews)
```

- **AgentConfig** — wires the `ChatMemory`, `VectorStore`, and the single
  Gemini `ChatClient` bean with both advisors + MCP tool callbacks attached.
- **ResearchAgentController** — `POST /agent/knowledge` seeds the vector
  store; `GET /agent/ask` queries the agent.
- Gemini chat rides Spring AI's OpenAI starter pointed at Google's
  OpenAI-compatible endpoint. Gemini **embeddings** use the native
  `spring-ai-starter-model-google-genai-embedding` module instead — Gemini's
  OpenAI-compatible `/embeddings` response omits the `index` field each
  item needs, which the strict `openai-java` client rejects
  (`OpenAIInvalidDataException: index is not set`).

## Prerequisites

1. **`research-agent-news-tool` must be running first**, on port 8090 — this app's
   MCP client autoconfig connects to it at startup and fails to boot if it's
   unreachable.
2. A free Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey),
   exported as `GEMINI_API_KEY`.

## Run

```bash
# terminal 1 — start the MCP tool server first
cd ../research-agent-news-tool
mvn spring-boot:run

# terminal 2 — start this app
cd research-agent
export GEMINI_API_KEY=...
mvn spring-boot:run
```

Runs on **port 8080**.

## Try it

```bash
# 1. seed the knowledge base (RAG)
curl -s -X POST http://localhost:8080/agent/knowledge \
  -H "Content-Type: application/json" \
  -d '["Kaushik has a Spring Boot MCP demo repo called research-agent that uses Gemini as its LLM.",
       "The research-agent project talks to a separate MCP tool server called research-agent-news-tool running on port 8090."]'

# 2. ask a question answerable from the ingested docs (RAG)
curl -s -G http://localhost:8080/agent/ask \
  --data-urlencode "question=What LLM does the research-agent project use, and what MCP tool server does it talk to?"

# 3. ask something that triggers the MCP tool (searchNews)
curl -s -G http://localhost:8080/agent/ask \
  --data-urlencode "question=Search the news for Gemini and tell me the headline you found."

# 4. session memory — same sessionId carries context across calls
curl -s -G http://localhost:8080/agent/ask \
  --data-urlencode "sessionId=s1" --data-urlencode "question=My name is Kaushik."
curl -s -G http://localhost:8080/agent/ask \
  --data-urlencode "sessionId=s1" --data-urlencode "question=What is my name?"
```

All four were verified working end-to-end against live Gemini + the MCP
tool server during development of this app.

## Configuration (`application.properties`)

- `spring.ai.mcp.client.sse.connections.news.url` — points at research-agent-news-tool (:8090).
- `spring.ai.openai.*` — Gemini chat, via the OpenAI-compatible endpoint (`gemini-flash-lite-latest`).
- `spring.ai.model.embedding=none` — disables the OpenAI starter's own (incompatible) embedding autoconfig.
- `spring.ai.google.genai.embedding.*` — Gemini embeddings, native module (`gemini-embedding-001`).

Both `spring.ai.openai.api-key` and `spring.ai.google.genai.embedding.api-key`
read from the same `GEMINI_API_KEY` env var.

## Stack

Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 — MCP client (WebFlux/SSE
transport), OpenAI starter (Gemini chat), Google GenAI embedding starter
(Gemini embeddings), in-memory `SimpleVectorStore`, Maven.

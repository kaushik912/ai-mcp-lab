# research-agent
Gemini research agent: ChatClient + MCP tool calling + RAG (in-memory SimpleVectorStore) + per-session memory. MCP client of sibling `research-agent-news-tool`.

## Stack
Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 (mcp-client-webflux, openai starter, google-genai-embedding, vector-store, vector-store-advisor), Maven.

## Commands
- Maven wrapper: `./mvnw spring-boot:run`; `./mvnw test`
- Start `../research-agent-news-tool` (:8090) FIRST, else boot fails
- Port 8080. `POST /agent/knowledge` (JSON array of strings, seeds RAG); `GET /agent/ask?question=&sessionId=`

## Config
- Env: `GEMINI_API_KEY` (free, Google AI Studio; dummy default so boot works)
- MCP SSE connection `news` -> http://localhost:8090; SYNC, 30s
- Chat: gemini-flash-lite-latest via OpenAI starter at Google OpenAI-compat endpoint
- Embeddings: gemini-embedding-001 via native google-genai module

## Layout (`com.example.researchagent`)
ResearchAgentApplication, AgentConfig (ChatMemory, VectorStore, ChatClient w/ MessageChatMemoryAdvisor + QuestionAnswerAdvisor + MCP tools), ResearchAgentController

## Gotchas
- Embeddings must NOT use OpenAI-compat endpoint (missing `index` field -> OpenAIInvalidDataException); hence `spring.ai.model.embedding=none` + `spring.ai.model.embedding.text=google-genai`
- Vector store + memory are in-memory; lost on restart
- webflux on classpath -> servlet type forced
- No Docker

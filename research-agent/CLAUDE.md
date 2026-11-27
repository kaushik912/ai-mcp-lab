# research-agent
OpenRouter research agent: ChatClient + MCP tool calling + RAG (in-memory SimpleVectorStore) + per-session memory. MCP client of sibling `research-agent-news-tool`.

## Stack
Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 (mcp-client-webflux, openai starter, vector-store, vector-store-advisor), Maven.

## Commands
- Maven wrapper: `./mvnw spring-boot:run`; `./mvnw test`
- Start `../research-agent-news-tool` (:8090) FIRST, else boot fails
- Port 8080. `POST /agent/knowledge` (JSON array of strings, seeds RAG); `GET /agent/ask?question=&sessionId=`

## Config
- Env: `OPENROUTER_API_KEY` (dummy default so boot works), `OPENROUTER_MODEL` (optional)
- MCP SSE connection `news` -> http://localhost:8090; SYNC, 30s
- Chat: deepseek/deepseek-v4.1-flash via OpenAI starter at https://openrouter.ai/api/v1
- Embeddings: openai/text-embedding-3-small via same OpenAI starter / OpenRouter

## Layout (`com.example.researchagent`)
ResearchAgentApplication, AgentConfig (ChatMemory, VectorStore, ChatClient w/ MessageChatMemoryAdvisor + QuestionAnswerAdvisor + MCP tools), ResearchAgentController

## Gotchas
- Previously Gemini: its OpenAI-compat /embeddings omitted `index` (OpenAIInvalidDataException), forcing native google-genai module. Not an issue on OpenRouter.
- Vector store + memory are in-memory; lost on restart
- webflux on classpath -> servlet type forced
- No Docker

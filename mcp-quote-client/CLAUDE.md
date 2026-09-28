# mcp-quote-client
MCP host/client: 3 ChatClients (Ollama, Anthropic, Gemini) sharing MCP tools discovered from sibling `mcp-quote-server`.

## Stack
Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 (mcp-client-webflux, ollama, anthropic, openai), springdoc 3.1.0, Maven.

## Commands
- `./mvnw spring-boot:run` ; `./mvnw test`
- Start `../mcp-quote-server` (:8080) FIRST, else startup fails (even /chat/ollama)
- Port 8081

## Endpoints
`GET /chat/ollama?q=`, `/chat/anthropic?q=`, `/chat/gemini?q=`, `/tools` (debug: discovered MCP tools). `openapi.json` at root.

## Config
- MCP SSE connection `quotes` -> http://localhost:8080; type SYNC, 30s timeout
- Ollama: localhost:11434, model mistral (needs local Ollama)
- Env: `ANTHROPIC_API_KEY`, `GEMINI_API_KEY` (dummy defaults; 401 on those endpoints until set)

## Layout (`com.example.mcpclient`)
McpClientApplication, ChatController (all wiring in one class)

## Gotchas
- Gemini via OpenAI starter pointed at Google OpenAI-compatible endpoint
- webflux on classpath (MCP transport) -> `spring.main.web-application-type=servlet` forced
- Tool name (randomQuote) discovered at runtime, not configured
- No Docker

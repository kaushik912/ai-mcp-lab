# research-agent-news-tool
MCP server exposing `searchNews(topic)` `@Tool` over SSE; keyword-filters hardcoded headlines. Consumed by sibling `research-agent`. No LLM, no API key.

## Stack
Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-starter-mcp-server-webmvc`), Maven.

## Commands
- Maven wrapper: `./mvnw spring-boot:run`; `./mvnw test`
- Port 8090
- Verify: `POST http://localhost:8090/mcp` JSON-RPC `tools/list` w/ `Accept: application/json, text/event-stream` (per README)

## Layout (`com.example.newssearchtool`)
NewsSearchToolApplication, NewsSearchTools (@Tool contract), NewsSearchService (swap for real news API; keep tool contract)

## Gotchas
- Must be running before research-agent starts
- No Docker

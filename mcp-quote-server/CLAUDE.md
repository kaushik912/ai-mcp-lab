# mcp-quote-server
MCP server exposing `randomQuote` tool (`@Tool`, QuoteTools) over SSE + plain REST `GET /randomQuote`. Consumed by sibling `mcp-quote-client`.

## Stack
Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-starter-mcp-server-webmvc`), Maven.

## Commands
- `./mvnw spring-boot:run` ; `./mvnw test`
- Port: default 8080 (application.properties only sets app name `randomquote`); SSE endpoint `/sse`
- No API keys, no LLM

## Layout (`com.example.randomquote`)
RandomquoteApplication, QuoteService, QuoteTools (@Tool), QuoteController (REST)

## Testing helpers
- `src/test/.../QuoteMcpTester.java`: scratch main() MCP client (no LLM); needs app already running on 8080; run from IDE
- `src/main/resources/scripts/QuoteMcpTesterPython.py`: same via python `mcp` pkg (use a venv); hits http://localhost:8080/sse
- `quotemcp_discussion.md`: notes

## Gotchas
- Port 8080 must be free; mcp-quote-client expects server there
- No Docker

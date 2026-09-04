# research-agent-news-tool (MCP Server)

Minimal **MCP server**: exposes a `searchNews` tool over SSE. Companion to
`research-agent`, which connects to this server as an MCP client.

- `NewsSearchTools` (`@Tool searchNews`) — filters a hardcoded headline list
  by keyword; swap `NewsSearchService` for a real news API later without
  touching the tool contract.

## Run

```bash
./mvnw spring-boot:run
# or, if you don't have the wrapper: mvn spring-boot:run
```

Runs on **port 8090**. No API key needed — this is a plain MCP server, no
LLM involved.

## Verify it's up

```bash
curl -s -X POST http://localhost:8090/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

Should list `searchNews` in the response. In practice you won't call this
server directly — `research-agent` discovers and calls the tool for you.

## Stack

Java 17, Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-starter-mcp-server-webmvc`), Maven.

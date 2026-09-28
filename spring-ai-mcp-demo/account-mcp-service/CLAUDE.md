# account-mcp-service
MCP server (WebFlux/SSE) over JPA/H2 accounts. Tool: `getAccountsByPersonId(personId)`. Consumed by sibling `sample-client` (with `person-mcp-service`).

## Stack
Java 17, Spring Boot 4.1.1, Spring AI 2.0.1 (`spring-ai-starter-mcp-server-webflux`), Spring Data JPA, H2 in-mem, Maven.

## Commands
- `./mvnw spring-boot:run` ; `./mvnw test`
- Port 8040; MCP server name `account-mcp-server` v1.0.0
- No API keys, no LLM

## Layout (`com.example.accountmcpservice`)
`domain/Account`, `repository/AccountRepository`, `tool/AccountToolService` (@Tool), `config/ToolCallbackProviderConfig`
Tests: repository, tool service, ToolCallbackProviderConfig, context load.

## Gotchas
- H2 `jdbc:h2:mem:accountdb`, `ddl-auto=create-drop`, `defer-datasource-initialization=true` -> seed from `src/main/resources/import.sql`; data resets each run
- `io.modelcontextprotocol` logging at DEBUG
- Start before sample-client (it connects on boot)
- No Docker

# sample-client
MCP client + REST API. ChatClient (OpenRouter via OpenAI starter) calls tools on sibling `person-mcp-service` (:8060) and `account-mcp-service` (:8040).

## Stack
Java 17, Spring Boot 4.1.1, Spring AI 2.0.1 (mcp-client-webflux, openai starter), springdoc 3.1.0, Maven.

## Commands
- `./mvnw spring-boot:run` ; `./mvnw test`
- Start both MCP services first (SSE connections `person-mcp-server`, `account-mcp-server`)
- Port 8080
- Swagger via springdoc (ApiDocsAvailabilityTest exists)

## Endpoints
- `GET /persons/nationality/{nationality}`, `/persons/count-by-nationality/{nationality}`
- `GET /accounts/count-by-person-id/{personId}`, `/accounts/balance-by-person-id/{personId}`

## Config
- Env: `OPENROUTER_API_KEY` (empty default), `OPENROUTER_MODEL` (default openai/gpt-4o-mini)
- base-url https://openrouter.ai/api/v1

## Layout (`com.example.sampleclient`)
`web/` (PersonController, AccountController), `config/ChatClientConfig`, `prompt/PromptTemplates`
Tests: controller integration tests, ApiDocsAvailabilityTest, PromptTemplatesTest.

## Gotchas
- `io.modelcontextprotocol` logging at DEBUG
- No Docker

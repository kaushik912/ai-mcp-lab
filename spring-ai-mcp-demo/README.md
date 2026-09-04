# spring-ai-mcp-demo

Multi-service Spring AI + Model Context Protocol (MCP) system: `person-mcp-service`, `account-mcp-service`, `sample-client`.

Ticket-driven build via `/ticket-spec` — see `tickets/` and `.spec/`.

## Running the system

Start in this order (each is an independent Maven project):

```
mvn -f person-mcp-service/pom.xml spring-boot:run    # port 8060
mvn -f account-mcp-service/pom.xml spring-boot:run   # port 8040
export OPENROUTER_API_KEY=sk-or-...                  # required by sample-client
mvn -f sample-client/pom.xml spring-boot:run          # port 8080
```

Swagger UI: `http://localhost:8080/swagger-ui.html`. OpenAPI spec: `http://localhost:8080/v3/api-docs`.

`OPENROUTER_MODEL` overrides the default chat model (`openai/gpt-4o-mini`) used against
OpenRouter's OpenAI-compatible API.

## Sample requests

Against the seed data (`person-mcp-service`'s `import.sql`: persons 1–4, id 1 = John
Smith/American; `account-mcp-service`'s `import.sql`: id 1 has 2 accounts totaling
3500):

```
# person-mcp-server only
curl http://localhost:8080/persons/nationality/Polish
curl http://localhost:8080/persons/count-by-nationality/Polish

# account-mcp-server only
curl http://localhost:8080/accounts/count-by-person-id/1

# both servers — multi-tool orchestration
curl http://localhost:8080/accounts/balance-by-person-id/1
```

Expect roughly:

```
> /persons/nationality/Polish
Anna Kowalska and Piotr Nowak are the persons with Polish nationality.

> /persons/count-by-nationality/Polish
There are 2 persons from Polish nationality.

> /accounts/count-by-person-id/1
Person with ID 1 has 2 accounts.

> /accounts/balance-by-person-id/1
The person with ID 1 is John Smith, an American. He has a total of 2 accounts,
and the total balance on his accounts is $3,500.
```

Exact wording varies run to run (LLM-generated), but the facts above should always
be present. Other nationalities/ids to try: `American` (id 1), `Brazilian` (id 4),
person id `2` or `3` (1 account each).

## Running tests

```
make test          # person-mcp-service + account-mcp-service (unit/slice) + sample-client
                    # (sample-client's integration tests self-skip without a live setup)
```

`sample-client`'s integration tests (`PersonControllerIntegrationTest`,
`AccountControllerIntegrationTest`, `ApiDocsAvailabilityTest`) require
`person-mcp-service` and `account-mcp-service` already running (see above) and
`OPENROUTER_API_KEY` set — without both, they self-skip via
`@EnabledIfEnvironmentVariable` rather than failing. To run them:

```
# with person-mcp-service and account-mcp-service already running, and
# OPENROUTER_API_KEY exported:
make test-client
```

Per module directly: `mvn -f <service>/pom.xml test`.

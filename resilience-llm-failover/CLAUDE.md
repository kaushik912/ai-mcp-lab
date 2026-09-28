# resilience-llm-failover
POC: chat API routes to Gemini, fails over to OpenRouter via Resilience4j `@CircuitBreaker` (`geminiService`).

## Stack
Java 17, Spring Boot 3.5.3, Resilience4j 2.3.0, LangChain4j (google-ai-gemini 1.0.0-beta5, open-ai 1.0.0), springdoc 2.8.6. NOT Spring AI. Maven.

## Commands
- `./mvnw spring-boot:run` (README uses mvn); `./mvnw test`
- Port 8080. `POST /api/chat` body `{"prompt":"..."}` -> `{"response","provider":GEMINI|OPENROUTER}`
- Swagger UI `/swagger-ui.html`, spec `/v3/api-docs`
- README: no automated tests; manual verification via curl

## Config
- Env: `GEMINI_API_KEY`, `OPENROUTER_API_KEY` (empty default)
- `gemini.model-name` (yml: gemini-1.5-turbo), `openrouter.model-name` (openrouter/free), base-url https://openrouter.ai/api/v1
- CB: window 4, min calls 2, 50% failure, 10s open; records only `RateLimitException`

## Layout (`com.example.resiliencellmfailover`)
`config/ModelConfig` (beans geminiModel, openRouterModel), `controller/AiController`, `service/ResilientAiService`, `dto/` (ChatRequest, ChatResponse, ChatResult)

## Gotchas
- Fallback fires on ANY Gemini exception; only RateLimitException counts toward opening the circuit
- Force fallback: invalid GEMINI_API_KEY
- Also has `tickets/`, `.spec/` dirs (spec-driven workflow)
- No Docker

# resilience-llm-failover

POC: Spring Boot service that routes chat requests to Google Gemini by default and automatically
fails over to OpenRouter when Gemini is rate-limited, using Resilience4j's `@CircuitBreaker`.

## Running the POC

Set your API keys as environment variables:

```
export GEMINI_API_KEY=your-gemini-api-key
export OPENROUTER_API_KEY=your-openrouter-api-key
```

Start the service:

```
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

### Verify normal (Gemini) routing

```
curl -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"prompt":"hello"}'
```

Expected response:

```
{"response":"...","provider":"GEMINI"}
```

### Verify Swagger docs

Open `http://localhost:8080/swagger-ui.html` in a browser, or check the raw spec:

```
curl -i http://localhost:8080/v3/api-docs
```

Both should return `200`.

### Verify OpenRouter fallback

The circuit breaker (`geminiService`) opens after 2+ calls with a 50%+ rate-limit failure rate
(see `resilience4j.circuitbreaker.instances.geminiService` in `application.yml`). Two ways to trigger it:

1. **Real rate limit**: send requests fast enough to hit your Gemini plan's actual rate limit.
2. **Force it**: temporarily set an invalid `GEMINI_API_KEY` and re-run the `curl` command above a
   couple of times. The fallback method fires on *any* exception from the Gemini call (not just
   rate limits), so you'll see the response's `provider` switch to `"OPENROUTER"` immediately —
   check the app logs for `Primary model [Gemini] failed! ... Falling back to OpenRouter...`.
   Note: only `RateLimitException` counts toward the circuit breaker's failure-rate calculation
   (`recordExceptions`), so other error types trigger the fallback per-call without opening the
   circuit itself.

No automated test suite — this is a POC verified manually via the steps above.

---
status: approved
---

# Gemini → OpenRouter resilience failover with Swagger API

## Problem

LLM API calls to Google Gemini are subject to rate limiting (HTTP 429). When rate-limited, requests fail immediately instead of gracefully degrading to a secondary provider.

## Goal

Build a Spring Boot microservice with Resilience4j circuit breaker that:
- Routes chat requests to Google Gemini by default
- Automatically switches to OpenRouter when Gemini returns HTTP 429 or times out
- Exposes a REST endpoint with Swagger/OpenAPI documentation
- Tracks which provider handled each response

## Non-Goals

- Full health monitoring or automatic recovery probing
- Database persistence or chat history
- Authentication/authorization beyond API keys
- Cost optimization or load balancing across providers

## Acceptance Criteria

- POST /api/chat accepts a JSON prompt and returns LLM response via Gemini or OpenRouter
- When Gemini returns HTTP 429, the next request routes to OpenRouter (circuit opens)
- Response includes a header or field indicating which provider handled the request
- Swagger documentation is accessible at /swagger-ui.html
- Service starts cleanly with `mvn spring-boot:run`

## Stack Notes

Spring Boot 3.x, Resilience4j 2.2.0, LangChain4j for Gemini/OpenRouter clients. Use /spring-init to scaffold the base project.

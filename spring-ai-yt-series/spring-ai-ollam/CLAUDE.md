# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./mvnw clean install`
- Run: `./mvnw spring-boot:run` (starts on port 8081)
- Test: `./mvnw test`
- Single test: `./mvnw test -Dtest=SpringAiOllamApplicationTests#contextLoads`

## Runtime dependency

Requires a local Ollama server reachable at `http://localhost:11434` (see `spring.ai.ollama.base-url` in `src/main/resources/application.properties`), with the `codellama:latest` model pulled (`spring.ai.ollama.chat.options.model`). The app will fail to serve chat requests if Ollama isn't running locally with that model available.

## Architecture

Minimal Spring Boot + Spring AI (1.0.1) demo wiring a REST endpoint to a local Ollama LLM:

- `SpringAiOllamApplication` — standard `@SpringBootApplication` entry point, no custom config.
- `controller/ChatController` — single `GET /chat?q=...` endpoint. Builds a `ChatClient` from the auto-configured `ChatClient.Builder` (Spring AI's Ollama starter wires this based on `application.properties`), sends the query string as the prompt, and returns the raw text response. No conversation memory, system prompt, or streaming — each request is a stateless one-shot call.
- Model/connection config lives entirely in `application.properties`, not code — change model or Ollama host there.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and run (use the Maven wrapper, no local Maven install required):

```bash
./mvnw clean install       # build + run tests
./mvnw test                 # run tests only
./mvnw test -Dtest=ToolCallingExampleApplicationTests#getWeatherTest   # single test
./mvnw spring-boot:run       # run the app (listens on port 8181)
```

Requires `OPENAI_API_KEY` to be set in the environment (`src/main/resources/application.properties` reads it via `${OPENAI_API_KEY}`).

Java 25, Spring Boot 3.5.6, Spring AI 1.0.3.

## Architecture

This is a minimal Spring AI **tool-calling** (function-calling) demo. One HTTP entry point, `GET /chat?q=...` (`controller/ChatController.java`), forwards the query to `ChatService.chat(String)`.

`ChatService` builds a `ChatClient` prompt and explicitly registers tool instances per-call via `.tools(new SimpleDateTimeTool(), weatherTool)` — the LLM (gpt-4o-mini, configured in `application.properties`) decides whether/which tool to invoke based on each tool's `@Tool(description = ...)` annotation, and Spring AI handles the tool-call round-trip automatically.

Two tools under `tools/`:
- `SimpleDateTimeTool` — no dependencies, returns current date/time (`getCurrentDateTime`) and a stub `setAlarm` action tool. Instantiated fresh per chat call (not a Spring bean).
- `WeatherTool` — a `@Service` bean, injected with a `RestClient` bean (base URL `http://api.weatherapi.com/v1`, configured in `AiConfig`) and calls the WeatherAPI `/current.json` endpoint using `app.weather.api-key` from `application.properties`.

`AiConfig` defines the two beans the rest of the app depends on: the `ChatClient` (with `SimpleLoggerAdvisor` for request/response logging) and the `RestClient` used by `WeatherTool`.

Tool method parameters use `@ToolParam(description = ...)` so the model knows what to pass (e.g. city name, ISO-8601 time).

## Notes

- `app.weather.api-key` in `src/main/resources/application.properties` is a hardcoded WeatherAPI key checked into source (unlike the OpenAI key, which is env-var-based). Treat as a leaked secret if this repo is ever made public.

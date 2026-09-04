# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build: `./mvnw clean install`
Run: `./mvnw spring-boot:run` (requires `GOOGLE_KEY` env var set to a Gemini API key)
Test (all): `./mvnw test`
Test (single class): `./mvnw test -Dtest=BootGeminiAppApplicationTests`
Package: `./mvnw clean package`

App listens on port `8083`. Manual smoke test:
`curl "http://localhost:8083/ai?query=hello"`

## Architecture

Spring Boot 3.5.9 app on Java 25, using Spring AI 1.1.2 (`spring-ai-starter-model-google-genai`) to call Google Gemini (model `gemini-3-pro-preview`, configured in `application.properties`).

Despite the directory name (`src/main/kotlin`, `src/test/kotlin`), most source files are plain `.java` — only the application entry point (`BootGeminiAppApplication.kt`) and the test class are actual Kotlin. `pom.xml` wires both `kotlin-maven-plugin` and Java compilation to the same `src/main/kotlin` / `src/test/kotlin` source roots, so mixed Kotlin/Java compilation in this one directory tree is intentional/expected, not a misconfiguration — don't "fix" it by moving files to `src/main/java`.

Request flow: `AiController` (`@RestController`, `GET /ai?query=...`) → `AiService.getResponseFromAI(String)` → Spring AI `ChatClient` (built from injected `ChatClient.Builder`, auto-configured against the Gemini starter) → returns plain-text model response wrapped in `ResponseEntity`.

The Gemini API key is read from env var `GOOGLE_KEY` via `spring.ai.google.genai.api-key=${GOOGLE_KEY}` — must be exported before running.

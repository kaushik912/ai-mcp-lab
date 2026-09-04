# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build/run from this module's directory (`spring_ai_media/`):

```bash
./mvnw clean install        # build
./mvnw spring-boot:run       # run (starts on port 8083)
./mvnw test                  # run all tests
./mvnw test -Dtest=SpringAiMediaApplicationTests#contextLoads   # run a single test
```

Requires `OPEN_AI_KEY` env var (or `.env`, currently empty) — bound to `spring.ai.openai.api-key` in `application.properties`. Java 25.

## Architecture

Single-module Spring Boot 3.5.9-SNAPSHOT app demonstrating Spring AI's audio/transcription support via `spring-ai-starter-model-openai` (BOM version 1.1.1). Despite "media" in the name, this module covers **audio transcription (speech-to-text)**, not text-to-speech.

- `AudioController` (`/api/v1/audio`) — three POST endpoints, all delegating to `AudioService`:
  - `/text` — accepts an uploaded `MultipartFile` and transcribes it.
  - `/transcript` — transcribes a fixed classpath resource (`sample2.m4a`) injected via `@Value("${classpath:sample2.m4a}")`.
  - `/transcript-with-options` — transcribes `sample.m4a` using `OpenAiAudioTranscriptionOptions` (language, temperature, prompt).
- `AudioService` wraps Spring AI's `TranscriptionModel` (auto-configured from the OpenAI starter based on `spring.ai.openai.api-key`), exposing `convertAudioToText(Resource)` and `convertAudioToTextWithOptions(Resource)`.
- Sample audio fixtures (`sample.m4a`, `sample2.m4a`) live in `src/main/resources/` and are referenced directly by the controller via classpath `@Value` injection — not loaded through `AudioService`.

No persistence layer, no other AI model types wired up in this module.

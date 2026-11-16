# quiz-generator
Topic + difficulty -> 10 dynamic single-correct MCQs (CONCEPT / SCENARIO) from an LLM via OpenRouter. Learning project: keep it simple. Spec: `spec.md`; deploy: `deploy.md`.

## Stack
Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, Maven. Uses the plain `spring-ai-openai` library, NOT the starter (starter autoconfig requires a server-side key and fails at boot; we are BYOK). Jackson 3 (`tools.jackson.databind.json.JsonMapper`).

## Commands
- `./mvnw spring-boot:run` -> http://localhost:8080 (port from `PORT`, default 8080)
- `make` lists shortcuts: `make run`, `make test`, `make e2e-install`, `make e2e`, `make e2e-headed`, `make e2e-report`, `make check`
- `./mvnw test` (16 tests; no network or key needed)
- Only endpoint: `POST /api/quiz`, header `X-OpenRouter-Key`, body `{topic, scope?, difficulty: EASY|MEDIUM|HARD, model?}` -> `{topic, questions[10]}` incl. `correctIndex` + `explanation`. `scope` = optional hint (max 400 chars) listing areas of the topic

## Layout (`com.example.quizgenerator`)
- `QuizController`: header check (401), maps `QuizException` to `{error}` + status
- `QuizGenerator`: plan -> prompt -> call -> parse -> validate loop, max `quiz.max-attempts` (3), then 502; shuffles options at the end
- `BlueprintBuilder`: 10 `Slot`s per request (3-5 SCENARIO, rest CONCEPT; distinct random angles)
- `QuizPromptBuilder`: prompt text; appends validator errors on retry
- `QuizValidator`: strict format rules, returns error strings
- `QuizLlm` (seam) / `OpenRouterLlm`: builds a fresh `OpenAiChatModel` per call with the caller's key, base-url `https://openrouter.ai/api/v1`
- `static/`: `index.html`, `style.css`, `app.js` (entry: tabs + wiring) and `js/` ES modules (`<script type="module">`, vanilla, no libs/CDN), one responsibility each:
  - `dom.js` helpers (`$`, `el`, option row, mark answer)
  - `favorites-store.js` data layer: validation (`clean`), id hash, localStorage, `subscribe()`; no DOM
  - `notices.js` message + storage-unavailable banners
  - `topics.js` topic picker: reads `static/topics.json` (6 umbrella `{label, hint}` entries + "Other"/free text); curated pick sends label as `topic` and hint as `scope`; falls back to free text if the JSON fails to load
  - `quiz-view.js` generate form, render, grading, star buttons
  - `saved-view.js` Saved tab: filter, reveal-to-practice cards, remove
  - `favorites-io.js` export/import JSON
  - Views react to the store via `subscribe()`; `app.js` calls `store.init()` last so every subscriber gets the first notification

## Rules to keep
- Never store, log or echo the API key (not in logs, errors, exceptions). Server has no LLM key.
- UI keeps the API key in page memory only: never in localStorage/sessionStorage/cookies, never in the favorites export. Render model/imported text via `textContent`, never `innerHTML`. No third-party/CDN scripts.
- Favorites (starred questions) are client-only: localStorage `quiz.favorites.v1` (max 200) + Export/Import JSON. No server endpoint or DB. Import is untrusted: `clean()` in `favorites-store.js` validates every item and recomputes `id`; all storage access in try/catch with in-memory fallback. See `spec.md` "Favorites".
- Retry only format failures. Provider errors (401/402/429, IO) fail fast, mapped in `QuizGenerator.map`.
- Questions test concepts/trade-offs, not annotation/syntax/version trivia (prompt rule). Short code-reasoning (what does this print, object counts, equality) IS allowed: it goes in a SCENARIO's `scenario` (max ~8 lines, `\n` breaks); UI shows scenario with `pre-wrap`, monospace when multi-line. No separate `code` field.
- `topic` (100) and `scope` (400) are untrusted: length-capped, delimited in the prompt (`<topic>`, `<scope>`). The `<scope>` mention and the coverage rule (spread across areas, none more than twice) appear only when a scope is sent.
- To change topics edit `static/topics.json` only (max ~6 labels, hint <= 400 chars each).
- Grading is client-side by design; no token/grade endpoint.

## Config (`application.yml`)
`quiz.model` (google/gemini-2.5-flash), `quiz.max-attempts` 3, `quiz.temperature` 0.9, `quiz.timeout` 90s, `quiz.base-url`.

## Gotchas
- Dev sandbox blocks openrouter.ai (proxy 403), so a real model call is unverified; tests use a fake `QuizLlm`.
- Provider error mapping matches on status codes in the exception message (`401`/`402`/`429`); revisit if Spring AI changes message format.
- Not yet built/tested: Docker image, Cloud Run deploy.
- Browser tests are Playwright for **Node** in `e2e/` (separate from Maven). `make e2e-install` once, then `make e2e`; it starts the app itself on port 8099 (`E2E_PORT`), never reuses a running server, and fakes `POST /api/quiz` in the browser (no key/network). Only `e2e/tests/api.spec.js` hits the real server. See `e2e/README.md`. Run `make check` for Java + browser tests.
- Keep e2e tests short, one user story each, sentence-style names, shared code in `e2e/tests/helpers.js` (the owner finds Playwright hard to read).
- UI changes need an e2e test update; a deliberately broken change should make a test fail (verified once with the 200-favorites cap).
- `spring-boot:run` serves `target/classes`: restart the app after editing `static/` files.
- Idea later: Firebase sync for favorites.

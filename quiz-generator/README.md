# quiz-generator
Topic + difficulty -> 10 dynamic MCQs (CONCEPT / SCENARIO) from an LLM via OpenRouter. Spec: `spec.md`.

- Stack: Java 21, Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-openai` library, no starter: BYOK means no server key/autoconfig). Maven.
- BYOK: user's OpenRouter key goes in the `X-OpenRouter-Key` header per request; never stored or logged. UI keeps it in page memory only.
- Run: `./mvnw spring-boot:run` then open http://localhost:8080 . Test: `./mvnw test`
- API: `POST /api/quiz` `{topic, difficulty: EASY|MEDIUM|HARD, model?}` -> `{topic, questions[10]}` (includes `correctIndex`, `explanation`; grading is in the browser).
- Reliability: reply is parsed and strictly validated (10 questions, kind per slot, 4 distinct options, valid index, unique subtopics); on failure re-prompts with the errors, max 3 attempts, then 502. Provider errors (401/402/429) are not retried.
- Variety: server plans 10 slots per request (random CONCEPT/SCENARIO mix, distinct angles); options are shuffled server-side.
- Config (`application.yml`): `quiz.model` (default `google/gemini-2.5-flash`), `quiz.max-attempts`, `quiz.temperature`, `quiz.timeout`, `quiz.base-url`.
- Deploy: `Dockerfile` for Cloud Run (reads `PORT`), steps in `deploy.md`. No env vars required.
- Not verified here: a real model call (sandbox could not reach OpenRouter).

# quiz-generator
Topic + difficulty -> 10 dynamic MCQs (CONCEPT / SCENARIO) from an LLM via OpenRouter. Spec: `spec.md`.

- Stack: Java 21, Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-openai` library, no starter: BYOK means no server key/autoconfig). Maven.
- BYOK: user's OpenRouter key goes in the `X-OpenRouter-Key` header per request; never stored or logged. UI keeps it in page memory only.
- Run: `./mvnw spring-boot:run` then open http://localhost:8080 . Java tests: `./mvnw test`
- `make` lists shortcuts. Browser tests (Playwright, Node): `make e2e-install` once, then `make e2e` (uses port 8099, fakes the AI, never touches 8080). `make check` = both suites. See `e2e/README.md`.
- API: `POST /api/quiz` `{topic, difficulty: EASY|MEDIUM|HARD, model?}` -> `{topic, questions[10]}` (includes `correctIndex`, `explanation`; grading is in the browser).
- Reliability: reply is parsed and strictly validated (10 questions, kind per slot, 4 distinct options, valid index, unique subtopics); on failure re-prompts with the errors, max 3 attempts, then 502. Provider errors (401/402/429) are not retried.
- Variety: server plans 10 slots per request (random CONCEPT/SCENARIO mix, distinct angles); options are shuffled server-side.
- Config (`application.yml`): `quiz.model` (default `deepseek/deepseek-v4.1-flash`), `quiz.max-attempts`, `quiz.temperature`, `quiz.timeout`, `quiz.base-url`.
- Topics: pick one of 6 umbrella labels (Java, Spring and JPA, Databases, System design, Web and security, DevOps and cloud) or "Other" and type your own. Each label has a hint in `static/topics.json` that is sent as `scope` to steer the model across the area's subtopics. Questions may include short code snippets ("what does this print?").
- Favorites: tap ☆ to save a question. Saved tab = practice mode (reveal answer on tap), topic filter, Export/Import `favorites.json`. Stored in browser localStorage only (per device; browsers may evict it, so Export for backup). No server storage.
- Deploy: `Dockerfile` for Cloud Run (reads `PORT`), steps in `deploy.md`. No env vars required.
- Not verified here: a real model call (sandbox could not reach OpenRouter).

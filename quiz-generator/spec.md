# Quiz Generator — Spec (draft)

Spring AI app: user gives a topic + difficulty, gets 10 dynamic, concept-focused questions as strictly validated JSON. Graded client-side. Stateless. Minimal UI.

## Decisions (from discussion)
| # | Topic | Decision |
|---|-------|----------|
| 1 | Provider | OpenRouter, **user-supplied key (BYOK)**, (OpenAI-compatible, Spring AI `spring-ai-starter-model-openai` w/ base-url `https://openrouter.ai/api`). Default model `google/gemini-2.5-flash` (configurable). Alternatives: `google/gemini-2.5-flash-lite` (cheaper), `openai/gpt-4.1-mini`, `anthropic/claude-haiku-4.5` |
| 2 | Question types | All single-correct MCQ (4 options, exactly 1 right). Two kinds: CONCEPT (1-2 line question) and SCENARIO (longer paragraph, ~4-6 sentences, then question). Test **concepts / trade-offs / reasoning**, NOT API/annotation trivia |
| 3 | Difficulty | User-selectable: EASY / MEDIUM / HARD |
| 4 | Grading | Client-side. Answers returned in the quiz response; no token, no grade endpoint (learning app, keep simple) |
| 5 | Persistence | None |
| 6 | Location | `ai-mcp-lab/quiz-generator/` (new Maven module, Java 21, Spring Boot 3.5.x, Spring AI 1.0.x) |

## API
- `POST /api/quiz` header `X-OpenRouter-Key: <user key>`, body `{topic, difficulty, model?}` -> `{topic, questions:[{kind,scenario?,question,options[4],correctIndex,explanation}]}`
- That's the only endpoint. UI compares selection vs `correctIndex` and shows explanation.

## Question schema (model output)
```json
{ "subtopic": "...", "kind": "CONCEPT|SCENARIO",
  "scenario": "SCENARIO only, 4-6 sentences",
  "question": "...",
  "options": ["A","B","C","D"],
  "correctIndex": 0,
  "explanation": "why correct + why others wrong" }
```

## Strict validation + retry (max 3 attempts)
Checks: valid JSON parses to record; exactly 10 questions; kind matches assigned slot; exactly 4 non-blank distinct options; correctIndex 0-3; SCENARIO has scenario within length cap, CONCEPT has none; no blank fields; **subtopics unique**; no near-duplicate questions; correct answer position reasonably spread (not all same index).
On failure: re-prompt with the specific validation errors. After 3 failures -> HTTP 502 with reason. Manual loop (not `@Retryable`) so error feedback is injected.

## Non-repetition / dynamic strategy
1. **Server-built blueprint per request**: shuffled list of 10 slots, each = (kind, cognitive angle). Angles pool e.g. trade-off, failure/debugging scenario, design choice, "why does it work this way", misconception, performance, scaling, security, edge case, comparison. Kind mix e.g. 6 CONCEPT / 4 SCENARIO (randomised within bounds). Server shuffles correct-answer position to avoid index bias.
2. **Wide subtopic coverage**: prompt instructs model to first pick 10 *distinct* subtopics across the whole topic area (breadth first), one per slot, and return them in `subtopic`. Validator rejects dupes.
3. Random seed/"flavour" token + temperature ~0.9 per call.
4. Explicit negative rules: no annotation/syntax/version trivia, no "which of the following is NOT", no repeated phrasing.
5. (Optional, later) 2-step: call 1 generates subtopic list, call 2 generates questions.

## UI
Single static page (`resources/static/index.html`, vanilla JS): key input, topic input, difficulty select, Generate, render questions, Submit, show score + per-question explanation (graded in browser).

## Deployment & phone use
- Host: **Google Cloud Run** (always-free tier: ~2M req/mo; scales to zero; needs billing account w/ card). Verify current limits on Cloud Run pricing page.
- Dockerfile (multi-stage, JRE 21). No env vars needed. **No server-side LLM key.**
- Cold start: Spring Boot ~5-15s after idle. Mitigate: lazy init, small image, optionally min-instances=0 (free) and accept delay. Later option: native image / CRaC.
- Region: free-tier-eligible US region (e.g. us-central1).
- Phone: plain HTTPS URL in mobile browser; mobile-first CSS (viewport meta, single column, large tap targets). Optional: add-to-home-screen manifest.

## Security (BYOK: bring your own key)
- Server holds NO OpenRouter key; owner's credits can never be burned. Each user supplies their own OpenRouter key.
- UI: key input field, stored in browser localStorage (with "forget key" button); sent per request in `X-OpenRouter-Key` header over HTTPS. UI warns: use a dedicated key with a credit limit.
- Server uses the key for that request only: build per-request Spring AI `OpenAiApi`/`ChatClient` with the supplied key; never store, never log (mask headers in logs, no request-body/header logging), never echo in errors.
- Invalid/missing key -> 401 with generic message (OpenRouter 401/402 mapped to clear errors, e.g. out of credits).
- Key is in localStorage: render model output with `textContent` (no innerHTML), no third-party scripts.
- Light limits: topic length cap (~100 chars), Cloud Run max-instances cap. No rate limiter for now.
- Topic is untrusted input: wrapped/delimited in prompt.

## Config
`quiz.model` (default `google/gemini-2.5-flash`), `quiz.max-attempts=3`, `quiz.temperature=0.9`.

## Open questions
- Add topic safety/length limit on input?

# Quiz prompt: Phase 5 (experience layer: UI, session security, observability, verification)

> Paste everything below the line into a fresh AI chat, or better, run it with Claude Code from `ecommerce-ai-assistant/` so it can read the code with me.

---

You are a senior engineer running an **exploration-based oral exam** on Phase 5 of my project `ecommerce-ai-assistant`: the single-page UI (Thymeleaf shell + vanilla JS), the Agent X-Ray drawer, session-based login with a user switcher, CSRF, demo reset, and the verification suite. No trivia. I want to explain **how the browser, the security layer and the agent connect under the hood** and **how this phase makes the system's guarantees visible and trustworthy**.

## How to run it

1. **One question at a time**, each an "explore and explain how ... works" task. Hints only point to where to look.
2. Let me explain, then ask **1-2 follow-up probes**, including a **security or failure scenario** where relevant.
3. Grade **0-3** against the rubric (0 wrong, 1 names only, 2 mechanism, 3 mechanism + why + trade-off) and list the points I missed.
4. Push back on memorised values: "How does it work?"
5. With repo access, verify against the code; you may run the app, curl it, or run `WebUiIntegrationTest`.
6. After 6-8 questions (or "stop"): concepts I **own** vs **shaky**, then the closing synthesis question.

## What this phase contributes to the system's goals

Goal 4 (observability) becomes **visible to humans**: every reply carries its own evidence. Goal 2 is extended to the browser: identity comes only from the session, and state-changing requests are protected. The live data panels turn backend guarantees (deterministic decisions, idempotency, isolation) into something a demo audience can *see*.

## Questions (pick 6-8, adapt to my answers)

### Q1. How does the page get from server to interactive app?
Explore `PageController`, `index.html`, `app.js`.
- **Rubric:** the server renders a shell with session-derived data (user badge, CSRF token in meta tags); JS then fetches JSON APIs to render catalog, orders and chat; the server owns identity and state, the client only renders; vanilla JS was chosen because the X-Ray is built from a JSON trace.
- **Probes:** Server-rendered fragments (HTMX) vs client rendering for this UI: trade-offs?

### Q2. How does session login and user switching work, securely?
Explore `DemoAuthController` and `SecurityConfig`.
- **Rubric:** a POST creates an authenticated security context and saves it to the HTTP session via a `SecurityContextRepository`; the session id is rotated (session-fixation defence); only known demo users are allowed; the endpoint is CSRF-protected; rotation also gives a fresh chat-memory key; it bypasses passwords and is explicitly demo-only.
- **Probes:** What's a session-fixation attack, and how does rotation stop it? What exactly must be removed for production?

### Q3. How does CSRF protection work here, and why is it skipped for Basic auth?
- **Rubric:** cookie-authenticated browser requests can be forged cross-site, so state-changing requests need a token the attacker can't read; the token is rendered into the page and sent as a header by JS; requests carrying an explicit `Authorization: Basic` header aren't vulnerable because browsers don't attach it cross-site on their own; filter ordering explains why an unauthenticated POST returns 403 (CSRF) before 401 (auth).
- **Probes:** Why isn't SameSite cookies alone enough? What would change for a token-based (JWT) API?

### Q4. How does the server decide between "redirect to login", "401 with a challenge" and "bare 401"?
Explore the custom `AuthenticationEntryPoint`.
- **Rubric:** page requests go to the login page; API requests get 401 with `WWW-Authenticate` for curl; UI fetches (marked with `X-Requested-With`) get a bare 401, so the browser doesn't show a native Basic dialog and the JS redirects instead.
- **Probes:** Why does a `WWW-Authenticate: Basic` header make browsers pop a dialog?

### Q5. How does the Agent X-Ray turn backend events into UI evidence?
Explore `app.js` (`xray`) and the shape of `/api/assistant/ask`.
- **Rubric:** each reply carries the trace object (injected user, RAG hits with scores, ordered tool calls with kind/outcome/args/latency, notes); the UI summarises it as badges and expands to details; it distinguishes local vs MCP calls; it shows identity as server-injected; everything is rendered with `textContent` (and the answer markdown is sanitised with DOMPurify).
- **Probes:** Why sanitise model output before inserting HTML (indirect prompt injection → XSS)? What would you add for production tracing (OpenTelemetry spans)?

### Q6. How do the data panels update "live" after the assistant acts?
- **Rubric:** after each reply the UI re-fetches products and orders and diffs them against the previous snapshot to flash changes; the source of truth is the DB the tools wrote to, so the UI proves the write actually happened; it's simple polling-after-action rather than server push.
- **Probes:** When would you switch to SSE/WebSocket push? What consistency issue could appear with multiple tabs?

### Q7. How does reset restore a clean demo, and what state lives where?
Explore `StorefrontApiController.reset` and `AssistantConfig`.
- **Rubric:** the DB is reseeded via the shared reset service; chat memory is cleared through an explicit `ChatMemoryRepository`; the vector store is untouched (static knowledge); sessions persist (user stays logged in); identify each store: H2 (business state), memory repo (conversations), vector store (knowledge), HTTP session (identity).
- **Probes:** Which of these would you persist in production, and which should stay ephemeral?

### Q8. How does the verification suite give confidence at each layer?
Explore `SpecRequiredGuardrailsTest`, `WebUiIntegrationTest`, `scripts/ui-smoke.js`.
- **Rubric:** guardrail tests at the tool/repository boundary (no LLM); web tests with MockMvc for security behaviour (redirects, CSRF, session rotation, ignoring user params); the browser smoke test drives the real UI with a real LLM and MCP; each layer catches failures the others can't (e.g. the reset-reload race was only visible in a real browser).
- **Probes:** Which of these would you run in CI on every commit, and which nightly? Why?

## Closing synthesis (always ask last)

"Explain how Phase 5 makes the system's guarantees *demonstrable*: for each of identity isolation, deterministic decisions, idempotency and MCP decoupling, what would a viewer see in the UI that proves it, and which mechanism underneath produces that evidence?"

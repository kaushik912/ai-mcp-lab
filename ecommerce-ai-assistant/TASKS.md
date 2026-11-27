# ecommerce-ai-assistant: progress & resume notes

**Full requirements per phase: see [spec/README.md](spec/README.md)** (original prompt verbatim in [spec/99-original-prompt.md](spec/99-original-prompt.md)). This file tracks status only.

**Verification guides per phase** (tests + curl, all commands run and verified): [docs/HOW_TO_TEST_PHASE1.md](docs/HOW_TO_TEST_PHASE1.md) · [2](docs/HOW_TO_TEST_PHASE2.md) · [3](docs/HOW_TO_TEST_PHASE3.md) · [4](docs/HOW_TO_TEST_PHASE4.md) · [5](docs/HOW_TO_TEST_PHASE5.md). Run/demo: [README.md](README.md).

**Self-quiz prompts per phase** (paste into an AI chat; one question at a time, graded, with answer key): [docs/quiz_phase_1_prompt.md](docs/quiz_phase_1_prompt.md) · [2](docs/quiz_phase_2_prompt.md) · [3](docs/quiz_phase_3_prompt.md) · [4](docs/quiz_phase_4_prompt.md) · [5](docs/quiz_phase_5_prompt.md). Each new phase must add its own `docs/HOW_TO_TEST_PHASE<X>.md`.

Branch: `ccr-8e701508-pkxqcw` (no PR yet). Root: `ecommerce-ai-assistant/`.
Build: `mvn -B verify` from this dir. Java 21, Boot 4.1.3, Spring AI 2.1.0.

## Rules (from original brief; apply to all phases)
1. Never expose `userId` in any `@Tool`/`@McpTool` schema; inject via SecurityContextHolder / `ToolContext`. All queries scoped `WHERE user_id = :authenticatedUserId`.
2. Policy enforced in Java (day diff vs `deliveredAt`); return codes `APPROVED`, `REJECTED_OUTSIDE_WINDOW`, `REJECTED_NON_RETURNABLE`, `ELIGIBLE_FOR_EXCHANGE_CREDIT`.
3. `initiateReturn` / `issueExchangeVoucher` idempotent per order item.
4. Work strictly in phases: verify, summarise, ask user to confirm before next phase.

## Decisions / gotchas
- LLM: OpenRouter ONLY (`OPENROUTER_API_KEY`). Chat `deepseek/deepseek-v4.1-flash` (user-chosen after comparing with v4-flash-latest and qwen3.7-flash; all passed RAG tests; v4.1 most thorough, cites POL ids; env `OPENROUTER_MODEL` overrides), embeddings `openai/text-embedding-3-small`. Base URL `https://openrouter.ai/api/v1`. Spring AI 2.x props have NO `.options` segment. Mirrors `spring-ai-basics`. DeepSeek tool calling untested; swap model if flaky.
- Spring Boot 4 starter names: `spring-boot-starter-webmvc`, etc.
- `ORD-1002` seeded `boxOpened=true` so opened-earbuds rule is DB-driven. Hydration pack is `APPAREL`. ThermoMerino has stock 0.
- Order/item repositories only have owner-scoped queries (no unscoped orderId lookup). Keep it that way.
- Security (Phase 5): browser = session login via `POST /demo/login/{alex|priya}` (demo-only, no password, CSRF-protected, session id rotated); curl = HTTP Basic alex/demo, priya/demo. CSRF skipped only when an `Authorization: Basic` header is present, so unauthenticated POSTs get 403 (not 401); use GET for 401 checks. UI fetches send `X-Requested-With` -> bare 401 (no Basic popup).
- DECIDED (user): this is a learning project, not production. Keep the password-less demo switcher and reset endpoint as they are; don't "harden" them unless asked.
- Emoji render as boxes in headless WSL Chromium (no emoji font); fine in desktop browsers.
- `pkill -f <jar>` from a Bash call kills the shell itself (pattern in its own cmdline): stop apps by port pid instead.
- `DemoDataService.reset()` = reset-demo-data hook for Phase 5.
- MCP tracking id for delayed order: `TRK-BLR-77412`. Mock data is deterministic.
- Spring AI 2.1 artifact is `spring-ai-vector-store-advisor` (NOT `spring-ai-advisors-vector-store`). Check BOM when an artifact "has no version".
- Policy decisions Phase 4 Java must mirror: grace (31-45d, 80%/90% VIP) applies to APPAREL+FOOTWEAR only; electronics 14d hard stop; opened in-ear audio only if defective; opened hygiene final sale, unopened hygiene 14d; after 45d nothing. Late coupon: >24h past `promisedDeliveryAt`.
- `RETRIEVED_DOCUMENTS` from `QuestionAnswerAdvisor` in `ChatClientResponse.context()` = source for trace drawer RAG chunks + scores.
- `@SpringBootTest`s that don't need RAG set `app.rag.ingest-on-startup=false` (avoid paid calls). Live tests named `*Test` + `@EnabledIfEnvironmentVariable(OPENROUTER_API_KEY)` (surefire skips `*IT`).
- Local dev machine: user edits via Write/Edit only (no Bash file edits) per their CLAUDE.md.
- Chat API: `POST /api/assistant/ask` (text body, basic auth, session cookie keys memory) -> `{answer, ragChunks, trace}`; `DELETE /api/assistant/conversation`. Phase 5 UI calls these; clear memory on user switch.
- MCP tool names arrive unprefixed (`trackShipment` etc.). Carrier down -> trace note, local tools still work.
- Live scenario test needs carrier running: `java -jar carrier-mcp-server/target/*.jar` (after `mvn install -DskipTests`).
- Scenario 6 (injection): model refuses or calls getMyOrders (alex-only); never returns priya data. Scenario 3: model asks about defect first.
- DECIDED (user): approved returns restock `Product.stockCount` (+quantity) on NEW approval only (replay doesn't). Implemented in `ReturnService`; test `approvedReturnRestocksExactlyOnce`. Phase 5 catalog grid should refresh after chat replies to show it.

## Status
- [x] Phase 1 ([spec](spec/01-phase1-scaffolding-domain-seed.md)): scaffolding, entities, owner-scoped repos, seeder (2 users, 8 products, 6 orders), tests (4 pass)
- [x] Phase 2 ([spec](spec/02-phase2-carrier-mcp-server.md)): carrier-mcp-server (:8081, `/mcp`, STREAMABLE): trackShipment, generateReturnShippingLabel, findNearbyDropoffPoints, request logging, 5 tests incl. real handshake
- [x] Phase 3 ([spec](spec/03-phase3-rag-and-search.md)): policy doc (POL-1..9) + product guides (8 SKUs), split per `##` section -> 17 chunks; `SimpleVectorStore` ingest at startup (skipped without key); `QuestionAnswerAdvisor` (topK 4, threshold 0.3) on `assistantChatClient`; `AssistantService` returns answer + retrieved chunks/scores; temp `POST /api/assistant/ask`. Tests: 3 offline + 5 live (OpenRouter embeddings + DeepSeek verified working).
- [x] Phase 4 ([spec](spec/04-phase4-tools-security-mcp-client.md)): MCP client (lazy, `initialized: false`); `ReturnPolicy` (pure rules) + `ReturnService` (owner-scoped, row-locked idempotent writes); tools `searchProducts`, `getMyOrders`, `getMyProfile`, `initiateReturn`, `issueExchangeVoucher` (identity via `ToolContext` only); advisor chain trace -> memory (`username:sessionId`) -> RAG -> tool loop (cap 8); `AgentTraceAdvisor` + `TracingToolCallback` (LOCAL vs MCP, args, ms, outcome; MCP gets no ToolContext). Tests: 19 policy + 10 tool boundary + 6 live scenarios (all pass, MCP ones skip when carrier down).
- [x] Phase 5 ([spec](spec/05-phase5-ui-and-verification.md)): Thymeleaf shell + vanilla JS + Tailwind CDN (`templates/index.html`, `login.html`, `static/js/app.js`): top bar (badge, 1-click switcher, reset, sign out), 70% catalog (filters, live stock, flash on change) + live orders table (ages, DELAYED, coupon flag, RMA/voucher badges), 30% chat (6 chips, markdown via marked+DOMPurify, collapsible Agent X-Ray). Security: session login via `DemoAuthController` (session id rotated), CSRF on (skipped only for Basic auth), API 401 / page redirect. APIs: `/api/me`, `/api/products`, `/api/orders`, `POST /api/demo/reset` (reseed + clear all chat memory). Tests: `SpecRequiredGuardrailsTest` (3, brief's named tests) + `WebUiIntegrationTest` (11). `README.md` (run + 3-min demo script). `scripts/ui-smoke.js` (Playwright, verified). Full `mvn verify`: 67 tests green.

## Resume
Read this file, the matching `spec/` phase file, `spec/06-demo-scenarios.md`, `git log`, then ask the user to confirm and start the next unchecked phase. Update checkboxes + commit/push at the end of each phase.

# Phase 5: Demo UI & verification suite (DONE)

## As built
- UI: Thymeleaf shell + vanilla JS (chosen over HTMX: the X-Ray drawer is rendered from the chat JSON) + Tailwind Play CDN 3.4.16; marked 12.0.2 + DOMPurify 3.1.6 (cdnjs) for safe markdown answers.
- Top bar: badge `Alex - VIP` / `Priya - Standard` (server-rendered), Alex|Priya switcher (POST `/demo/login/{u}`), Reset Demo Data, Sign out.
- Left 70%: catalog (category chips, live stock, "+1" flash), My Orders (age text, DELAYED + hours late, ₹500 coupon flag, RMA / voucher badges, row flash). Right 30%: chat, 6 chips (exact brief prompts), per-reply collapsible Agent X-Ray (badges RAG/Local/MCP/User/ms; identity box, RAG chunks with score bars, ordered tool calls with kind/outcome/args/ms, notes).
- Live updates: after each reply the UI re-fetches `/api/products` and `/api/orders`.
- Security: session login with session-id rotation, CSRF on (skipped only for Basic auth), page -> /login redirect, API -> 401, UI fetch -> bare 401. Reset clears all chat memory (`ChatMemoryRepository`).
- Tests: `SpecRequiredGuardrailsTest` (the brief's 3), `WebUiIntegrationTest` (11). Browser smoke: `scripts/ui-smoke.js`.
- `README.md`: architecture sketch, run commands, env vars, 3-minute demo script, guardrail map.

## Requirements: UI
Single-page **Storefront + AI Concierge**: Thymeleaf + HTMX (or vanilla JS) + Tailwind (CDN).
- **Top bar:** logged-in user badge (`Alex - VIP` / `Priya - Standard`), **1-click user switcher** (session/security isolation demo), **"Reset Demo Data"** button.
- **Left/main panel (70%):**
  - Product catalog grid, live stock counts (update dynamically after actions), category filters.
  - "My Orders" live table: order ids, delivery age (`4 days ago`, `36 days ago`, `DELAYED`), live return/RMA badges updating immediately after the chatbot processes a return/voucher.
- **Right panel (30%): AI assistant + "X-Ray Trace Drawer":**
  - Chat window with **1-click demo prompt chips** (see [06-demo-scenarios.md](06-demo-scenarios.md)).
  - Collapsible **Agent X-Ray / Trace** under each bot reply with badges `[RAG Docs Used]`, `[Local @Tool]`, `[Remote MCP Tool]`, `[Server-Injected User: alex]`.

## Requirements: JUnit 5 integration suite
- Out-of-window return (`ORD-1003`) rejected by `OrderAndReturnTools` regardless of LLM phrasing.
- Cross-user access (`alex` querying priya's `ORD-2001`) returns/throws not-found at repository/tool boundary.
- Idempotency: `initiateReturn` twice for `ORD-1001` yields the exact same RMA id.

## Requirements: README.md
- Exact startup commands for both services.
- 3-minute live demo script.

## Notes
- Replace the Phase 1 HTTP-basic baseline with form/session auth + switcher (POST switch endpoint that re-authenticates the session; clear chat memory/trace on switch). Re-enable CSRF with HTMX token headers.
- Chat endpoint must derive identity from the security context only; never accept a user id from the browser.
- Catalog stock count: decide whether returns restock (UI "live stock counts" should visibly change on some action).
- Trace delivery: simplest = return trace JSON with each reply; optional SSE for real-time streaming.
- Document `OPENROUTER_API_KEY` in README; tests that hit the model are env-gated.

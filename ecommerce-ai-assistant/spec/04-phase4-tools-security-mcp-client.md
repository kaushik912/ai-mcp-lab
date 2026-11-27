# Phase 4: Local `@Tool` beans, identity security & MCP client wiring (DONE)

## As built
- `returns/ReturnPolicy`: pure rules mirroring POL-1..7 (windows, in-ear hygiene via `IN_EAR_AUDIO_SKUS`, hygiene final sale, 31-45 grace 80/90%, lateness >24h). Whole-day counts.
- `returns/ReturnService`: owner-scoped reads; `lockByOrderAndSkuForUser` (PESSIMISTIC_WRITE) for writes; idempotent RMA (`OrderItem.rmaId`) and voucher (`OrderItem.voucherCode/voucherValue`); other users' orders -> `NOT_FOUND` identical to missing. Item resolution accepts SKU, product name, or none (single-item order).
- `ReturnDecision`: status (`DecisionStatus`: APPROVED, REJECTED_OUTSIDE_WINDOW, REJECTED_NON_RETURNABLE, ELIGIBLE_FOR_EXCHANGE_CREDIT, VOUCHER_ISSUED, NOT_ELIGIBLE_FOR_VOUCHER, NOT_DELIVERED, NOT_FOUND), days, rmaId, voucher code/value/%, policyRef, message, `nextStep` hint for the model, `alreadyProcessed`.
- Tools: `ProductCatalogTools.searchProducts`; `OrderAndReturnTools.getMyOrders/getMyProfile/initiateReturn/issueExchangeVoucher`. Added `getMyProfile` (name+pincode for MCP label). Late coupon flag computed in `getMyOrders` (deterministic).
- Identity: `CurrentUserService` (SecurityContext -> AppUser) -> `ToolContext[authenticatedUserId]`, memory id `username:sessionId`, trace. Missing identity fails closed.
- MCP: `spring-ai-starter-mcp-client`, connection `carrier` -> `${CARRIER_MCP_URL:http://localhost:8081}/mcp`, `initialized: false`; `ToolRegistry` fetches MCP tools per turn (degrades with trace note).
- Advisors (explicit orders): `AgentTraceAdvisor` (HIGHEST) -> `MessageChatMemoryAdvisor` (+100, window 20) -> `QuestionAnswerAdvisor` (+200) -> auto ToolCallingAdvisor; `ToolCallingManager.maxTotalToolCalls(8)`.
- Trace: `AgentTrace{injectedUserId/Username, ragHits[docType,sectionId,title,score], toolCalls[name, LOCAL|MCP, server, sanitized args, ms, success, outcome, resultPreview], notes, totalMs}` via ThreadLocal `TraceRecorder`. MCP callbacks don't receive ToolContext.
- API: `POST /api/assistant/ask`, `DELETE /api/assistant/conversation`.
- Restock (user decision): a NEW return approval adds the item quantity back to `Product.stockCount`; idempotent replays don't.
- Live verified (deepseek-v4.1-flash): all 6 demo scenarios; carrier log shows only trackingId/rmaId/pincode/category.

## Requirements
1. Connect `storefront-app` to `carrier-mcp-server` via `spring-ai-starter-mcp-client`, `http://localhost:8081/mcp` (Streamable HTTP).
2. Local `@Tool` beans using `ToolContext` / `SecurityContextHolder`. NO `userId` param exposed to the LLM:
   - `ProductCatalogTools.searchProducts(String category, Double maxPrice, Boolean inStockOnly)`: deterministic SQL filtering + product details.
   - `OrderAndReturnTools`:
     - `getMyOrders(ToolContext ctx)`: all orders+items of the authenticated user only.
     - `initiateReturn(String orderId, String sku, String reason, boolean isDefective, ToolContext ctx)`: verify ownership; category window (14 vs 30 days) and hygiene/opened rules in Java; idempotent RMA; returns structured `ReturnDecision` DTO.
     - `issueExchangeVoucher(String orderId, String sku, ToolContext ctx)`: enforce 31-45 day grace (80% standard / 90% VIP) in Java; idempotent voucher code.
3. `ChatClient` config:
   - Crisp system prompt: persona, tool-first for order/product facts, RAG-first for policies, refuse to guess order statuses.
   - `MessageChatMemoryAdvisor` (or session memory) keyed by `userId + sessionId` so multi-turn follow-ups work ("What about the jacket I bought last month?" then "Okay, give me the exchange voucher").
4. **Agent Execution Trace Collector** (custom `CallAdvisor` / tool-execution wrapper), captured per chat turn:
   - Retrieved RAG snippets + similarity scores
   - Local `@Tool` calls (name, sanitized args, execution time, result status)
   - Remote MCP tools invoked (`carrier-mcp-server`)
   - Authenticated `userId` injected server-side

## Design constraints
- `ReturnDecision` status codes: `APPROVED`, `REJECTED_OUTSIDE_WINDOW`, `REJECTED_NON_RETURNABLE`, `ELIGIBLE_FOR_EXCHANGE_CREDIT` (+ not-found for non-owned orders; same response as a missing order, never leak existence).
- Day diff computed from `deliveredAt` (not from LLM text). Windows: Apparel/Footwear 30d; Electronics 14d; opened electronics (audio) only if defective; opened Hygiene = final sale; 31-45d => exchange credit only (80% / 90% VIP).
- Idempotency keyed on (orderId, sku): second call returns same RMA id / voucher code.
- RMA flow: after approval the model may call MCP `generateReturnShippingLabel` / `findNearbyDropoffPoints` (pincode from user profile server-side if possible, else from user).
- Trace collector must distinguish local vs MCP tools (e.g. by tool callback origin/name) and record userId from the security context, not from model output.
- Late-delivery coupon (₹500 if >24h past `promisedDeliveryAt`) is policy-level; decide if it is a tool or RAG+Java check (prefer deterministic Java).
- Writes must update `OrderItem.returnStatus`/`rmaId` so the Phase 5 orders table can reflect them live.
- Verify DeepSeek tool-calling works on OpenRouter; swap model if flaky.

## Verification
Unit/integration tests on tools (no LLM): rejection, ownership, idempotency. Env-gated end-to-end chat test.

# Overview & guardrails

Role: Principal Java & AI Systems Architect. Build a production-grade, demo-ready **AI-Powered E-Commerce Storefront & Post-Purchase Assistant**: Java 21, Spring Boot 4.x, Spring AI 2.1.x.

Must demonstrate (wow-factor enterprise AI architecture):
1. **RAG** for nuanced return & warranty policies.
2. **Deterministic tool calling (`@Tool`)** with server-side identity enforcement and idempotency.
3. **MCP** via a separate Spring Boot MCP server simulating an external 3PL logistics/carrier service.
4. **Live agentic observability**: "Under-the-Hood Trace Drawer" in the UI showing RAG chunk retrieval, local `@Tool` calls and remote MCP calls in real time.

## Mandatory guardrails (all phases)
1. **Zero LLM-supplied user identity.** NEVER expose `userId` as a param of any `@Tool` or `@McpTool`. Inject the authenticated user out-of-band (`SecurityContextHolder` or Spring AI `ToolContext`). Every repository query scopes `WHERE user_id = :authenticatedUserId`.
2. **Deterministic policy in Java.** Never rely on prompts alone to block out-of-window/non-returnable items. Service layer computes exact day differences against DB `deliveredAt` and returns structured codes: `APPROVED`, `REJECTED_OUTSIDE_WINDOW`, `REJECTED_NON_RETURNABLE`, `ELIGIBLE_FOR_EXCHANGE_CREDIT`.
3. **Idempotent writes.** `initiateReturn` / `issueExchangeVoucher` called repeatedly for the same order item return the existing RMA/voucher, no duplicates.
4. **Multi-service workspace** (multi-module Maven):
   - `carrier-mcp-server` (port 8081): Spring Boot MCP server, `spring-ai-starter-mcp-server-webmvc`, Streamable HTTP.
   - `storefront-app` (port 8080): storefront, RAG engine, local tools, MCP client (`spring-ai-starter-mcp-client`), web UI.

## Resolved decisions (user-confirmed or derived)
- LLM provider: **OpenRouter only** (user has no other provider). Chat `deepseek/deepseek-v4.1-flash` (user choice, pinned id), embeddings `openai/text-embedding-3-small`, base URL `https://openrouter.ai/api/v1`, env `OPENROUTER_API_KEY`. Spring AI 2.x property names have no `.options`. Mirrors the user's `spring-ai-basics` project.
- Versions: Spring Boot 4.1.3, Spring AI 2.1.0.
- Repo location: `ecommerce-ai-assistant/` in repo root. Branch `ccr-8e701508-pkxqcw`. No PR unless asked.

# Original prompt (verbatim)

# Role & Objective
Act as a Principal Java & AI Systems Architect. Build a production-grade, demo-ready **AI-Powered E-Commerce Storefront & Post-Purchase Assistant** using **Java 21, Spring Boot 4.x, and Spring AI 2.1.x**.

The system must demonstrate an authoritative, "wow-factor" enterprise AI architecture combining:
1. **RAG (Retrieval-Augmented Generation)** for complex, nuanced return & warranty policies.
2. **Deterministic Tool Calling (`@Tool`)** with strict server-side identity enforcement and idempotency.
3. **Model Context Protocol (MCP)** via a separate Spring Boot MCP Server simulating an external 3PL Logistics & Carrier service.
4. **Live Agentic Observability ("Under-the-Hood Trace Drawer")** in the UI so demo viewers can watch RAG chunk retrieval, local `@Tool` calls, and remote MCP calls happen in real time.

Execute this build **strictly in phases**. After completing each phase, verify compilation/tests, provide a brief summary of what was built, and ask for confirmation before proceeding to the next phase.

---

# Architecture & Core Engineering Guardrails (Mandatory Across All Phases)
1. **Zero LLM-Supplied User Identity:** NEVER expose `userId` as a parameter in any `@Tool` or `@McpTool` schema. Always inject the authenticated user out-of-band via Spring Security (`SecurityContextHolder`) or Spring AI `ToolContext`. Every repository query MUST scope by `WHERE user_id = :authenticatedUserId`.
2. **Deterministic Policy Enforcement in Java:** Never rely on prompt instructions alone to block out-of-window or non-returnable items. The Java service layer must compute exact day differences against the database `deliveredAt` timestamp and return structured status codes (`APPROVED`, `REJECTED_OUTSIDE_WINDOW`, `REJECTED_NON_RETURNABLE`, `ELIGIBLE_FOR_EXCHANGE_CREDIT`).
3. **Idempotent Write Operations:** Calling `initiateReturn` or `issueExchangeVoucher` multiple times for the same order item must return the existing RMA/voucher rather than creating duplicate records.
4. **Multi-Service Workspace:** Structure the repository as a clean multi-module Maven project (or two sibling Maven projects in one root folder):
   - `carrier-mcp-server` (Port `8081`): Standalone Spring Boot MCP Server (`spring-ai-starter-mcp-server-webmvc`, Streamable HTTP transport).
   - `storefront-app` (Port `8080`): Main E-Commerce Storefront, RAG Engine, Local Tools, MCP Client (`spring-ai-starter-mcp-client`), and Web UI.

---

# Phase 1: Project Scaffolding, Domain Model & Seed Data
1. Set up the multi-module Maven project with Java 21, Spring Boot 4.x, Spring AI 2.1.x BOM, Spring Data JPA, H2 (in-memory for zero-setup demo portability), Spring Security, and Thymeleaf + HTMX + Tailwind CSS (via CDN for clean modern styling).
2. Configure LLM provider properties cleanly via environment variables (`OPENAI_API_KEY` or configurable OpenAI-compatible/Ollama profile) with sensible defaults.
3. Create JPA Entities & Repositories for:
   - `AppUser` (id, username, displayName, loyaltyTier: `STANDARD` / `VIP`, city, pincode)
   - `Product` (sku, name, category: `ELECTRONICS`, `APPAREL`, `FOOTWEAR`, `HYGIENE`, price, stockCount, specsDescription, imageUrl)
   - `CustomerOrder` (orderId, userId, orderDate, deliveredAt, status: `DELIVERED` / `IN_TRANSIT` / `DELAYED`, shipmentTrackingId)
   - `OrderItem` (id, orderId, sku, productName, category, quantity, pricePaid, boxOpened, returnStatus: `NONE` / `RETURN_APPROVED` / `EXCHANGE_ISSUED`, rmaId)
4. Create a `DataSeeder` (`CommandLineRunner`) that populates:
   - **2 Demo Users** (with a one-click "Switch User" switcher in the UI navbar so the demo presenter can test cross-user security isolation live!):
     - `alex` (VIP tier, Bangalore 560100)
     - `priya` (Standard tier, Mumbai 400001)
   - **8 Rich Products** across Electronics, Footwear, Apparel, and Hygiene (e.g., *AeroBuds Pro Wireless Earbuds*, *SummitGrip Waterproof Trail Shoes*, *Nimbus Down Jacket*, *PureSonic Electric Toothbrush*, etc.).
   - **4 Pre-Seeded Orders for `alex`** specifically crafted to trigger every demo scenario immediately:
     - `ORD-1001`: Delivered **4 days ago** — *SummitGrip Waterproof Trail Shoes* (Eligible for full return).
     - `ORD-1002`: Delivered **6 days ago** — *AeroBuds Pro Wireless Earbuds* (Electronics: 14-day window, conditional on whether box is opened/defective).
     - `ORD-1003`: Delivered **36 days ago** — *Nimbus Down Jacket* (Outside 30-day refund window, but inside 31–45 day partial store-credit/exchange window!).
     - `ORD-1004`: Status `DELAYED` — *TrailBlazer Hydration Pack* (In transit, delayed at carrier hub).
   - **2 Pre-Seeded Orders for `priya`** (e.g., `ORD-2001`) so we can demonstrate prompt-injection & cross-user isolation failure when `alex` tries to query `ORD-2001`.

---

# Phase 2: External Carrier & Logistics MCP Server (`carrier-mcp-server`)
1. Build `carrier-mcp-server` on port `8081` using `spring-ai-starter-mcp-server-webmvc` with `spring.ai.mcp.server.protocol=STREAMABLE` (Streamable HTTP).
2. Implement `@McpTool` methods simulating a third-party logistics provider:
   - `trackShipment(String trackingId)`: Returns live carrier scan events, current hub location, and delay reason (e.g., weather delay at Bangalore hub for `ORD-1004`'s tracking ID).
   - `generateReturnShippingLabel(String rmaId, String customerName, String pickupPincode, String category)`: Generates a structured shipping label object (AWB tracking number, barcode string, carrier name, instructions).
   - `findNearbyDropoffPoints(String pincode)`: Returns 3 realistic drop-off locker/hub locations with distance and operating hours for the given pincode.
3. Add clean request logging so the terminal/UI shows incoming MCP protocol tool invocations.

---

# Phase 3: RAG Policy Knowledge Base & Semantic Product Search (`storefront-app`)
1. Author a detailed, realistic `src/main/resources/docs/return-and-warranty-policy.md` containing:
   - Standard 30-day return window for Apparel & Footwear (unworn, tags attached).
   - Strict 14-day return window for Electronics; opened audio/earbuds can ONLY be returned if defective (hygiene & seal policy), whereas unopened electronics get a full refund.
   - Final-sale rule for Personal Care / Hygiene items (`PureSonic Electric Toothbrush`) once opened.
   - **31–45 Day Grace Policy:** Items between 31 and 45 days post-delivery are ineligible for original-payment refunds, but eligible for an **80% Instant Exchange / Store Credit Voucher** (90% for VIP users).
   - Late delivery compensation rule: Orders delayed more than 24 hours past promised delivery qualify for a ₹500 courtesy coupon.
2. Configure a `VectorStore` (`SimpleVectorStore` initialized at startup) that ingests:
   - `return-and-warranty-policy.md` (tagged with metadata `docType=POLICY`)
   - Rich product specification & use-case guides for all catalog items (tagged with metadata `docType=CATALOG`)
3. Wire `QuestionAnswerAdvisor` (or `RetrievalAugmentationAdvisor`) onto `ChatClient` so policy questions and fuzzy product recommendations ("Which shoes are best for muddy monsoon trails?") are grounded in retrieved chunks.

---

# Phase 4: Local `@Tool` Beans, Identity Security & MCP Client Wiring
1. Connect `storefront-app` to `carrier-mcp-server` via `spring-ai-starter-mcp-client` (`http://localhost:8081/mcp`).
2. Implement local `@Tool` beans in `storefront-app` using `ToolContext` / `SecurityContextHolder` (NO `userId` parameter exposed to the LLM):
   - `ProductCatalogTools`:
     - `searchProducts(String category, Double maxPrice, Boolean inStockOnly)`: Deterministic SQL filtering combined with product details.
   - `OrderAndReturnTools`:
     - `getMyOrders(ToolContext ctx)`: Lists all orders and items belonging strictly to the authenticated user.
     - `initiateReturn(String orderId, String sku, String reason, boolean isDefective, ToolContext ctx)`: Verifies order ownership, checks category-specific window (14 vs 30 days) and hygiene/opened rules deterministically in Java, creates an idempotent RMA, and returns a structured `ReturnDecision` DTO.
     - `issueExchangeVoucher(String orderId, String sku, ToolContext ctx)`: Enforces the 31–45 day grace rule (80% standard / 90% VIP credit) deterministically and issues an idempotent voucher code.
3. Configure `ChatClient` with:
   - A crisp system prompt defining persona, tool-first behavior for order/product facts, RAG-first behavior for policies, and refusal to guess order statuses.
   - `MessageChatMemoryAdvisor` (or Session memory) keyed by `userId + sessionId` so multi-turn follow-ups ("What about the jacket I bought last month?" -> "Okay, give me the exchange voucher then") work seamlessly.
4. Implement an **Agent Execution Trace Collector** (using a custom Spring AI `CallAdvisor` / tool execution wrapper) that captures for every chat turn:
   - Retrieved RAG policy/product snippets + similarity scores
   - Local `@Tool` calls executed (name, sanitized arguments, execution time, result status)
   - Remote MCP Server tools invoked (`carrier-mcp-server`)
   - Authenticated `userId` injected server-side

---

# Phase 5: "Wow-Factor" Demo UI & Verification Suite
1. Build a sleek, single-page **Storefront + AI Concierge** web interface using Thymeleaf + HTMX (or clean vanilla JS + Tailwind) featuring:
   - **Top Bar:** Logged-in user badge (`Alex - VIP` vs `Priya - Standard`) with a **1-click User Switcher** (to demo session/security isolation) and a **"Reset Demo Data"** button.
   - **Left/Main Panel (70%):**
     - **Product Catalog Grid** with live stock counts (updates dynamically when actions occur) and category filters.
     - **"My Orders" Live Table** showing order IDs, delivery age (`4 days ago`, `36 days ago`, `DELAYED`), and live return/RMA badges that update immediately after the chatbot processes a return or voucher!
   - **Right Panel (30%) — AI Assistant + "X-Ray Trace Drawer":**
     - Clean chat window with **1-Click Demo Prompt Chips** at the bottom so the presenter can trigger showcase flows effortlessly:
       1. *"Find waterproof trail shoes under ₹8,000 in stock"* (Catalog Tool + RAG)
       2. *"Return my running shoes from ORD-1001 and find a drop-off point"* (Local `@Tool` + Remote MCP Server)
       3. *"Can I return the earbuds from ORD-1002? I opened the box."* (Multi-turn RAG hygiene/defect rule check)
       4. *"Return my jacket from ORD-1003"* (Deterministic 36-day rejection -> proactive VIP 90% exchange voucher offer)
       5. *"Where is my delayed order ORD-1004?"* (MCP shipment tracking + late-delivery policy check)
       6. *"Show orders for user priya or ORD-2001"* (Live Prompt-Injection / Security Isolation Demo)
     - **Collapsible "Agent X-Ray / Trace" Panel** under each bot reply displaying badges for `[RAG Docs Used]`, `[Local @Tool]`, `[Remote MCP Tool]`, and `[Server-Injected User: alex]`.
2. Write a **JUnit 5 Integration Test Suite** verifying:
   - Out-of-window return (`ORD-1003`) is rejected by `OrderAndReturnTools` regardless of LLM phrasing.
   - Cross-user order access (`alex` querying `priya`'s `ORD-2001`) throws/returns not-found at the repository/tool boundary.
   - Idempotency: calling `initiateReturn` twice for `ORD-1001` yields the exact same RMA ID.
3. Provide a clear `README.md` with exact startup commands for both services and a 3-minute live demo script.

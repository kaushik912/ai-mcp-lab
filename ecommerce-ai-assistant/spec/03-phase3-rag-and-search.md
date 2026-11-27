# Phase 3: RAG policy knowledge base & semantic product search (`storefront-app`) (DONE)

## As built
- `docs/return-and-warranty-policy.md`: POL-1 apparel/footwear 30d, POL-2 electronics 14d, POL-3 in-ear hygiene/seal, POL-4 31-45d grace (80%/90% VIP, apparel+footwear only), POL-5 hygiene final sale, POL-6 warranty, POL-7 late delivery ₹500, POL-8 process, POL-9 VIP.
- `docs/product-guides.md`: one section per SKU (8), with "best for / not ideal for" use-cases.
- `MarkdownSectionSplitter`: one Document per `##` section; metadata `docType`, `sectionId`, `title`, `source`.
- `KnowledgeBaseConfig`: `SimpleVectorStore`, startup ingestion (17 chunks); skipped with warning when no key; ingestion failure does not crash app.
- `AssistantConfig`: `assistantChatClient` + system prompt + `QuestionAnswerAdvisor` (topK 4, threshold 0.3; `app.rag.*`).
- `AssistantService.ask()` -> `AssistantReply(answer, ragChunks[docType, sectionId, title, score])`. Temp endpoint `POST /api/assistant/ask` (basic auth).
- Verified live: jacket/36d -> POL-4 (0.585) top; answer offers 90% VIP voucher. Muddy monsoon -> SUMMIT-SHOE-01 top.

## Requirements
1. Write `src/main/resources/docs/return-and-warranty-policy.md`, detailed and realistic, containing:
   - Standard **30-day** return window for Apparel & Footwear (unworn, tags attached).
   - Strict **14-day** window for Electronics; opened audio/earbuds can ONLY be returned if defective (hygiene & seal policy); unopened electronics get a full refund.
   - Final-sale rule for Personal Care/Hygiene once opened (`PureSonic Electric Toothbrush`).
   - **31-45 day grace policy:** ineligible for original-payment refund but eligible for an **80% instant exchange / store-credit voucher (90% for VIP)**.
   - Late-delivery compensation: orders delayed **>24h past promised delivery** qualify for a **₹500 courtesy coupon**.
2. `VectorStore` = `SimpleVectorStore` initialised at startup, ingesting:
   - the policy doc, metadata `docType=POLICY`
   - rich product spec & use-case guides for ALL catalog items, metadata `docType=CATALOG`
3. Wire `QuestionAnswerAdvisor` (or `RetrievalAugmentationAdvisor`) onto `ChatClient` so policy questions and fuzzy recommendations ("Which shoes are best for muddy monsoon trails?") are grounded in retrieved chunks.

## Notes / to verify
- Verify OpenRouter embeddings endpoint works with `openai/text-embedding-3-small` (the user's `spring-ai-basics` uses it successfully). Fallback: local embedding model.
- Policy must agree with seed data: ORD-1001 (4d footwear, OK), ORD-1002 (6d electronics, opened), ORD-1003 (36d apparel, grace), ORD-1004 (late).
- Keep policy chunks small/well-headed so retrieval scores are meaningful in the trace drawer (Phase 4/5 shows similarity scores).
- Startup must not hard-fail when `OPENROUTER_API_KEY` is unset (decide: skip ingestion with a warning, or fail fast with a clear message).
- Tests that call the real model must be env-gated on `OPENROUTER_API_KEY`.

## Verification
Context loads; ingestion count logged; (env-gated) retrieval test returns the grace-policy chunk for a 36-day question.

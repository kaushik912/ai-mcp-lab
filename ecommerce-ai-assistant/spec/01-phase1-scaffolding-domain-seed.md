# Phase 1: Scaffolding, domain model & seed data (DONE)

## Requirements
1. Multi-module Maven; Java 21, Spring Boot 4.x, Spring AI 2.1.x BOM, Spring Data JPA, H2 in-memory (zero-setup portability), Spring Security, Thymeleaf + HTMX + Tailwind (CDN) for the UI.
2. LLM provider via env vars with sensible defaults. **DEVIATION:** OpenRouter only (originally `OPENAI_API_KEY` or OpenAI-compatible/Ollama).
3. JPA entities & repositories:
   - `AppUser`: id, username, displayName, loyaltyTier (`STANDARD`/`VIP`), city, pincode
   - `Product`: sku, name, category (`ELECTRONICS`, `APPAREL`, `FOOTWEAR`, `HYGIENE`), price, stockCount, specsDescription, imageUrl
   - `CustomerOrder`: orderId, userId, orderDate, deliveredAt, status (`DELIVERED`/`IN_TRANSIT`/`DELAYED`), shipmentTrackingId
   - `OrderItem`: id, orderId, sku, productName, category, quantity, pricePaid, boxOpened, returnStatus (`NONE`/`RETURN_APPROVED`/`EXCHANGE_ISSUED`), rmaId
4. `DataSeeder` (`CommandLineRunner`):
   - Users: `alex` (VIP, Bangalore 560100), `priya` (Standard, Mumbai 400001). UI navbar needs 1-click Switch User to demo cross-user isolation.
   - 8 products across Electronics, Footwear, Apparel, Hygiene (e.g. AeroBuds Pro Wireless Earbuds, SummitGrip Waterproof Trail Shoes, Nimbus Down Jacket, PureSonic Electric Toothbrush, ...).
   - alex orders: `ORD-1001` delivered 4 days ago, SummitGrip shoes (fully returnable); `ORD-1002` delivered 6 days ago, AeroBuds (14-day electronics window, conditional on opened/defective); `ORD-1003` delivered 36 days ago, Nimbus jacket (outside 30d refund, inside 31-45d exchange window); `ORD-1004` `DELAYED`, TrailBlazer Hydration Pack (delayed at carrier hub).
   - priya: 2 orders incl. `ORD-2001`, for the prompt-injection / cross-user isolation demo (alex tries to query `ORD-2001`).

## Implementation notes / additions
- Added `CustomerOrder.promisedDeliveryAt` (needed for the late-delivery coupon rule in Phase 3/4).
- `ORD-1002.boxOpened = true` (DB-driven opened-earbuds rule). Hydration pack categorised `APPAREL`. 8th product ThermoMerino stock 0 (demo in-stock filter). Others: VoltEdge power bank, UrbanStride running shoes.
- Repos expose ONLY owner-scoped order/item queries (no unscoped orderId lookup).
- `DemoDataService.reset()` re-seeds (hook for "Reset Demo Data").
- Baseline security: in-memory alex/priya, pw `demo`, HTTP basic. Switcher is Phase 5.
- priya: `ORD-2001` Nimbus jacket delivered 10d ago; `ORD-2002` power bank in transit.

## Verification: DONE
`DemoDataSeedTest` (4 tests): counts, order ages 4/6/36, DELAYED status, owner-scoped lookups, catalog filter. `mvn verify` green.

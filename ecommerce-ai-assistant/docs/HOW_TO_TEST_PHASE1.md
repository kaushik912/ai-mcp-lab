# How to test Phase 1: scaffolding, domain model & seed data

Spec: [../spec/01-phase1-scaffolding-domain-seed.md](../spec/01-phase1-scaffolding-domain-seed.md)

Goal: prove the multi-module build works, the 4 entities exist, demo data is seeded with the right ages, order lookups are owner-scoped, and the app is protected by login.

## Prerequisites

- Java 21, Maven 3.8+, `curl`, `jq`
- No API key needed for this phase.

All commands run from the project root:

```bash
cd ecommerce-ai-assistant
```

## Step 1: Build both modules

```bash
mvn -B -q install -DskipTests && echo BUILD OK
```

Expected: `BUILD OK`, and two jars exist:

```bash
ls carrier-mcp-server/target/*.jar storefront-app/target/*.jar
```

## Step 2: Run the Phase 1 tests

```bash
mvn -B -q -pl storefront-app test -Dtest=DemoDataSeedTest
grep "Tests run" storefront-app/target/surefire-reports/*DemoDataSeedTest.txt
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`

What each test proves:

| Test | Proves |
|---|---|
| `seedsUsersProductsAndOrders` | 2 users, 8 products, 6 orders; alex is VIP; priya's pincode is 400001 |
| `alexOrdersHaveExpectedAges` | ORD-1001/1002/1003 delivered 4/6/36 days ago; ORD-1004 is DELAYED |
| `orderLookupIsScopedToOwner` | alex cannot load ORD-2001 or its items; priya can |
| `catalogSearchFiltersDeterministically` | category/price/stock filter works; out-of-stock item excluded |

## Step 3: Inspect the owner-scoping guardrail in code

```bash
grep -n "userId" storefront-app/src/main/java/com/aicommerce/storefront/repo/CustomerOrderRepository.java \
                 storefront-app/src/main/java/com/aicommerce/storefront/repo/OrderItemRepository.java
```

Expected: every `@Query` filters on `userId = :userId`. There must be no custom order/item lookup without it.

## Step 4: Start the storefront and check the seed log

```bash
java -jar storefront-app/target/storefront-app-0.1.0-SNAPSHOT.jar
```

Expected log lines:

```
Started StorefrontApplication in ... seconds
Demo data seeded: 2 users, 8 products, 6 orders
```

(Without `OPENROUTER_API_KEY` you also see `RAG ingestion skipped ...`. That's fine for Phase 1.)

## Step 5: Security checks with curl (second terminal)

```bash
# No credentials -> 401
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/orders

# Wrong password -> 401
curl -s -o /dev/null -w "%{http_code}\n" -u alex:wrong localhost:8080/api/orders

# Valid login -> authenticated
curl -s -o /dev/null -w "%{http_code}\n" -u alex:demo localhost:8080/
```

Expected: `401`, `401`, then `200` (with the Phase 5 UI; it was `404` before Phase 5 because there was no page at `/`).

Note: since Phase 5, CSRF protection is on. An unauthenticated **POST** without Basic credentials gets `403` (CSRF) instead of `401`. That's why these checks use GET.

Demo logins: `alex` / `demo` and `priya` / `demo`.

## Step 6 (optional): See the seed data through the app

Phase 1 has no data endpoint of its own. With Phase 4 in place and an API key set, you can list orders through the chat API. See [HOW_TO_TEST_PHASE4.md](HOW_TO_TEST_PHASE4.md), scenario 6.

Stop the app with `Ctrl+C`.

## Pass checklist

- [ ] `mvn install` builds both modules (Java 21, Boot 4.1.3, Spring AI 2.1.0)
- [ ] `DemoDataSeedTest`: 4/4 pass
- [ ] Log shows `Demo data seeded: 2 users, 8 products, 6 orders`
- [ ] Order/item queries all scoped by `userId`
- [ ] curl: 401 without login, 401 with a bad password, 200 with a valid login (404 before Phase 5)

Seed data reference:

| User | Order | Item | State |
|---|---|---|---|
| alex (VIP, 560100) | ORD-1001 | SummitGrip Waterproof Trail Shoes | delivered 4 days ago |
| alex | ORD-1002 | AeroBuds Pro Wireless Earbuds (box opened) | delivered 6 days ago |
| alex | ORD-1003 | Nimbus Down Jacket | delivered 36 days ago |
| alex | ORD-1004 | TrailBlazer Hydration Pack | DELAYED, tracking TRK-BLR-77412 |
| priya (Standard, 400001) | ORD-2001 | Nimbus Down Jacket | delivered 10 days ago |
| priya | ORD-2002 | VoltEdge Power Bank | IN_TRANSIT |

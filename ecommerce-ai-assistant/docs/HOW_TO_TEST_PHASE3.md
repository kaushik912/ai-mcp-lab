# How to test Phase 3: RAG policy knowledge base & semantic product search

Spec: [../spec/03-phase3-rag-and-search.md](../spec/03-phase3-rag-and-search.md)

Goal: prove the policy document and product guides are split into chunks, embedded at startup into the vector store with `docType` metadata, and retrieved for policy questions and fuzzy product questions, with answers grounded in those chunks.

## Prerequisites

- Java 21, Maven, `curl`, `jq`
- **OpenRouter key** for the live steps:

```bash
export OPENROUTER_API_KEY=sk-or-...
cd ecommerce-ai-assistant
mvn -B -q install -DskipTests
```

## Step 1: Offline tests (no key needed)

```bash
mvn -B -q -pl storefront-app test -Dtest=KnowledgeBaseDocumentsTest
grep "Tests run" storefront-app/target/surefire-reports/*KnowledgeBaseDocumentsTest.txt
```

Expected: `Tests run: 3, Failures: 0`. These prove:

- the policy splits into exactly 9 chunks, POL-1 .. POL-9
- every one of the 8 products has a CATALOG chunk
- the policy states the numbers the Java code enforces: 30 days, 14-day, day 31, day 45, 80%, 90%, 24 hours, ₹500

## Step 2: Live retrieval tests (uses the key, costs a fraction of a cent)

```bash
mvn -B -q -pl storefront-app test -Dtest=RagLiveTest
grep "Tests run" storefront-app/target/surefire-reports/*RagLiveTest.txt
```

Expected: `Tests run: 5, Failures: 0, Skipped: 0`. If it shows `Skipped: 5`, the key isn't exported in this shell.

| Test | Query | Must retrieve |
|---|---|---|
| `gracePolicyRetrievedForLateApparelReturn` | jacket 36 days ago | POL-4 in top 3 |
| `hygieneSealRuleRetrievedForOpenedEarbuds` | opened earbuds | POL-3 in top 3 |
| `lateDeliveryRuleRetrieved` | delayed 3 days | POL-7 in top 3 |
| `muddyMonsoonQueryFindsWaterproofTrailShoe` | muddy monsoon trails | SUMMIT-SHOE-01 first |
| `assistantAnswerIsGroundedInGracePolicy` | full chat | POL-4 retrieved, answer mentions 90%/voucher |

## Step 3: Start the storefront and check ingestion

```bash
java -jar storefront-app/target/storefront-app-0.1.0-SNAPSHOT.jar
```

Expected log line (a few seconds after `Started StorefrontApplication`):

```
RAG knowledge base ready: 17 chunks (9 POLICY, 8 CATALOG)
```

Without a key you instead get `RAG ingestion skipped ...` and the app still starts.

## Step 4: Ask policy and product questions with curl (second terminal)

```bash
ask() { curl -s -u alex:demo -c /tmp/jar -b /tmp/jar -H 'Content-Type: text/plain' \
          --data-binary "$1" localhost:8080/api/assistant/ask; }
```

### 4a. Policy question: grace policy chunk ranks first

```bash
ask 'What is the policy for returning a jacket 36 days after delivery for a VIP member?' \
  | jq '{answer: .answer[0:300], rag: [.ragChunks[] | "\(.sectionId) \(.docType) \(.score|tostring|.[0:5])"]}'
```

Expected (scores vary slightly):

```json
{
  "answer": "... past the 30-day window (POL-1) ... 31–45 day grace policy (POL-4) ... 90% ...",
  "rag": ["POL-4 POLICY 0.674", "POL-9 POLICY 0.621", "POL-1 POLICY 0.600", "POL-8 POLICY 0.497"]
}
```

Check: POL-4 is among the chunks (usually first), the answer says no refund but a **90%** voucher for VIP, and it cites policy ids.

### 4b. Fuzzy product question: catalog chunk ranks first

```bash
ask 'Which shoes are best for muddy monsoon trails?' \
  | jq '{answer: .answer[0:250], rag: [.ragChunks[] | "\(.sectionId) \(.docType)"]}'
```

Expected: first chunk `SUMMIT-SHOE-01 CATALOG`, and the answer recommends **SummitGrip Waterproof Trail Shoes**, explaining the waterproof membrane and lugged outsole. UrbanStride should **not** be recommended for mud (its guide says it isn't waterproof).

### 4c. Each hygiene/electronics rule retrieves its own chunk

```bash
for q in 'Can I return earbuds after opening them?' \
         'Is an opened electric toothbrush returnable?' \
         'My order arrived 3 days late, do I get compensation?'; do
  echo "Q: $q"; ask "$q" | jq -c '[.ragChunks[].sectionId]'
done
```

Expected: the lists include `POL-3`, `POL-5` and `POL-7` respectively.

Note: since Phase 4 this endpoint also runs tools and keeps chat memory. That doesn't affect these checks; the `ragChunks` field is the RAG evidence.

Stop the app with `Ctrl+C`.

## Pass checklist

- [ ] `KnowledgeBaseDocumentsTest` 3/3 (offline)
- [ ] `RagLiveTest` 5/5 with the key (not skipped)
- [ ] Startup log: `17 chunks (9 POLICY, 8 CATALOG)`
- [ ] Jacket/36 days -> POL-4 retrieved; answer: no refund, 90% VIP voucher
- [ ] Muddy monsoon -> `SUMMIT-SHOE-01` first; SummitGrip recommended
- [ ] Every chunk in `ragChunks` has `docType` POLICY or CATALOG and a score
- [ ] App starts without a key (ingestion skipped with a warning)

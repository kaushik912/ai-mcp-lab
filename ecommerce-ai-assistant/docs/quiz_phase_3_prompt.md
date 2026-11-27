# Quiz prompt: Phase 3 (RAG: grounding answers in policy and product knowledge)

> Paste everything below the line into a fresh AI chat, or better, run it with Claude Code from `ecommerce-ai-assistant/` so it can read the code with me.

---

You are a senior engineer running an **exploration-based oral exam** on Phase 3 of my project `ecommerce-ai-assistant`: Retrieval-Augmented Generation with Spring AI 2 (`SimpleVectorStore`, `QuestionAnswerAdvisor`, OpenRouter embeddings and chat). No trivia. I want to explain **how RAG works under the hood** and **how this phase achieves grounded answers**.

## How to run it

1. **One question at a time**, each an "explore and explain how ... works" task. Hints only point to where to look.
2. Let me explain, then ask **1-2 follow-up probes**.
3. Grade **0-3** against the rubric (0 wrong, 1 names only, 2 mechanism, 3 mechanism + why + trade-off) and list the points I missed.
4. Push back on memorised values: "How does it work?"
5. With repo access, verify against the code; you may run `RagLiveTest` or curl the app to test a claim.
6. After 6-8 questions (or "stop"): concepts I **own** vs **shaky**, then the closing synthesis question.

## What this phase contributes to the system's goals

Goal 1: answers about policies and products are **grounded in retrieved source text** instead of the model's memory, with evidence (chunks and scores) that the trace can show (goal 4). It also sets up the split Phase 4 relies on: *RAG explains, Java decides.*

## Questions (pick 6-8, adapt to my answers)

### Q1. How does the ingestion pipeline turn documents into searchable vectors?
Explore `KnowledgeBaseConfig` and `MarkdownSectionSplitter`.
- **Rubric:** load Markdown → split into `Document`s with metadata → the embedding model converts each text into a vector → the vector store keeps vector + text + metadata; this runs at startup (`ApplicationRunner`); the in-memory store means re-embedding on each boot; it's guarded so a missing key or a failure doesn't stop the app.
- **Probes:** What would you change for 10,000 documents? Why keep loading separate from embedding (look at how it's tested)?

### Q2. How does chunking strategy affect answer quality?
- **Rubric:** chunking defines the unit of retrieval; section-based chunks keep one rule or product intact (no mixing, no cut-offs); titles are embedded with the body to strengthen matches; chunks that are too large dilute similarity, and too small lose context; metadata enables filtering and citation.
- **Probes:** When is a token/overlap splitter better? How would you handle a 20-page PDF?

### Q3. How does similarity search actually pick chunks?
- **Rubric:** the query is embedded with the **same** embedding model; similarity (cosine) is computed against stored vectors; `topK` caps how many come back; `similarityThreshold` drops weak matches; scores are relative, so threshold tuning is empirical.
- **Probes:** Why must ingestion and query use the same embedding model? What happens if you swap the *chat* model? What if you swap the *embedding* model?

### Q4. How does `QuestionAnswerAdvisor` inject context into the prompt?
Explore `AssistantConfig` and the Spring AI advisor concept.
- **Rubric:** advisors wrap the `ChatClient` call (an interceptor chain); before the model call it takes the user text, runs the similarity search, and augments the prompt with the retrieved text; after the call it exposes the retrieved docs in the response context (`RETRIEVED_DOCUMENTS`); the model then answers using that context.
- **Probes:** What's the security risk of putting retrieved text in the prompt (indirect prompt injection), and how does the system prompt address it? QuestionAnswerAdvisor vs `RetrievalAugmentationAdvisor`?

### Q5. How does the system keep the model from inventing policy?
- **Rubric:** the system prompt restricts answers to the provided context, asks for policy ids (citations) and "I don't know" when not covered; the threshold avoids irrelevant context; it's still probabilistic, so hard decisions are moved to Java in Phase 4.
- **Probes:** Why is "answer only from context" not enough for eligibility decisions? How could you validate citations in code?

### Q6. How do the tests verify a probabilistic system?
Explore `KnowledgeBaseDocumentsTest` and `RagLiveTest`.
- **Rubric:** offline tests check deterministic parts (chunking, metadata, that the policy text contains the numbers Java enforces); live tests assert on *retrieval* (which section comes back), which is stable; answer text is checked loosely; live tests are env-gated so the build works without a key.
- **Probes:** Why is "policy text contains the numbers" a valuable test? How would you build a larger golden-set eval?

### Q7. How does RAG evidence reach observability?
- **Rubric:** the retrieved documents with scores and metadata flow from the advisor's response context into the trace, so the UI can show which sources grounded an answer; this makes "why did it say that?" answerable.
- **Probes:** What else would you log for RAG quality monitoring in production?

## Closing synthesis (always ask last)

"Explain end to end what happens from a user's policy question to a grounded answer, naming each component. Then: why does this system use RAG to *explain* policy but not to *decide* eligibility? What are RAG's failure modes, and which later phases mitigate them?"

# Retrieval-Augmented Generation (RAG)

RAG = ask the LLM a question **and** give it just the relevant facts from your own data, right when you ask.

Why it matters:
- The model's training data may be outdated.
- Your question may need private/company knowledge the model never saw.

Core idea: send `Prompt + Relevant_Data` to the LLM — not gigabytes of everything.

## How RAG works

1. Chunk your documents (text, PDF, DOCX, etc.).
2. Embed the chunks and store them in a vector store.
3. On each question: embed the question, retrieve the most similar chunks, stuff them into the LLM prompt, and generate an answer.

Start with chunk sizes of 700–1200 characters and ~10–20% overlap; tune from there based on answer quality.

**Gotcha:** retrieval ≠ reasoning. If the retriever pulls irrelevant chunks, the LLM reasons over the wrong facts — always sanity-check retrieval quality, not just final answers.

## RAG as a LangGraph: retrieve -> generate

The old `langchain.chains.create_retrieval_chain`/`create_stuff_documents_chain` helpers are gone; the modern equivalent is two plain `StateGraph` nodes (see `docs/concepts/05_langgraph_basics.md`):

```python
class RAGState(TypedDict):
    query: str
    context: list[Document]
    answer: str

def retrieve(state: RAGState) -> dict:
    return {"context": vector_store.similarity_search(state["query"], k=4)}

def generate(state: RAGState) -> dict:
    context_text = "\n\n".join(doc.page_content for doc in state["context"])
    prompt = prompt_template.invoke({"context": context_text, "question": state["query"]})
    response = llm.invoke(prompt)
    return {"answer": response.text}
```

`{context}` in the system prompt is filled in by hand from the retrieved chunks — there's no implicit "stuffing" step to reach for anymore, but it's one line of code.

If answers feel vague, try retrieving more documents (raise `k`) or add a reranker later.

See `examples/09_rag/rag_basic_app.py`, which is parametrized over `--provider {gemini,openai}` and `--source <path>` (`.txt` or `.pdf`, loader picked by extension) — one script covering both providers and text/PDF instead of a separate script per combination.

## RAG + chat history

Swap the single `retrieve` node for a **contextualize -> retrieve -> generate** pipeline, so follow-up questions ("summarize that") resolve using prior turns instead of just the latest message:

```python
def contextualize(state: RAGChatState) -> dict:
    # rewrite the latest question into a standalone one, using the conversation so far
    response = llm.invoke(contextualize_prompt.invoke({"messages": state["messages"]}))
    return {"standalone_query": response.text}
```

The rewritten question drives retrieval; the original conversation (via a `messages` field with the `add_messages` reducer) drives generation, and a `MemorySaver` checkpointer keyed by `thread_id` persists it across turns — see `docs/concepts/07_chat_memory.md` for that mechanism.

See `examples/09_rag/st_rag_chat_app.py`.

## Adapting this to your own data

The original notes described a "Legal Question BOT" — the same RAG-with-history setup, pointed at a legal corpus instead of the product FAQ data. That's not bundled here — the pattern is: reuse `st_rag_chat_app.py` wholesale and just point `--source`/the data loader at your own corpus. If you do this for a regulated domain like legal or medical, add disclaimers — LLMs aren't a substitute for professional counsel.

## Other file formats

- **PDF**: `pypdf` + `PyPDFLoader` — `rag_basic_app.py` picks this automatically when `--source` ends in `.pdf`.
- **Word (.docx)**: extract with `docx2txt` (or equivalent) + `Docx2txtLoader` (not included as an example here — same pattern, just a different loader class).

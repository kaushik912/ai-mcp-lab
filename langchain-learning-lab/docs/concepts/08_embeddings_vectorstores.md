# Embeddings and Vector Databases

Machines work with numbers, not words. An **embedding** is a vector that captures the semantic meaning of text — similar meanings produce vectors that are close together. "Happy," "joyful," and "glad" would have similar embeddings.

Common uses: document similarity, meaning-based search, recommendation systems, cross-language understanding.

## Embeddings via LangChain

```python
embeddings = get_gemini_embeddings()  # or OpenAIEmbeddings(api_key=...)
vec1 = embeddings.embed_query("happy")
vec2 = embeddings.embed_query("joyful")
similarity = np.dot(vec1, vec2)  # e.g. ~0.88 -> semantically close
```

`embed_documents()` embeds multiple pieces of text at once, returning one vector per input. `embed_query()` calls are chargeable but much cheaper than `invoke()` calls.

See `examples/08_embeddings_vectorstores/embeddings_similarity_demo.py` (`--provider {gemini,openai}`).

## How vector databases work

1. Split data into small, meaningful chunks.
2. Generate embeddings for each chunk.
3. Store chunks + embeddings in a vector database.
4. On query: embed the query, compare against stored embeddings, return the most similar chunks.

| Vector Store | Description |
|---|---|
| Chroma | Simple and local, great for prototyping |
| FAISS | Facebook AI Similarity Search — fast and efficient |
| Pinecone | Cloud-based, scalable |
| LanceDB | Lightweight and modern |

LangChain makes switching vector stores a near one-line change — `examples/08_embeddings_vectorstores/chroma_vectorstore_demo.py` uses Chroma; swapping `Chroma` for `langchain_community.vectorstores.FAISS` is the only change needed to use FAISS instead.

## Chunking parameters

```python
RecursiveCharacterTextSplitter(chunk_size=200, chunk_overlap=10)
```
`chunk_size` sets each chunk's character length; `chunk_overlap` keeps context continuity between adjacent chunks.

## High-level vs. manual retrieval

High level: `retriever.invoke(text)` — embeds the query and searches in one call.

Manual (what's happening under the hood):
```python
embedding_vector = embeddings.embed_query(text)
docs = db.similarity_search_by_vector(embedding_vector)
```

At this stage there's no generative AI yet — just embeddings and similarity search. Combining this with generation is RAG, covered next in `docs/concepts/09_rag.md`.

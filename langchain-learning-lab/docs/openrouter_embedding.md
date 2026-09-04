Here is how to use OpenRouter's free embedding models—`nvidia/nemotron-3-embed-1b:free`, `liquid/lfm-2.5-embedding-350m:free`, and `nvidia/llama-nemotron-embed-vl-1b-v2:free`—with **LangChain** in Python.

Since OpenRouter uses an OpenAI-compatible API, you can use the `OpenAIEmbeddings` class from `langchain_openai`.

### Prerequisites

Install the required package:

```bash
pip install langchain-openai

```

Set your OpenRouter API key as an environment variable:

```bash
export OPENROUTER_API_KEY="your_openrouter_api_key_here"

```

---

### Python Code Example

```python
import os
from langchain_openai import OpenAIEmbeddings

# Define OpenRouter configuration
OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
API_KEY = os.getenv("OPENROUTER_API_KEY")

# 1. NVIDIA Nemotron 3 Embed 1B (Text Embedding)
embeddings_nemotron = OpenAIEmbeddings(
    model="nvidia/nemotron-3-embed-1b:free",
    openai_api_base=OPENROUTER_BASE_URL,
    openai_api_key=API_KEY
)

text_query = "What is semantic search and RAG?"
query_vector = embeddings_nemotron.embed_query(text_query)
print("Nemotron 3 Embedding Vector Length:", len(query_vector))


# 2. Liquid LFM 2.5 Embedding 350M (Lightweight Text Embedding)
embeddings_liquid = OpenAIEmbeddings(
    model="liquid/lfm-2.5-embedding-350m:free",
    openai_api_base=OPENROUTER_BASE_URL,
    openai_api_key=API_KEY
)

documents = [
    "LangChain simplifies building applications with LLMs.",
    "OpenRouter provides access to various open-source embedding models."
]
doc_vectors = embeddings_liquid.embed_documents(documents)
print(f"Liquid LFM 2.5 Embedded {len(doc_vectors)} documents.")


# 3. NVIDIA Llama Nemotron Embed VL 1B V2 (Multimodal Text Embeddings)
embeddings_vl = OpenAIEmbeddings(
    model="nvidia/llama-nemotron-embed-vl-1b-v2:free",
    openai_api_base=OPENROUTER_BASE_URL,
    openai_api_key=API_KEY
)

multimodal_text_query = "A photo of a dog playing in the park"
vl_query_vector = embeddings_vl.embed_query(multimodal_text_query)
print("Llama Nemotron VL Query Vector Length:", len(vl_query_vector))

```

---

### Integration with LangChain Vector Stores (e.g., FAISS or Chroma)

You can pass any of these configured embedding instances directly into a LangChain vector store for similarity search or RAG pipelines:

```python
from langchain_community.vectorstores import FAISS

# Initialize FAISS vector store using the free Liquid embedding model
vectorstore = FAISS.from_texts(
    texts=documents,
    embedding=embeddings_liquid
)

# Perform similarity search
results = vectorstore.similarity_search("Tell me about OpenRouter", k=1)
print("Top Result:", results[0].page_content)

```
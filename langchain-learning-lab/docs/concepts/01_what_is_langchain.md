# What Is LangChain?

LangChain is an open-source framework that makes it easy to build applications powered by Large Language Models (LLMs). It handles the repetitive boilerplate so you can focus on the app logic.

With LangChain you can:
- Quickly develop standalone or web-based AI applications.
- Effortlessly switch between different LLMs (OpenAI, Gemini, open-source models).
- Write clean, minimal code to bring ideas to life.

## The Magic Behind LangChain

| Model Type | Class to Use |
|---|---|
| OpenAI models | `ChatOpenAI` |
| Google Gemini | `ChatGoogleGenerativeAI` |

The overall workflow stays the same no matter which model you use — see `examples/01_first_programs/provider_tour.py`.

## Building with Chains and Graphs

LangChain lets you build "chains" — sequences where the output of one model becomes the input to the next — via LCEL (`docs/concepts/03_lcel.md`). For anything with more than one step, branching, or memory, this repo builds on **LangGraph** instead: a `StateGraph` of nodes and edges (`docs/concepts/05_langgraph_basics.md`), which is what `docs/concepts/06_sequential_workflows.md`, chat memory, RAG, and agents are all built from underneath.

Other features:
- Maintaining chat history automatically via LangGraph checkpointers (`docs/concepts/07_chat_memory.md`).
- Loading documents, creating vector stores, and answering queries based on that content (`docs/concepts/08_embeddings_vectorstores.md`, `docs/concepts/09_rag.md`).
- Processing images as part of AI workflows.
- Building AI agents that think and act dynamically (`docs/concepts/10_agents.md`).

## LangChain + Web Applications

In a web app, LangChain acts as the management layer between your front-end and the LLM — coordinating prompts and responses.

## Bonus Notes

- "Chain" means linking one model's output to another's input, forming a workflow of reasoning steps.
- **RAG (Retrieval-Augmented Generation)** combines document retrieval with LLM generation for more accurate, context-aware responses.

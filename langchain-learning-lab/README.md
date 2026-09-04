# LangChain Learning Lab

Runnable Python port of a personal LangChain course that was originally written as markdown notes with embedded code blocks. Every example here is a real, standalone `.py` (Streamlit apps prefixed `st_`) file — no more copy-pasting fenced code out of prose to try something. Conceptual explanations live under `docs/`, separate from the code.

Rewritten on the current LangChain v1 / LangGraph API: multi-step examples are `StateGraph`s (nodes + edges + a checkpointer for memory) rather than `langchain.chains`/`RunnableWithMessageHistory`/`AgentExecutor`, which are gone or deprecated in LangChain v1. See `docs/concepts/05_langgraph_basics.md` for the graph fundamentals every later example builds on.

Source material: `code_public/myblog/technical/genai/langchain-learning/` (markdown notes, kept as-is — this repo is a from-scratch port, not a migration).

## Quickstart

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pip install -e .       # makes `common/` importable from any example script
cp .env.example .env   # then fill in whichever provider key(s) you use
```

Most examples default to Gemini, which has a free tier (`GEMINI_API_KEY` in `.env`, no local model or paid credits needed) — see `docs/setup.md`. OpenAI is a drop-in alternative wherever a script exposes `--provider`.

## Running an example

- Plain script: `python examples/01_first_programs/provider_tour.py`
- Streamlit app (any file starting with `st_`): `streamlit run examples/03_prompt_templates/st_cuisine_info_app.py`

## Layout

```
common/           shared helpers: env/key loading, LLM client factories, data path resolution
common/prompts/   prompt/template text used by the examples, kept out of the scripts themselves
data/             shared data assets used by RAG examples (product-data.txt)
examples/         one numbered folder per topic, mirroring the course order below
docs/             concept explanations (prose only, no embedded code) + setup guide
```

| Topic | Examples | Concept doc |
|---|---|---|
| First programs (OpenAI + Gemini) | `examples/01_first_programs/` | `docs/concepts/01_what_is_langchain.md` |
| Debugging & Streamlit basics | `examples/02_debugging_and_streamlit/` | — |
| Prompt templates | `examples/03_prompt_templates/` | `docs/concepts/02_prompt_templates.md` |
| LCEL | `examples/04_lcel/` | `docs/concepts/03_lcel.md` |
| LangGraph fundamentals | `examples/05_langgraph_basics/` | `docs/concepts/05_langgraph_basics.md` |
| Sequential workflows | `examples/06_sequential_workflows/` | `docs/concepts/06_sequential_workflows.md` |
| Chat memory | `examples/07_chat_memory/` | `docs/concepts/07_chat_memory.md` |
| Embeddings & vector stores | `examples/08_embeddings_vectorstores/` | `docs/concepts/08_embeddings_vectorstores.md` |
| RAG | `examples/09_rag/` | `docs/concepts/09_rag.md` |
| Agents | `examples/10_agents/` | `docs/concepts/10_agents.md` |

Full index with links: `docs/index.md`.

## Notes on the port

- `mynotes/` in the source markdown was the fullest/most authoritative version of each topic; near-duplicate examples from the source's `chains/`, `prompt_templates/`, `deepseek/`, and `basics/` folders were folded in only where they added something new, otherwise dropped.
- The LangGraph rewrite dropped several more near-duplicates that taught the same technique twice (e.g. a CLI and a Streamlit version of the same chat-memory demo, three single-purpose RAG scripts that differed only in provider/loader) in favor of one parametrized or merged example each — see each folder's example count against the source notes for what was consolidated.
- A stray unused `numpy.f2py` import from the original ReAct agent notes was dropped (not carried over); this example doesn't need `numpy`.
- A few source sections referenced files that were never actually shipped (a legal-document corpus, a sample PDF, `job_listings.txt`) — those examples take a path as a CLI argument instead of hardcoding a missing filename; see the relevant script's docstring.

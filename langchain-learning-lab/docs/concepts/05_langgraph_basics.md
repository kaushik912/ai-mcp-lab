# LangGraph Fundamentals

LangGraph is the runtime the rest of this repo's multi-step examples are built on. Instead of chaining `Runnable`s with `|`, you describe a **graph**: a `State` shape, `Node`s that read and write it, and `Edge`s that say what runs next.

## State, nodes, edges

```python
from typing import TypedDict
from langgraph.graph import StateGraph, START, END

class State(TypedDict):
    text: str

def shout(state: State) -> dict:
    return {"text": state["text"].upper()}

builder = StateGraph(State)
builder.add_node("shout", shout)
builder.add_edge(START, "shout")
builder.add_edge("shout", END)

graph = builder.compile()
graph.invoke({"text": "hello"})  # {"text": "HELLO"}
```

- A node is a plain function: state in, a **partial update** out. LangGraph merges what you return into the existing state — you don't need to return the whole thing back.
- `START`/`END` are fixed markers for the graph's entry and exit points.
- `builder.compile()` turns the declarative node/edge spec into something you can actually `.invoke()`.

## Reducers: overwrite vs. accumulate

By default, a field you return replaces the old value. To *accumulate* into a list instead (useful for logs, message history, search results from parallel branches), annotate the field with a reducer:

```python
from operator import add
from typing import Annotated

class State(TypedDict):
    text: str                       # overwritten by each node
    log: Annotated[list[str], add]  # appended to by each node
```

Now a node returning `{"log": ["did a thing"]}` appends to the existing log instead of replacing it. `langgraph.graph.message.add_messages` is the same idea specialized for chat messages (see `docs/concepts/07_chat_memory.md`).

See `examples/05_langgraph_basics/graph_fundamentals_demo.py` for a runnable version of this (no LLM/API key needed).

## Conditional edges

A plain edge always goes to the same next node. A **conditional edge** picks the next node based on state, via a routing function that returns a string key:

```python
from typing import Literal

def route(state: State) -> Literal["billing", "technical", "general"]:
    return state["category"]

builder.add_conditional_edges(
    "classify",
    route,
    {"billing": "billing", "technical": "technical", "general": "general"},
)
```

This is LangGraph's answer to what used to require a `RouterChain`/`MultiPromptChain` in old LangChain — routing is just a Python function.

See `examples/05_langgraph_basics/conditional_routing_demo.py`.

## Where this shows up later

Every other multi-step example in this repo is a `StateGraph` under the hood: `docs/concepts/06_sequential_workflows.md` (nodes replace `LCEL` chain steps), `docs/concepts/07_chat_memory.md` (a checkpointer persists state across turns), `docs/concepts/09_rag.md` (retrieve/generate nodes), and `docs/concepts/10_agents.md` (`create_agent` compiles to a graph internally).

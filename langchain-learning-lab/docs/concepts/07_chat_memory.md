# Chat Memory

`ChatPromptTemplate` still defines roles (system, human, ai) the same way it always did:

```python
prompt_template = ChatPromptTemplate.from_messages([
    ("system", "You are an Agile Coach. Answer any questions related to the agile process."),
    ("human", "{input}")
])
```

**Without memory**, a follow-up question ("summarize it in 2 points") won't work — the model has no idea what "it" refers to, because chat history isn't tracked.

## Memory via checkpointing, not `RunnableWithMessageHistory`

Old LangChain tracked history with `ChatMessageHistory`/`StreamlitChatMessageHistory` objects wrapped by `RunnableWithMessageHistory`, keyed by a `session_id`. LangGraph folds this into the graph itself: compile with a **checkpointer**, and the graph's own state — including a message list — persists automatically per **thread_id**.

```python
from typing import Annotated, TypedDict
from langchain_core.messages import BaseMessage
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages

class ChatState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]

def respond(state: ChatState) -> dict:
    response = llm.invoke(state["messages"])
    return {"messages": [response]}

builder = StateGraph(ChatState)
builder.add_node("respond", respond)
builder.add_edge(START, "respond")
builder.add_edge("respond", END)
graph = builder.compile(checkpointer=MemorySaver())

config = {"configurable": {"thread_id": "abc123"}}
graph.invoke({"messages": [HumanMessage("hi")]}, config)
```

- `Annotated[list[BaseMessage], add_messages]` is a reducer (see `docs/concepts/05_langgraph_basics.md`) specialized for chat messages — it appends new messages instead of overwriting the list, and merges message edits/deletes by id.
- Every `graph.invoke(..., config)` call with the same `thread_id` sees the full prior history; a different `thread_id` starts a fresh conversation. This replaces the old `session_id` concept.
- `MemorySaver` keeps state in memory (process lifetime only — fine for local scripts and demos). Swap in `SqliteSaver`/`PostgresSaver` for anything that needs to survive a restart.
- To seed a system prompt before the first user turn without triggering an LLM call, use `graph.update_state(config, {"messages": [SystemMessage(...)]})`.

## Example in this repo

`examples/07_chat_memory/st_chat_memory_app.py` — Streamlit chat UI backed by this pattern.

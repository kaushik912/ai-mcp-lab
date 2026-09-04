"""Chat memory via LangGraph checkpointing: a StateGraph compiled with a MemorySaver
checkpointer persists the conversation per thread_id, replacing the old
RunnableWithMessageHistory + StreamlitChatMessageHistory pattern.
Run with: streamlit run examples/07_chat_memory/st_chat_memory_app.py
"""
from typing import Annotated, TypedDict

import streamlit as st
from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages

from common.models import get_chat_model
from common.prompts.chat_memory import AGILE_COACH_SYSTEM_PROMPT

llm = get_chat_model()


class ChatState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]


def respond(state: ChatState) -> dict:
    response = llm.invoke(state["messages"])
    return {"messages": [response]}


@st.cache_resource
def get_graph():
    builder = StateGraph(ChatState)
    builder.add_node("respond", respond)
    builder.add_edge(START, "respond")
    builder.add_edge("respond", END)
    return builder.compile(checkpointer=MemorySaver())


graph = get_graph()
config = {"configurable": {"thread_id": "agile-coach-session"}}

st.title("Agile Guide")

if "history" not in st.session_state:
    st.session_state.history = []
    graph.update_state(config, {"messages": [SystemMessage(AGILE_COACH_SYSTEM_PROMPT)]})

for message in st.session_state.history:
    st.chat_message(message["role"]).write(message["text"])

user_input = st.chat_input("Ask an agile question")

if user_input:
    st.session_state.history.append({"role": "human", "text": user_input})
    st.chat_message("human").write(user_input)

    result = graph.invoke({"messages": [HumanMessage(user_input)]}, config)
    reply = result["messages"][-1].text

    st.session_state.history.append({"role": "ai", "text": reply})
    st.chat_message("ai").write(reply)

"""Two-step sequential workflow built as a LangGraph: topic -> title -> speech.
Each node constructs its own model, so nothing stops write_title and write_speech
from using different models/providers if you want a stronger model for the fuller
speech body - here both default to the same fast/free-tier-friendly model.
Run with: streamlit run examples/06_sequential_workflows/st_speech_generator_app.py
"""
from typing import TypedDict

import streamlit as st
from langchain_core.prompts import PromptTemplate
from langgraph.graph import END, START, StateGraph

from common.models import get_chat_model
from common.prompts.sequential_workflows import SPEECH_BODY_TEMPLATE, SPEECH_TITLE_TEMPLATE

title_llm = get_chat_model()
speech_llm = get_chat_model()

title_prompt = PromptTemplate.from_template(SPEECH_TITLE_TEMPLATE)
speech_prompt = PromptTemplate.from_template(SPEECH_BODY_TEMPLATE)


class SpeechState(TypedDict):
    topic: str
    title: str
    speech: str


def write_title(state: SpeechState) -> dict:
    response = title_llm.invoke(title_prompt.invoke({"topic": state["topic"]}))
    return {"title": response.text}


def write_speech(state: SpeechState) -> dict:
    response = speech_llm.invoke(speech_prompt.invoke({"title": state["title"]}))
    return {"speech": response.text}


def build_graph():
    builder = StateGraph(SpeechState)
    builder.add_node("write_title", write_title)
    builder.add_node("write_speech", write_speech)
    builder.add_edge(START, "write_title")
    builder.add_edge("write_title", "write_speech")
    builder.add_edge("write_speech", END)
    return builder.compile()


graph = build_graph()

st.title("Speech Generator")

topic = st.text_input("Enter the topic:")

if topic:
    result = graph.invoke({"topic": topic, "title": "", "speech": ""})
    st.subheader(result["title"])
    st.write(result["speech"])

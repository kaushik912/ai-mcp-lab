"""Debugging a LangGraph app: the old `langchain.globals.set_debug()` toggle is gone
in LangChain v1 - the modern equivalent is the `stream_mode` argument on a compiled
graph's `.stream()`. This runs the same tiny two-node graph three times, once per
mode, so you can see what each one actually reports:
  - "values":  the full state snapshot after every node
  - "updates": only the delta each node returned
  - "debug":   framework-internal task/task_result events (the most detailed)
Run with: python examples/02_debugging_and_streamlit/debug_tools_demo.py
"""
from typing import TypedDict

from langgraph.graph import END, START, StateGraph

from common.models import get_chat_model


class State(TypedDict):
    question: str
    answer: str


def answer_node(state: State) -> dict:
    llm = get_chat_model()
    response = llm.invoke(f"Answer in one short sentence: {state['question']}")
    return {"answer": response.text}


def build_graph():
    builder = StateGraph(State)
    builder.add_node("answer", answer_node)
    builder.add_edge(START, "answer")
    builder.add_edge("answer", END)
    return builder.compile()


def main():
    graph = build_graph()
    question = input("Enter the question: ")

    for mode in ("values", "updates", "debug"):
        print(f"\n--- stream_mode={mode!r} ---")
        for chunk in graph.stream({"question": question, "answer": ""}, stream_mode=mode):
            print(chunk)


if __name__ == "__main__":
    main()

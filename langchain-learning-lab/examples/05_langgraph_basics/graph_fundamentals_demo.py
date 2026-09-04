"""LangGraph fundamentals: state, nodes, edges, and reducers - no LLM calls, so this
runs instantly with no API key. A tiny "text pipeline" graph passes a string through
three nodes, each returning only the state keys it changed:
  - `text`: overwritten by each node (default reducer - last write wins)
  - `log`: accumulated across nodes via `Annotated[list[str], operator.add]`
Run with: python examples/05_langgraph_basics/graph_fundamentals_demo.py
"""
from operator import add
from typing import Annotated, TypedDict

from langgraph.graph import END, START, StateGraph

from common.env import load_env

load_env()


class PipelineState(TypedDict):
    text: str
    log: Annotated[list[str], add]


def count_words(state: PipelineState) -> dict:
    word_count = len(state["text"].split())
    return {"log": [f"count_words: {word_count} word(s)"]}


def shout(state: PipelineState) -> dict:
    shouted = state["text"].upper()
    return {"text": shouted, "log": [f"shout: -> {shouted!r}"]}


def reverse_text(state: PipelineState) -> dict:
    reversed_text = state["text"][::-1]
    return {"text": reversed_text, "log": [f"reverse_text: -> {reversed_text!r}"]}


def build_graph():
    builder = StateGraph(PipelineState)
    builder.add_node("count_words", count_words)
    builder.add_node("shout", shout)
    builder.add_node("reverse_text", reverse_text)

    builder.add_edge(START, "count_words")
    builder.add_edge("count_words", "shout")
    builder.add_edge("shout", "reverse_text")
    builder.add_edge("reverse_text", END)

    return builder.compile()


def main():
    graph = build_graph()
    text = input("Enter some text: ")

    result = graph.invoke({"text": text, "log": []})

    print(f"\nFinal text: {result['text']!r}")
    print("Log (accumulated across every node):")
    for entry in result["log"]:
        print(f"  - {entry}")


if __name__ == "__main__":
    main()

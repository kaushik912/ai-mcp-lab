"""LangGraph conditional edges: a support-ticket triage graph. A routing function
inspects the state and returns a string key; `add_conditional_edges` maps that key
to the next node to run. No LLM calls, so this runs instantly with no API key.
Run with: python examples/05_langgraph_basics/conditional_routing_demo.py
"""
from typing import Literal, TypedDict

from langgraph.graph import END, START, StateGraph

from common.env import load_env

load_env()

BILLING_KEYWORDS = ("refund", "charge", "invoice", "payment")
TECHNICAL_KEYWORDS = ("error", "bug", "crash", "broken")


class TicketState(TypedDict):
    message: str
    category: str
    reply: str


def classify(state: TicketState) -> dict:
    text = state["message"].lower()
    if any(word in text for word in BILLING_KEYWORDS):
        return {"category": "billing"}
    if any(word in text for word in TECHNICAL_KEYWORDS):
        return {"category": "technical"}
    return {"category": "general"}


def route_by_category(state: TicketState) -> Literal["billing", "technical", "general"]:
    return state["category"]


def handle_billing(state: TicketState) -> dict:
    return {"reply": "Routed to billing support - a specialist will review your charge."}


def handle_technical(state: TicketState) -> dict:
    return {"reply": "Routed to technical support - please attach any error logs."}


def handle_general(state: TicketState) -> dict:
    return {"reply": "Routed to general support - we'll get back to you shortly."}


def build_graph():
    builder = StateGraph(TicketState)
    builder.add_node("classify", classify)
    builder.add_node("billing", handle_billing)
    builder.add_node("technical", handle_technical)
    builder.add_node("general", handle_general)

    builder.add_edge(START, "classify")
    builder.add_conditional_edges(
        "classify",
        route_by_category,
        {"billing": "billing", "technical": "technical", "general": "general"},
    )
    builder.add_edge("billing", END)
    builder.add_edge("technical", END)
    builder.add_edge("general", END)

    return builder.compile()


def main():
    graph = build_graph()
    message = input("Describe your issue: ")

    result = graph.invoke({"message": message, "category": "", "reply": ""})

    print(f"\nCategory: {result['category']}")
    print(f"Reply: {result['reply']}")


if __name__ == "__main__":
    main()

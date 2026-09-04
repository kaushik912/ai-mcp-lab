"""Marketing Email Generator: product_name + features -> subject line -> full email
(which also needs product_name + target_audience). Built as a LangGraph so the two
independent inputs (features, target_audience) merge into state naturally instead of
needing RunnablePassthrough.assign; the email node uses with_structured_output for a
typed result instead of a JSON-parsing prompt instruction.
Run with: streamlit run examples/06_sequential_workflows/st_marketing_email_generator_app.py
"""
from typing import TypedDict

import streamlit as st
from langchain_core.prompts import PromptTemplate
from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, Field

from common.models import get_chat_model
from common.prompts.sequential_workflows import EMAIL_BODY_TEMPLATE, EMAIL_SUBJECT_TEMPLATE

llm = get_chat_model()

subject_prompt = PromptTemplate.from_template(EMAIL_SUBJECT_TEMPLATE)
email_prompt = PromptTemplate.from_template(EMAIL_BODY_TEMPLATE)


class MarketingEmail(BaseModel):
    subject: str = Field(description="the marketing email's subject line")
    audience: str = Field(description="the target audience the email is written for")
    email: str = Field(description="the full email body")


class EmailState(TypedDict):
    product_name: str
    features: str
    target_audience: str
    subject_line: str
    email: MarketingEmail | None


def write_subject_line(state: EmailState) -> dict:
    response = llm.invoke(
        subject_prompt.invoke({"product_name": state["product_name"], "features": state["features"]})
    )
    return {"subject_line": response.text}


def write_email(state: EmailState) -> dict:
    structured_llm = llm.with_structured_output(MarketingEmail)
    email = structured_llm.invoke(
        email_prompt.invoke(
            {
                "product_name": state["product_name"],
                "subject_line": state["subject_line"],
                "target_audience": state["target_audience"],
            }
        )
    )
    return {"email": email}


def build_graph():
    builder = StateGraph(EmailState)
    builder.add_node("write_subject_line", write_subject_line)
    builder.add_node("write_email", write_email)
    builder.add_edge(START, "write_subject_line")
    builder.add_edge("write_subject_line", "write_email")
    builder.add_edge("write_email", END)
    return builder.compile()


graph = build_graph()

st.title("Marketing Email Generator")

product_name = st.text_input("Input Product Name")
features = st.text_input("Input Product Features (comma-separated)")
target_audience = st.text_input("Input Target Audience")

if product_name and features and target_audience:
    result = graph.invoke(
        {
            "product_name": product_name,
            "features": features,
            "target_audience": target_audience,
            "subject_line": "",
            "email": None,
        }
    )
    st.write(result["email"].model_dump())

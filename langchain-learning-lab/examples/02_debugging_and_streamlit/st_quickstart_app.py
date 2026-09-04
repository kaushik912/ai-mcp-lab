"""Minimal LangChain + Streamlit app.
Run with: streamlit run examples/02_debugging_and_streamlit/st_quickstart_app.py
"""
import streamlit as st

from common.models import get_chat_model

llm = get_chat_model()

st.title("Ask Anything")

question = st.text_input("Enter the question:")

if question:
    response = llm.invoke(question)
    st.write(response.text)

"""PromptTemplate basics: dynamic, reusable prompts with input_variables.
Run with: streamlit run examples/03_prompt_templates/st_cuisine_info_app.py
"""
import streamlit as st
from langchain_core.prompts import PromptTemplate

from common.models import get_chat_model
from common.prompts.prompt_templates import CUISINE_TEMPLATE

llm = get_chat_model()

prompt_template = PromptTemplate(
    input_variables=["country", "no_of_paras", "language"],
    template=CUISINE_TEMPLATE,
)

st.title("Cuisine Info")

country = st.text_input("Enter the country:")
no_of_paras = st.number_input("Enter the number of paras", min_value=1, max_value=5)
language = st.text_input("Enter the language:")

if country:
    prompt = prompt_template.invoke(
        {"country": country, "no_of_paras": no_of_paras, "language": language}
    )
    response = llm.invoke(prompt)
    st.write(response.text)

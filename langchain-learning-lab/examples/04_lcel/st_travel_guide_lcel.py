"""Travel Guide app built with LCEL's `|` pipe operator, composing a prompt template
directly with the model instead of formatting the prompt and calling invoke() by hand.
Run with: streamlit run examples/04_lcel/st_travel_guide_lcel.py
"""
import streamlit as st
from langchain_core.prompts import PromptTemplate

from common.models import get_chat_model
from common.prompts.prompt_templates import TRAVEL_GUIDE_TEMPLATE

llm = get_chat_model()

prompt_template = PromptTemplate(
    input_variables=["city", "month", "language", "budget"],
    template=TRAVEL_GUIDE_TEMPLATE,
)

st.title("Travel Guide")

city = st.text_input("Enter the city:")
month = st.text_input("Enter the month of travel:")
language = st.text_input("Enter the language:")
budget = st.selectbox("Travel Budget", ["Low", "Medium", "High"])

chain = prompt_template | llm

if city and month and language and budget:
    response = chain.invoke(
        {"city": city, "month": month, "language": language, "budget": budget}
    )
    st.write(response.text)

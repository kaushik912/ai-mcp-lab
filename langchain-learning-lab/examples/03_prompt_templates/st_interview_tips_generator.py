"""PromptTemplate with role-setting ("You are a career coach") for tailored tone.
Run with: streamlit run examples/03_prompt_templates/st_interview_tips_generator.py
"""
import streamlit as st
from langchain_core.prompts import PromptTemplate

from common.models import get_chat_model
from common.prompts.prompt_templates import INTERVIEW_TIPS_TEMPLATE

llm = get_chat_model()

prompt_template = PromptTemplate(
    input_variables=["company", "position", "strengths", "weaknesses"],
    template=INTERVIEW_TIPS_TEMPLATE,
)

st.title("Interview Tips Generator")

company = st.text_input("Company Name")
position = st.text_input("Position Title")
strengths = st.text_area("Your Strengths", height=100)
weaknesses = st.text_area("Your Weaknesses", height=100)

if company and position and strengths and weaknesses:
    prompt = prompt_template.invoke(
        {
            "company": company,
            "position": position,
            "strengths": strengths,
            "weaknesses": weaknesses,
        }
    )
    response = llm.invoke(prompt)
    st.write(response.text)

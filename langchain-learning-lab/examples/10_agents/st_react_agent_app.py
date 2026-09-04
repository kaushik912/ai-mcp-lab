"""Tool-using agent: create_agent wires the model plus Wikipedia + DuckDuckGo search
tools together and runs the reason -> pick a tool -> act -> observe loop itself.
Run with: streamlit run examples/10_agents/st_react_agent_app.py
Note: wikipedia/duckduckgo-search may be blocked on some corporate networks.
"""
import streamlit as st
from langchain.agents import create_agent
from langchain_community.tools import DuckDuckGoSearchRun, WikipediaQueryRun
from langchain_community.utilities import WikipediaAPIWrapper

from common.models import get_chat_model

llm = get_chat_model()
tools = [WikipediaQueryRun(api_wrapper=WikipediaAPIWrapper()), DuckDuckGoSearchRun()]
agent = create_agent(model=llm, tools=tools)

st.title("This is an AI agent")
task = st.text_input("Assign me a task")
if task:
    result = agent.invoke({"messages": [{"role": "user", "content": task}]})
    st.write(result["messages"][-1].text)

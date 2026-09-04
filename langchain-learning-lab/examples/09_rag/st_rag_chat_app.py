"""RAG + chat history as a LangGraph: a "contextualize" node rewrites follow-up
questions ("summarize that") into standalone queries using the conversation so far,
so retrieval still works even when the latest message alone is ambiguous. Chat
history itself comes from a MemorySaver checkpointer keyed by thread_id, replacing
the old create_history_aware_retriever + RunnableWithMessageHistory pattern.

Run with: streamlit run examples/09_rag/st_rag_chat_app.py
"""
from typing import Annotated, TypedDict

import streamlit as st
from langchain_chroma import Chroma
from langchain_community.document_loaders import TextLoader
from langchain_core.documents import Document
from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage
from langchain_core.prompts import ChatPromptTemplate
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages

from common.env import data_path
from common.models import get_chat_model, get_embeddings_model
from common.prompts.rag import CONTEXTUALIZE_QUESTION_SYSTEM_PROMPT, RAG_SYSTEM_PROMPT

MODEL_CHOICES = ("openrouter","gemini", "openai")


def build_chat_and_embeddings(choice: str):
    return get_chat_model(choice), get_embeddings_model(choice)


class RAGChatState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    context: list[Document]
    standalone_query: str


def build_graph(llm, vector_store):
    contextualize_prompt = ChatPromptTemplate.from_messages(
        [("system", CONTEXTUALIZE_QUESTION_SYSTEM_PROMPT), ("placeholder", "{messages}")]
    )

    def contextualize(state: RAGChatState) -> dict:
        if len(state["messages"]) <= 1:
            return {"standalone_query": state["messages"][-1].text}
        response = llm.invoke(contextualize_prompt.invoke({"messages": state["messages"]}))
        return {"standalone_query": response.text}

    def retrieve(state: RAGChatState) -> dict:
        return {"context": vector_store.similarity_search(state["standalone_query"], k=4)}

    def generate(state: RAGChatState) -> dict:
        context_text = "\n\n".join(doc.page_content for doc in state["context"])
        system_message = SystemMessage(RAG_SYSTEM_PROMPT.format(context=context_text))
        response = llm.invoke([system_message, *state["messages"]])
        return {"messages": [response]}

    builder = StateGraph(RAGChatState)
    builder.add_node("contextualize", contextualize)
    builder.add_node("retrieve", retrieve)
    builder.add_node("generate", generate)
    builder.add_edge(START, "contextualize")
    builder.add_edge("contextualize", "retrieve")
    builder.add_edge("retrieve", "generate")
    builder.add_edge("generate", END)
    return builder.compile(checkpointer=MemorySaver())


@st.cache_resource
def get_graph(model_choice: str):
    llm, embeddings = build_chat_and_embeddings(model_choice)

    loader = TextLoader(data_path("product-data.txt"))
    documents = loader.load()
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
    chunks = text_splitter.split_documents(documents)
    # Each provider's embeddings have a different vector dimension, and Chroma
    # reuses its default collection ("langchain") across calls within the same
    # process - without a per-provider name, switching the selectbox crashes
    # with a dimension mismatch against whichever provider built it first.
    vector_store = Chroma.from_documents(chunks, embeddings, collection_name=f"rag-chat-{model_choice}")

    return build_graph(llm, vector_store)


st.title("Chat with Document")

model_choice = st.selectbox("Model", MODEL_CHOICES)
graph = get_graph(model_choice)
config = {"configurable": {"thread_id": f"rag-chat-{model_choice}"}}

if "history" not in st.session_state:
    st.session_state.history = []

for message in st.session_state.history:
    st.chat_message(message["role"]).write(message["text"])

question = st.chat_input("Your question")

if question:
    st.session_state.history.append({"role": "human", "text": question})
    st.chat_message("human").write(question)

    result = graph.invoke({"messages": [HumanMessage(question)], "context": [], "standalone_query": ""}, config)
    reply = result["messages"][-1].text

    st.session_state.history.append({"role": "ai", "text": reply})
    st.chat_message("ai").write(reply)

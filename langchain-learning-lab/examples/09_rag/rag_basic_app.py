"""Retrieval-Augmented Generation as a LangGraph: retrieve -> generate. Replaces the
old langchain.chains.create_retrieval_chain/create_stuff_documents_chain helpers with
two plain graph nodes. Parametrized over provider (chat + matching embeddings) and
source document (.txt or .pdf, loader picked by extension) so one script covers what
used to be three separate single-purpose scripts.
Run with: python examples/09_rag/rag_basic_app.py [--provider {gemini,openai,openrouter}] [--source <path>] [--query <text>]
"""
import argparse
from typing import TypedDict

from langchain_chroma import Chroma
from langchain_community.document_loaders import PyPDFLoader, TextLoader
from langchain_core.documents import Document
from langchain_core.prompts import ChatPromptTemplate
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langgraph.graph import END, START, StateGraph

from common.env import data_path
from common.models import get_chat_model, get_embeddings_model
from common.prompts.rag import RAG_SYSTEM_PROMPT


def build_models(provider: str):
    return get_chat_model(provider), get_embeddings_model(provider)


def load_and_chunk(path: str):
    loader = PyPDFLoader(path) if path.lower().endswith(".pdf") else TextLoader(path)
    documents = loader.load()
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
    return text_splitter.split_documents(documents)


class RAGState(TypedDict):
    query: str
    context: list[Document]
    answer: str


def build_graph(llm, vector_store):
    prompt_template = ChatPromptTemplate.from_messages(
        [("system", RAG_SYSTEM_PROMPT), ("human", "{question}")]
    )

    def retrieve(state: RAGState) -> dict:
        return {"context": vector_store.similarity_search(state["query"], k=4)}

    def generate(state: RAGState) -> dict:
        context_text = "\n\n".join(doc.page_content for doc in state["context"])
        prompt = prompt_template.invoke({"context": context_text, "question": state["query"]})
        response = llm.invoke(prompt)
        return {"answer": response.text}

    builder = StateGraph(RAGState)
    builder.add_node("retrieve", retrieve)
    builder.add_node("generate", generate)
    builder.add_edge(START, "retrieve")
    builder.add_edge("retrieve", "generate")
    builder.add_edge("generate", END)
    return builder.compile()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--provider", choices=["gemini", "openai", "openrouter"], default="openrouter")
    parser.add_argument("--source", default=str(data_path("product-data.txt")))
    parser.add_argument("--query", default=None)
    args = parser.parse_args()

    llm, embeddings = build_models(args.provider)
    chunks = load_and_chunk(args.source)
    vector_store = Chroma.from_documents(chunks, embeddings)

    graph = build_graph(llm, vector_store)

    query = args.query or input("Enter your question: ")
    result = graph.invoke({"query": query, "context": [], "answer": ""})
    print(result["answer"])


if __name__ == "__main__":
    main()

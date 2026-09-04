"""Chunk a text file, embed the chunks, store them in Chroma, and retrieve the most
semantically similar chunks for a query - no generation yet, just retrieval.
(FAISS works as a drop-in replacement for Chroma here: swap the `Chroma` import/class
for `langchain_community.vectorstores.FAISS` if you'd rather use that store.)
Run with: python examples/08_embeddings_vectorstores/chroma_vectorstore_demo.py <path-to-text-file> [--provider {gemini,openai,openrouter}]
"""
import argparse

from langchain_chroma import Chroma
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter

from common.models import get_embeddings_model


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("path", help="Path to a text file to index")
    parser.add_argument("--provider", choices=["gemini", "openai", "openrouter"], default="openrouter")
    args = parser.parse_args()

    embeddings = get_embeddings_model(args.provider)

    document = TextLoader(args.path).load()
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=200, chunk_overlap=10)
    chunks = text_splitter.split_documents(document)

    db = Chroma.from_documents(chunks, embeddings)
    retriever = db.as_retriever()

    text = input("Enter your query: ")
    docs = retriever.invoke(text)

    for doc in docs:
        print(doc.page_content)


if __name__ == "__main__":
    main()

"""Embeddings basics: embed_query, embed_documents, and comparing meaning via dot
product. Also shows the "manual" retrieval path (embed query yourself, then
similarity_search_by_vector) vs. letting a retriever do it - see
chroma_vectorstore_demo.py for the higher-level version.
Run with: python examples/08_embeddings_vectorstores/embeddings_similarity_demo.py [--provider {gemini,openai,openrouter}]
"""
import argparse

import numpy as np

from common.models import get_embeddings_model


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--provider", choices=["gemini", "openai", "openrouter"], default="openrouter")
    args = parser.parse_args()

    embeddings = get_embeddings_model(args.provider)

    input1 = input("Enter First Input: ")
    input2 = input("Enter Second Input: ")

    response1 = embeddings.embed_query(input1)
    response2 = embeddings.embed_query(input2)

    similarity_score = np.dot(response1, response2)
    print(f"{similarity_score * 100:.2f}% similar")

    # embed_documents batches multiple pieces of text at once
    docs = embeddings.embed_documents(
        [
            "I love playing video games",
            "I am going to the movie",
            "I love coding",
            "Hello World!",
        ]
    )
    print(f"Embedded {len(docs)} documents, each of dimension {len(docs[0])}")


if __name__ == "__main__":
    main()

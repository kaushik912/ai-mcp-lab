"""Streaming a model's response, token by token, vs. accumulating it.
Run with: python examples/01_first_programs/streaming_demo.py
"""
from common.models import get_chat_model


def main():
    llm = get_chat_model()
    question = input("Enter the question: ")
    response = llm.stream(question)

    stream_output = False  # set True for real-time token-by-token printing

    if not stream_output:
        full_response = ""
        for chunk in response:
            full_response += chunk.text
        print(full_response)
    else:
        for chunk in response:
            print(chunk.text, end="", flush=True)


if __name__ == "__main__":
    main()

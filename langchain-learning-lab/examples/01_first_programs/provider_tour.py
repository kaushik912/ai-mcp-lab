"""Tour of swapping LLM providers with LangChain: only the model construction
changes, the rest of your code stays the same.
Run with: python examples/01_first_programs/provider_tour.py
"""
from common.models import get_chat_model

PROVIDERS = {
    "openai": lambda: get_chat_model("openai"),
    "gemini": lambda: get_chat_model("gemini"),
    "openrouter": lambda: get_chat_model("openrouter"),
}


def main():
    print(f"Providers available: {', '.join(PROVIDERS)}")
    choice = input("Pick a provider: ").strip().lower()
    if choice not in PROVIDERS:
        print(f"Unknown provider {choice!r}")
        return
    llm = PROVIDERS[choice]()
    question = input("Enter the question: ")
    response = llm.invoke(question)
    print(response.text)


if __name__ == "__main__":
    main()

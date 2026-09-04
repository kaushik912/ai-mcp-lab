"""Ask a question scoped to a topic of your choosing. Completed from a prompt-only
fragment in the original notes into a standalone runnable script.
Run with: python examples/03_prompt_templates/favorite_topic_prompt.py
"""
from langchain_core.prompts import PromptTemplate

from common.models import get_chat_model
from common.prompts.prompt_templates import TOPIC_QA_TEMPLATE

prompt_template = PromptTemplate(
    input_variables=["topic", "question"],
    template=TOPIC_QA_TEMPLATE,
)


def main():
    llm = get_chat_model()
    topic = input("Topic: ")
    question = input("Question: ")
    prompt = prompt_template.invoke({"topic": topic, "question": question})
    response = llm.invoke(prompt)
    print(response.text)


if __name__ == "__main__":
    main()

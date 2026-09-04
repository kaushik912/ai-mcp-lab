"""Prompt text for examples/09_rag."""

RAG_SYSTEM_PROMPT = """You are an assistant for answering questions.
Use the provided context to respond. If the answer isn't clear,
acknowledge that you don't know. Limit your response to three concise sentences.

Context:
{context}"""

CONTEXTUALIZE_QUESTION_SYSTEM_PROMPT = """Given the conversation so far and the latest
user question, rewrite the question as a standalone question that can be understood
without the earlier conversation. Do NOT answer the question - only rewrite it.
If it is already standalone, return it unchanged."""

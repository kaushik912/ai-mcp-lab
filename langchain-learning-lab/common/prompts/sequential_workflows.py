"""Prompt text for examples/06_sequential_workflows."""

SPEECH_TITLE_TEMPLATE = """You are an experienced speech writer.
You need to craft an impactful title for a speech
on the following topic: {topic}
Answer exactly with one title."""

SPEECH_BODY_TEMPLATE = """You need to write a powerful speech of 350 words
for the following title: {title}"""

EMAIL_SUBJECT_TEMPLATE = """You are an experienced marketing specialist.
Create a catchy subject line for a marketing
email promoting the following product: {product_name}.
Highlight these features: {features}.
Respond with only the subject line."""

EMAIL_BODY_TEMPLATE = """Write a marketing email of 300 words for the
product: {product_name}. Use the subject line: {subject_line}.
Tailor the message for the following target audience: {target_audience}."""

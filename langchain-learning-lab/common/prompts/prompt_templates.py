"""Prompt text for examples/03_prompt_templates and examples/04_lcel."""

CUISINE_TEMPLATE = """You are an expert in traditional cuisines.
You provide information about a specific dish from a specific country.
Avoid giving information about fictional places. If the country is fictional
or non-existent answer: I don't know.
Answer the question: What is the traditional cuisine of {country}?
Answer in {no_of_paras} short paras in {language}
"""

TOPIC_QA_TEMPLATE = """You are a helpful assistant with expertise in {topic}.
Answer the following question concisely:
Question: {question}
"""

INTERVIEW_TIPS_TEMPLATE = """You are a career coach. Provide tailored interview tips for the
position of {position} at {company}.
Highlight your strengths in {strengths} and prepare for questions
about your weaknesses such as {weaknesses}."""

TRAVEL_GUIDE_TEMPLATE = """Welcome to the {city} travel guide!
If you're visiting in {month}, here's what you can do:
1. Must-visit attractions.
2. Local cuisine you must try.
3. Useful phrases in {language}.
4. Tips for traveling on a {budget} budget.
Enjoy your trip!
"""

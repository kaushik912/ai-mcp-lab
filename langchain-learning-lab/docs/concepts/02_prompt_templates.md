# Prompt Templates

`PromptTemplate` makes prompts dynamic and reusable, using two arguments:
- **`input_variables`** — the placeholders your prompt accepts (an array, e.g. `["country", "no_of_paras", "language"]`), filled from user input.
- **`template`** — the prompt text, using `{}` to mark where variables get injected.

Analogy: it works like Python's f-strings (`f"Hello {name}"`) but with validation — LangChain ensures all input variables are defined before the LLM call.

```python
name = "Kaushik"
salutation = f"Hello {name}"
```

## Guardrails

Without constraints, a model asked "What's the traditional cuisine on Mars?" might invent an answer. Add a rule directly in the template:

```
Avoid giving information about fictional places.
If the country is fictional or non-existent answer: I don't know.
```

That's prompt engineering in action.

## Steps to use a PromptTemplate

1. Initialize the LLM.
2. Create the template: `PromptTemplate(input_variables=[...], template="...")`.
3. Collect inputs (Streamlit, CLI, etc.).
4. Build a prompt value: `prompt_template.invoke({...})` — returns a `PromptValue` with inputs injected (prefer this over the older `.format()`, which just returns a bare string).
5. Invoke the LLM: `llm.invoke(prompt_value)` — chat models accept a `PromptValue` directly.
6. Display the output via `response.text` (LangChain v1's message content is a list of content blocks, not a plain string — `.text` gives you the plain-text view regardless of provider).

## Examples in this repo

- `examples/03_prompt_templates/st_cuisine_info_app.py` — Cuisine Info app with guardrails.
- `examples/03_prompt_templates/st_interview_tips_generator.py` — role-setting ("You are a career coach") to shape tone.
- `examples/03_prompt_templates/favorite_topic_prompt.py` — minimal topic+question template.
- `examples/04_lcel/st_travel_guide_lcel.py` — multi-variable template + `st.selectbox` for constrained choices (avoids typos/unpredictable input compared to free text), composed with LCEL (see `docs/concepts/03_lcel.md`).

Prompt text for these examples lives in `common/prompts/`, not inline in the scripts — keeps the templates easy to scan and reuse.

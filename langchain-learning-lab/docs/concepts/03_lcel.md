# LCEL (LangChain Expression Language)

LCEL lets you compose LangChain components using the `|` (pipe) operator:

```python
chain = prompt_template | llm
```

The output of `PromptTemplate` becomes the input for the LLM. `chain.invoke()` takes a dictionary of key–value pairs matching the template's input variables — keys must match the variable names exactly.

```python
response = chain.invoke({"city": "Paris", "month": "June", "language": "French", "budget": "Medium"})
```

## Simple Sequential Chain

A pipeline where the output of one chain becomes the input of the next: `Chain1 | Chain2`.

## Beyond a simple two-step chain

LCEL is great for a prompt-then-model pipeline like the one above. Once a workflow needs multiple steps with independent inputs, branching, or persisted state, this repo switches to LangGraph's `StateGraph` instead — see `docs/concepts/05_langgraph_basics.md` and `docs/concepts/06_sequential_workflows.md` for that pattern and why it replaced the old multi-input LCEL approach.

## Example in this repo

`examples/04_lcel/st_travel_guide_lcel.py` — the Travel Guide app built with `chain = prompt_template | llm` instead of calling `llm.invoke(prompt.invoke(...))` in two separate steps.

## Key takeaways

- LCEL (`|`) composes chains declaratively.
- A Simple Sequential Chain passes outputs directly between steps.
- `chain.invoke()` always takes a dictionary — like a map in JS/Java.

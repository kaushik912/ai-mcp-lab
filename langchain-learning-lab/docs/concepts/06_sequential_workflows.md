# Sequential Workflows

A sequential workflow connects multiple LLM calls into one logical flow: the output of one step becomes the input to the next. Previously this repo built these with LCEL chains (`prompt | llm`) glued together with lambdas; now each step is a `StateGraph` node instead — see `docs/concepts/05_langgraph_basics.md` for the node/edge basics.

## Single-input step (topic -> title -> speech)

```python
class SpeechState(TypedDict):
    topic: str
    title: str
    speech: str

def write_title(state: SpeechState) -> dict:
    response = title_llm.invoke(title_prompt.invoke({"topic": state["topic"]}))
    return {"title": response.text}

def write_speech(state: SpeechState) -> dict:
    response = speech_llm.invoke(speech_prompt.invoke({"title": state["title"]}))
    return {"speech": response.text}
```

`write_title`'s output (`state["title"]`) flows into `write_speech` because they share the same graph state — no `StrOutputParser` + lambda plumbing needed to pass a value between steps, and each node can use a different model (`title_llm`/`speech_llm` are separate objects - nothing stops them from being different models or providers, see `examples/06_sequential_workflows/st_speech_generator_app.py`).

## Multiple independent inputs

The old LCEL version of this repo had a documented pitfall: a later step needing both a prior step's output *and* a separate piece of user input (e.g. `subject_line` from step 1, plus `target_audience` typed by the user) required either a fragile closure over a Streamlit-page variable, or `RunnablePassthrough.assign` to merge values into the chain's input dict.

With a `StateGraph`, this isn't a special case at all - every field the graph needs is just a key in `State`, written by whichever node produces it and read by whichever node needs it:

```python
class EmailState(TypedDict):
    product_name: str
    features: str
    target_audience: str
    subject_line: str
    email: MarketingEmail | None
```

`write_subject_line` fills `subject_line`; `write_email` reads `product_name`, `subject_line`, *and* `target_audience` together. No merge step required - they were all in state from the initial `graph.invoke({...})` call or a prior node's return value.

## Structured output instead of JSON-parsing prompts

Rather than instructing the model to "format the output as JSON with keys X, Y, Z" and parsing the result with `JsonOutputParser`, use `model.with_structured_output(SomePydanticModel)` for a typed, validated result:

```python
class MarketingEmail(BaseModel):
    subject: str
    audience: str
    email: str

structured_llm = llm.with_structured_output(MarketingEmail)
email: MarketingEmail = structured_llm.invoke(prompt)
```

See `examples/06_sequential_workflows/st_marketing_email_generator_app.py`.

## Key takeaways

- Each node reads the state fields it needs and returns only the ones it changes.
- Independent inputs don't need special merging - they're just state fields.
- Prefer `with_structured_output(PydanticModel)` over a JSON-formatting instruction + `JsonOutputParser`.
- Different nodes can call different models/providers in the same graph.

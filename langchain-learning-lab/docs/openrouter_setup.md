Set up **ChatOpenRouter** to use OpenRouter's routing layer and 400+ models in a LangChain application:

### 1. Installation & Authentication

* **Python**: Install the official package using `pip install -U langchain-openrouter`.
* **TypeScript**: Install the package using `npm install @langchain/openrouter`.
* **API Key**: Set your API key in your environment variables as `OPENROUTER_API_KEY="sk-or-..."`. `ChatOpenRouter` automatically reads this variable.

---

### 2. Basic Initialization

**Python Implementation**

```python
from langchain_openrouter import ChatOpenRouter

model = ChatOpenRouter(
    model="anthropic/claude-sonnet-4.5",  # provider/model format
    temperature=0,
    max_tokens=1024,
    max_retries=2,
)

response = model.invoke("Summarize this text.")
print(response.content)

```

**TypeScript Implementation**

```typescript
import { ChatOpenRouter } from '@langchain/openrouter';

const model = new ChatOpenRouter('anthropic/claude-sonnet-4.5', {
  temperature: 0.8,
});

const response = await model.invoke('Summarize this text.');
console.log(response.content);

```

---

### 3. Key Capabilities & Custom Configurations

* **Model Swapping**: Specify models using the `provider/model` slug format (e.g., `openai/gpt-5-mini` or `deepseek/deepseek-r1`).
* **Provider Routing & Failover**: Steer provider preference, routing behaviors, or fallbacks via `openrouter_provider`:
```python
model = ChatOpenRouter(
    model="anthropic/claude-sonnet-4.5",
    openrouter_provider={
        "order": ["Anthropic", "Google"],
        "allow_fallbacks": True,
        "data_collection": "deny",
        "sort": "throughput"
    }
)

```


* **Model Fallbacks**: Pass a list of backup models via `model_kwargs` so requests fail over across models if the primary is unavailable:
```python
model = ChatOpenRouter(
    model="anthropic/claude-sonnet-4.5",
    route="fallback",
    model_kwargs={
        "models": [
            "anthropic/claude-sonnet-4.5",
            "openai/gpt-5-mini",
            "google/gemini-3-flash-preview"
        ]
    }
)

```


* **Tool Calling & Structured Output**: Bind Pydantic tools with `model.bind_tools([...], strict=True)` or output structured data using `model.with_structured_output(Schema, method="json_schema")`.
* **Reasoning Budget**: Configure thinking effort for supported models using `reasoning={"effort": "high"}`.

> **Note for Older Codebases:** If using an older version of LangChain that lacks the dedicated package, you can configure `ChatOpenAI` by overriding its `base_url` to `[https://openrouter.ai/api/v1](https://openrouter.ai/api/v1)` along with your OpenRouter key. However, `langchain-openrouter` is the recommended path for native support.
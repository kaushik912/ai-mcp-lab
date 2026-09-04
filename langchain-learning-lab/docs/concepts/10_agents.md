# AI Agents

An AI Agent combines LLM reasoning with tools that let it take actions and fetch data — a digital assistant that thinks *and* acts.

## The ReAct loop

1. **Reasoning** — the LLM interprets the task.
2. **Tool Selection** — checks which tools are available (web search, Wikipedia, etc.).
3. **Action** — picks a tool and executes it.
4. **Observation** — reviews the tool's output (doesn't blindly trust it).
5. **Re-evaluation** — satisfied → produce the final answer; not satisfied → try another tool or rephrase.
6. **Completion** — shares the final answer.

This "Reason → Act → Observe → Reason again" cycle is what gives the ReAct agent its name.

## Building one with `create_agent`

The old `AgentExecutor` + `create_react_agent` + `langchain.hub.pull(...)` + `load_tools(...)` pattern is gone. `langchain.agents.create_agent` replaces all of it — it builds a small LangGraph internally and runs the reasoning-action loop for you:

```python
from langchain.agents import create_agent
from langchain_community.tools import DuckDuckGoSearchRun, WikipediaQueryRun
from langchain_community.utilities import WikipediaAPIWrapper

tools = [WikipediaQueryRun(api_wrapper=WikipediaAPIWrapper()), DuckDuckGoSearchRun()]
agent = create_agent(model=llm, tools=tools)

result = agent.invoke({"messages": [{"role": "user", "content": task}]})
print(result["messages"][-1].text)
```

No prompt needs pulling from LangChain Hub — `create_agent` has sensible built-in tool-calling behavior, and you can pass `system_prompt="..."` if you want to steer it. Requires the `wikipedia` and `ddgs` packages; both tools may be blocked on some corporate networks (try Google Colab as a fallback in that case).

See `examples/10_agents/st_react_agent_app.py`.

> Note: the original notes' "Final Code" block for this example had a stray `from numpy.f2py.crackfortran import verbose` import — an IDE autocomplete artifact, unused and unrelated to the example. It's dropped in the ported version; `numpy` isn't a dependency this example actually needs.

## Key takeaways

- Agents combine reasoning (LLMs) with action (tools).
- ReAct agents use a reasoning-action loop.
- `create_agent` (LangGraph-based) replaces `AgentExecutor` + `create_react_agent` + `hub.pull` — no separate prompt-fetching step needed.

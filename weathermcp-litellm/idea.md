# What I Built: A Provider-Agnostic AI Agent with Pluggable Tools

Here's a clean narrative you can walk someone through.

## The one-liner

> "I built a small web app where you type a question in the browser and an LLM answers it — but the LLM can *use tools* (fetch live weather, pull a quote) to do so. The catch: it's not tied to any single AI vendor, and new tools can be added without touching the app's code."

## The story / flow

**1. The problem.** A normal chatbot only knows what's in its training data. I wanted an *agent* — an LLM that can reason, call real tools to fetch live data, look at the result, and then answer. And I didn't want to lock the company into one AI provider (Anthropic, OpenAI, a local model…).

**2. The building blocks.** Three technologies, each doing one job:

| Piece | Role | Why |
|---|---|---|
| **LiteLLM** | The model layer | One unified interface to ~100 LLM providers. Swapping Claude → a local Ollama model → OpenRouter is a **one-line change**. No vendor lock-in. |
| **MCP** (Model Context Protocol) | The tool layer | An open standard for exposing tools to an LLM. Each tool lives in its own small **MCP server**, independent of the app. |
| **FastAPI** | The web layer | Serves the browser UI and a `/chat` endpoint that runs the agent. |

**3. How a request flows.**
```
Browser ("how's the weather?")
   │
   ▼
FastAPI /chat ──▶ Agent loop
                    │  1. asks the LLM (via LiteLLM)
                    │  2. LLM decides: "call get_weather(city='SF')"
                    │  3. agent runs the tool on its MCP server
                    │  4. feeds the result back to the LLM
                    │  5. LLM writes the final answer
                    ▼
              Answer back to the browser
```
That reason → act → observe → repeat cycle *is* the agent.

**4. The clever parts (the design wins).**

- **Tools are discovered, not hardcoded.** The app never mentions "weather." On startup it asks each MCP server "what tools do you have?" and advertises them to the LLM automatically. Add a tool to a server → the agent picks it up with **zero code changes**.
- **Many tool servers, one agent.** I ran two independent MCP servers (weather on :8001, quotes on :8002). The agent aggregates their tools and routes each call back to the server that owns it. This is how different teams could each contribute their own tools (a DB team, a Splunk team…) into one shared agent.
- **The agent loop is defensive.** Cheaper/smaller models tend to over-call tools and never stop. So the loop has a step budget, detects repeated calls, and forces a final text answer — robustness that doesn't depend on the model being smart.
- **Config lives outside the code.** Which AI model, which tool servers — all externalized (env vars + a `mcp_config.json`), so nothing is a hardcoded string in the app.

**5. Ready to grow up.** It runs locally now but is already **Dockerized** (each MCP server + the agent as separate containers), so it maps straight onto the eventual goal: an internal portal where anyone in the company can use the LLM + tools through a browser, instead of everyone running a tool on their own laptop.

## The 30-second version (if someone's in a hurry)

> "It's a browser-based AI assistant that can call live tools. I used **MCP** so each tool is a plug-in the agent auto-discovers, and **LiteLLM** so we're not locked to one AI vendor — I can point it at Claude or a free local model with one setting. It's Dockerized and structured so new tools are just config, not code — which is exactly what we'd need to turn it into a shared internal portal."

## The three names to drop, and why they matter

- **MCP** → *tools as pluggable, auto-discovered services* (the "USB-C for AI tools").
- **LiteLLM** → *no vendor lock-in; swap models freely, add cost controls later*.
- **Agent loop** → *the LLM doesn't just answer, it reasons and acts using those tools*.

---

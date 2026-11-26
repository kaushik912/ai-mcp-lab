"""
Provider-agnostic LiteLLM agent + web UI, with MULTI-server MCP discovery.

Flow:
  browser -> POST /chat -> agent loop -> LiteLLM (any model) + tools from
  every configured MCP server (weather @8001, quotes @8002, ...)

The frontend lives in ./static (index.html, styles.css, app.js) and is served
by FastAPI's StaticFiles -- keeping UI out of this backend module.

Run the MCP servers first, then:
  uvicorn agent:app --port 8000
Open http://127.0.0.1:8000
"""
import os
import json
import contextlib
from pathlib import Path

import litellm
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, FileResponse
from fastapi.staticfiles import StaticFiles
from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client

# ---- config (all swappable via env -> no vendor lock-in) --------------------
# BYOK: the visitor's OpenRouter key arrives per request in the X-OpenRouter-Key
# header and is passed straight to LiteLLM. It is never stored or logged.
MODEL = os.getenv("MODEL", "openrouter/~deepseek/deepseek-v4-flash-latest")
# e.g. MODEL="ollama/llama3.1"  or  "openrouter/meta-llama/llama-3.1-8b-instruct"
MAX_MESSAGE_CHARS = 500

litellm.suppress_debug_info = True  # keep provider debug dumps out of logs/errors

BASE_DIR = Path(__file__).parent
STATIC_DIR = BASE_DIR / "static"
MCP_CONFIG = Path(os.getenv("MCP_CONFIG", BASE_DIR / "mcp_config.json"))


def load_mcp_servers() -> dict[str, str]:
    """Return {server_name: url}. Source of truth is the JSON config file;
    the MCP_URLS env var overrides it for deployments (e.g. Docker service names)."""
    env = os.getenv("MCP_URLS")
    if env:  # deployment override: "url1,url2,..."
        return {f"server{i + 1}": u.strip()
                for i, u in enumerate(env.split(",")) if u.strip()}
    data = json.loads(MCP_CONFIG.read_text())
    return {name: cfg["url"] for name, cfg in data.get("mcpServers", {}).items()}


MCP_SERVERS = load_mcp_servers()

app = FastAPI()
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


def mcp_tool_to_openai(t):
    """MCP inputSchema is already JSON Schema -> drop it straight into OpenAI tool format."""
    return {
        "type": "function",
        "function": {
            "name": t.name,
            "description": t.description or "",
            "parameters": t.inputSchema,
        },
    }


async def connect_all(stack: contextlib.AsyncExitStack):
    """Open every MCP server and aggregate their tools.

    Returns:
      tools    -- combined tool list in OpenAI format (advertised to the LLM)
      registry -- {tool_name: session} so a call routes back to the right server
    """
    tools, registry = [], {}
    for name, url in MCP_SERVERS.items():
        # enter_async_context keeps each connection open until run_agent exits;
        # AsyncExitStack is how you manage a *dynamic* number of `async with`s.
        read, write, _ = await stack.enter_async_context(streamablehttp_client(url))
        session = await stack.enter_async_context(ClientSession(read, write))
        await session.initialize()
        listed = await session.list_tools()
        for t in listed.tools:
            if t.name in registry:  # name collision across servers -> keep first
                print(f"[warn] duplicate tool {t.name!r} from server {name!r}; skipping")
                continue
            registry[t.name] = session
            tools.append(mcp_tool_to_openai(t))
    print(f"[agent] discovered tools: {list(registry)} from {len(MCP_SERVERS)} server(s)")
    return tools, registry


SYSTEM_PROMPT = (
    "You are a helpful assistant. Use a tool only when you need information you "
    "don't already have. As soon as you have the tool result, STOP calling tools "
    "and reply to the user in plain text. Never call the same tool with the same "
    "arguments twice."
)


async def run_agent(user_msg: str, api_key: str | None, max_steps: int = 4):
    """max_steps also bounds LLM calls per question (max_steps + 1), protecting the caller's key."""
    steps = []
    seen: dict[str, str] = {}  # cache tool results; also used to detect loops

    async with contextlib.AsyncExitStack() as stack:
        tools, registry = await connect_all(stack)

        messages = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ]

        for _ in range(max_steps):
            resp = await litellm.acompletion(
                model=MODEL, messages=messages, api_key=api_key,
                tools=tools, tool_choice="auto",
            )
            msg = resp.choices[0].message
            messages.append(msg.model_dump())

            if not msg.tool_calls:              # no tool -> final answer
                return msg.content, steps

            ran_new = False
            for call in msg.tool_calls:
                name = call.function.name
                key = f"{name}:{call.function.arguments}"
                args = json.loads(call.function.arguments or "{}")

                if key in seen:                  # already ran this exact call
                    text = seen[key]
                elif name not in registry:       # model hallucinated a tool name
                    text = f"ERROR: unknown tool {name!r}"
                    seen[key] = text
                else:
                    session = registry[name]     # route to the owning server
                    result = await session.call_tool(name, args)
                    text = "\n".join(
                        c.text for c in result.content
                        if getattr(c, "type", None) == "text"
                    )
                    seen[key] = text
                    ran_new = True

                steps.append({"tool": name, "args": args, "result": text})
                messages.append({
                    "role": "tool",
                    "tool_call_id": call.id,
                    "content": text,
                })

            if not ran_new:                      # only repeats -> model is looping
                break

        # Budget spent (or looping) without a text answer -> force synthesis.
        # Small models often never stop on their own; drop the tools so the
        # model MUST answer in plain text.
        messages.append({"role": "user", "content":
            "Using the information above, answer my original question now "
            "in plain text. Do not call any tools."})
        resp = await litellm.acompletion(model=MODEL, messages=messages, api_key=api_key)
        return resp.choices[0].message.content or "(no answer produced)", steps


def _leaf_errors(e: BaseException):
    """Unwrap ExceptionGroups (async TaskGroups wrap the real cause) to readable leaves."""
    if isinstance(e, BaseExceptionGroup):
        out = []
        for sub in e.exceptions:
            out.extend(_leaf_errors(sub))
        return out
    return [f"{type(e).__name__}: {e}"]


def _error(status: int, text: str):
    return JSONResponse({"answer": "Error: " + text, "steps": []}, status_code=status)


def _first_leaf(e: BaseException) -> BaseException:
    while isinstance(e, BaseExceptionGroup) and e.exceptions:
        e = e.exceptions[0]
    return e


def _public_error(e: BaseException, api_key: str | None) -> tuple[int, str]:
    """Map failures to a safe message: known LLM errors get a friendly text; anything
    else gets the readable cause, with the caller's key scrubbed just in case."""
    leaf = _first_leaf(e)
    if isinstance(leaf, litellm.AuthenticationError):
        return 401, "invalid OpenRouter API key"
    if isinstance(leaf, litellm.RateLimitError):
        return 429, "rate limited by the model provider (check your OpenRouter credits/limits)"
    if isinstance(leaf, litellm.APIError):
        return 502, "the model provider returned an error, try again"
    text = " | ".join(_leaf_errors(e))
    if api_key:
        text = text.replace(api_key, "***")
    return 500, text


@app.post("/chat")
async def chat(req: Request):
    api_key = (req.headers.get("x-openrouter-key") or "").strip()
    if not api_key and MODEL.startswith("openrouter/"):
        return _error(401, "X-OpenRouter-Key header is required (bring your own OpenRouter key)")

    body = await req.json()
    message = (body.get("message") or "").strip()
    if not message:
        return _error(400, "message is empty")
    if len(message) > MAX_MESSAGE_CHARS:
        return _error(400, f"message too long (max {MAX_MESSAGE_CHARS} characters)")

    try:
        answer, steps = await run_agent(message, api_key or None)
        return JSONResponse({"answer": answer, "steps": steps})
    except BaseException as e:
        status, text = _public_error(e, api_key)
        return _error(status, text)


@app.get("/")
async def index():
    return FileResponse(STATIC_DIR / "index.html")
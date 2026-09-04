"""
A second 'hello-world' MCP server: one tool, get_random_quote(topic).
Note it has DIFFERENT arguments from weather's get_weather -- the agent
discovers each tool's own schema, no per-tool code in agent.py.

Transport: Streamable HTTP. Default URL: http://127.0.0.1:8002/mcp
"""
import os
import random
from mcp.server.fastmcp import FastMCP

MCP_HOST = os.getenv("MCP_HOST", "127.0.0.1")  # use 0.0.0.0 in Docker
MCP_PORT = int(os.getenv("MCP_PORT", "8002"))

mcp = FastMCP("quotes", host=MCP_HOST, port=MCP_PORT)

QUOTES = {
    "motivation": [
        "The secret of getting ahead is getting started. — Mark Twain",
        "Well done is better than well said. — Benjamin Franklin",
    ],
    "coding": [
        "Programs must be written for people to read. — Harold Abelson",
        "Simplicity is the soul of efficiency. — Austin Freeman",
    ],
    "life": [
        "In the middle of difficulty lies opportunity. — Albert Einstein",
        "What we think, we become. — Buddha",
    ],
}


@mcp.tool()
async def get_random_quote(topic: str = "motivation") -> str:
    """Return a random quote. topic can be one of: motivation, coding, life."""
    pool = QUOTES.get(topic.lower())
    if not pool:
        return (f"No quotes for topic {topic!r}. "
                f"Try one of: {', '.join(QUOTES)}.")
    return random.choice(pool)


if __name__ == "__main__":
    print(f"[quote_mcp] serving on http://{MCP_HOST}:{MCP_PORT}/mcp")
    mcp.run(transport="streamable-http")
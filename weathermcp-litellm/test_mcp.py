"""Quick check that the MCP server works WITHOUT any LLM/API key."""
import asyncio, os
from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client

MCP_URL = os.getenv("WEATHER_MCP_URL", "http://127.0.0.1:8001/mcp")


async def main():
    async with streamablehttp_client(MCP_URL) as (read, write, _):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            print("tools:", [t.name for t in tools.tools])
            res = await session.call_tool("get_weather", {"city": "Tokyo"})
            print("result:", "\n".join(c.text for c in res.content if getattr(c, "type", None) == "text"))


if __name__ == "__main__":
    asyncio.run(main())
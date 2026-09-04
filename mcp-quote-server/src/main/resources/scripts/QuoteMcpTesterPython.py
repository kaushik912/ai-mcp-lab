import asyncio
from mcp import ClientSession
from mcp.client.sse import sse_client   # SSE transport — matches your /sse endpoint

SERVER_SSE_URL = "http://localhost:8080/sse"   # full SSE endpoint, incl. /sse
TOOL_NAME = "randomQuote"
ARGS = {}                                       # e.g. {"category": "tech"}

# pip install mcp

async def main():
    async with sse_client(SERVER_SSE_URL) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()                    # handshake

            tools = await session.list_tools()            # == tools/list
            for t in tools.tools:
                print(f"- {t.name}: {t.description}")

            result = await session.call_tool(TOOL_NAME, ARGS)   # == tools/call
            print("isError:", result.isError)
            print("content:", result.content[0].text)     # text block

asyncio.run(main())
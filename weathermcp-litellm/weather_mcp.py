"""
A tiny 'hello-world' MCP server exposing one tool: get_weather(city).
Uses the free Open-Meteo API (no API key required).

Transport: Streamable HTTP -> runs as its own service (maps cleanly to
Docker / the 'remote MCP' portal direction). Default URL:
    http://127.0.0.1:8001/mcp
"""
import os
import httpx
from mcp.server.fastmcp import FastMCP

MCP_HOST = os.getenv("MCP_HOST", "127.0.0.1")  # use 0.0.0.0 in Docker
MCP_PORT = int(os.getenv("MCP_PORT", "8001"))

mcp = FastMCP("weather", host=MCP_HOST, port=MCP_PORT)

# WMO weather interpretation codes -> human text
WMO = {
    0: "clear sky", 1: "mainly clear", 2: "partly cloudy", 3: "overcast",
    45: "fog", 48: "depositing rime fog",
    51: "light drizzle", 53: "drizzle", 55: "dense drizzle",
    56: "light freezing drizzle", 57: "dense freezing drizzle",
    61: "light rain", 63: "rain", 65: "heavy rain",
    66: "light freezing rain", 67: "heavy freezing rain",
    71: "light snow", 73: "snow", 75: "heavy snow", 77: "snow grains",
    80: "light rain showers", 81: "rain showers", 82: "violent rain showers",
    85: "snow showers", 86: "heavy snow showers",
    95: "thunderstorm", 96: "thunderstorm with light hail",
    99: "thunderstorm with heavy hail",
}


@mcp.tool()
async def get_weather(city: str = "San Francisco") -> str:
    """Get the current weather for a city. Defaults to San Francisco if no city is given."""
    async with httpx.AsyncClient(timeout=15) as client:
        geo = (await client.get(
            "https://geocoding-api.open-meteo.com/v1/search",
            params={"name": city, "count": 1},
        )).json()
        if not geo.get("results"):
            return f"Could not find location: {city!r}."
        loc = geo["results"][0]
        lat, lon = loc["latitude"], loc["longitude"]
        place = f'{loc["name"]}, {loc.get("country", "")}'.strip(", ")

        wx = (await client.get(
            "https://api.open-meteo.com/v1/forecast",
            params={
                "latitude": lat, "longitude": lon,
                "current": "temperature_2m,apparent_temperature,weather_code,wind_speed_10m",
            },
        )).json()
        cur = wx["current"]
        desc = WMO.get(cur["weather_code"], "unknown conditions")
        return (
            f'Weather in {place}: {desc}, {cur["temperature_2m"]}°C '
            f'(feels like {cur["apparent_temperature"]}°C), '
            f'wind {cur["wind_speed_10m"]} km/h.'
        )


if __name__ == "__main__":
    print(f"[weather_mcp] serving on http://{MCP_HOST}:{MCP_PORT}/mcp")
    mcp.run(transport="streamable-http")
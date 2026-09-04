# Building Deep Agents with Sub-Agent Architectures

## Introduction

In this advanced tutorial, you'll learn how to build **multi-agent systems** using the **orchestrator pattern** - the architecture used by production systems like Claude Code, LangGraph Deep Agents, and enterprise AI applications.

### What You'll Learn

- Why monolithic agents struggle with complex tasks
- How to design sub-agent architectures with clear responsibility boundaries
- The orchestrator pattern: coordinator + specialized workers
- Building production-grade specialized agents (Research + Weather)
- Practical travel planning system with parallel sub-agent execution

### The Problem: Monolithic Agents Hit Limits

**Single Agent with All Tools:**
- Context window fills with 20+ tool descriptions
- System prompt becomes unfocused and conflicting
- Agent gets confused about which tool to use when
- Difficult to debug and maintain

**Sub-Agent Solution:**
- **Cognitive specialization**: Each agent has focused expertise
- **Context efficiency**: Only relevant tools per agent
- **Parallel processing**: Multiple sub-agents work concurrently
- **Maintainability**: Test and update individual agents

### The Orchestrator Pattern

The orchestrator pattern divides responsibilities into two types of agents:

| **Orchestrator (Coordinator)** | **Worker Agents (Specialists)** |
|--------------------------------|----------------------------------|
| Analyze overall task | Execute specific sub-tasks |
| Decompose into sub-tasks | Use specialized tools |
| Route to workers (parallel) | Return structured results |
| Synthesize final response | Remain stateless |
| **Tools:** Planning, file I/O | **Tools:** Domain-specific |
| **Focus:** Breadth, delegation | **Focus:** Depth, completion |

**Communication Flow:**
```
User: "I'm traveling to Tokyo next week. Research the city and give me the weather."
          ↓
    Orchestrator
    (decomposes into 2 sub-tasks)
          ↓
    ┌─────┴─────┐
    ↓           ↓
 Research    Weather
 Agent       Agent
(Tavily)    (Weather API)
    ↓           ↓
    └─────┬─────┘
          ↓
    Orchestrator
    (synthesizes)
          ↓
   Travel Brief
```

### Prerequisites

**Required API Keys:**
- OpenAI API key (for GPT-4)
- Tavily API key (for web research)
- OpenWeatherMap API key (for weather data)

**Setup:**
Create a `.env` file with:
```
OPENAI_API_KEY=your_openai_key_here
TAVILY_API_KEY=your_tavily_key_here
OPENWEATHER_API_KEY=your_openweather_key_here
```

### Learning Objectives

By the end of this tutorial, you'll be able to:
1. Design responsibility boundaries for multi-agent systems
2. Build specialized sub-agents with focused toolsets
3. Implement an orchestrator that coordinates parallel execution
4. Use shared filesystem for artifact-based communication
5. Deploy production-grade multi-agent systems

## Part 1: Setup and Installation

First, let's install all required packages and verify our environment.


```python
# Install required packages
# !pip install -q deepagents langchain-openai tavily-python requests tenacity pydantic python-dotenv
```

### Import Libraries and Load Environment Variables


```python
import os
import logging
import time
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, Any
from collections import defaultdict

import requests
from dotenv import load_dotenv
from pydantic import BaseModel, Field, field_validator
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
    before_sleep_log
)

from deepagents import create_deep_agent
from deepagents.backends import FilesystemBackend
from langchain_openai import ChatOpenAI
from tavily import TavilyClient

# Load environment variables
load_dotenv()

# Verify all API keys are loaded
required_keys = ["OPENAI_API_KEY", "TAVILY_API_KEY", "OPENWEATHER_API_KEY"]
for key in required_keys:
    if not os.getenv(key):
        raise ValueError(f"{key} not found in environment variables")

print("All API keys loaded successfully!")
print("Required packages imported successfully!")
```

    All API keys loaded successfully!
    Required packages imported successfully!


### Configure Structured Logging

Logging is critical in multi-agent systems to understand:
- Which sub-agent is executing which task
- How long each sub-task takes
- The sequence of orchestration decisions


```python
# Configure logging with structured format
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

# Create loggers for different components
logger = logging.getLogger('multi_agent_system')
weather_logger = logging.getLogger('weather_agent')
research_logger = logging.getLogger('research_agent')
orchestrator_logger = logging.getLogger('orchestrator')

logger.info("Multi-agent logging system initialized")
print("Logging configured - you'll see detailed execution traces below")
```

    2025-11-17 15:31:39 - multi_agent_system - INFO - Multi-agent logging system initialized


    Logging configured - you'll see detailed execution traces below


## Part 2: Building Production-Grade Weather Tools

Our Weather Agent needs robust, production-ready tools. We'll implement:
- Geocoding helper (convert city names to coordinates)
- Pydantic validation models
- Weather API client with retry logic
- Two weather tools: current conditions and 2-day forecast

### Step 1: Geocoding Helper Function

Convert user-friendly city names to the lat/lon coordinates required by the weather API.


```python
def geocode_city(city_name: str) -> tuple[float, float]:
    """
    Convert a city name to latitude and longitude coordinates.
    
    Args:
        city_name: The name of the city (e.g., "Tokyo", "London")
        
    Returns:
        tuple[float, float]: (latitude, longitude)
        
    Raises:
        ValueError: If city not found or invalid
    """
    city_name = city_name.strip()
    
    if not city_name:
        raise ValueError("City name cannot be empty")
    
    # Sanitize input: allow only safe characters
    if not all(c.isalnum() or c in " -'," for c in city_name):
        raise ValueError(f"Invalid city name format: {city_name}")
    
    weather_logger.info(f"Geocoding city: {city_name}")
    
    api_key = os.getenv("OPENWEATHER_API_KEY")
    url = "http://api.openweathermap.org/geo/1.0/direct"
    
    params = {
        "q": city_name,
        "limit": 1,
        "appid": api_key
    }
    
    try:
        response = requests.get(url, params=params, timeout=10)
        response.raise_for_status()
        data = response.json()
        
        if not data:
            raise ValueError(
                f"Could not find coordinates for '{city_name}'. "
                "Please check the spelling."
            )
        
        lat = data[0]["lat"]
        lon = data[0]["lon"]
        
        weather_logger.info(f"Geocoded {city_name} to ({lat}, {lon})")
        return lat, lon
        
    except requests.exceptions.RequestException as e:
        weather_logger.error(f"Geocoding failed: {str(e)}")
        raise ValueError(f"Failed to geocode city: {str(e)}")

# Test geocoding
lat, lon = geocode_city("Tokyo")
print(f"Tokyo coordinates: {lat}, {lon}")
```

    2025-11-17 15:31:39 - weather_agent - INFO - Geocoding city: Tokyo
    2025-11-17 15:31:40 - weather_agent - INFO - Geocoded Tokyo to (35.6828387, 139.7594549)


    Tokyo coordinates: 35.6828387, 139.7594549


### Step 2: Pydantic Validation Models

Type-safe input validation prevents errors before they reach the API.


```python
class WeatherLocation(BaseModel):
    """Validates location input for weather queries."""
    city: str = Field(
        ..., 
        min_length=1,
        max_length=100,
        description="City name"
    )
    
    @field_validator('city')
    @classmethod
    def validate_city(cls, v):
        v = v.strip()
        if not v:
            raise ValueError("City name cannot be empty")
        if not all(c.isalnum() or c in " -'," for c in v):
            raise ValueError("City name contains invalid characters")
        return v


class ForecastWeatherInput(BaseModel):
    """Validates forecast input (always 2 days)."""
    city: str = Field(..., min_length=1, max_length=100)
    days: int = Field(default=2, description="Forecast days (must be 2)")
    
    @field_validator('days')
    @classmethod
    def validate_days(cls, v):
        if v != 2:
            raise ValueError(f"Forecast only available for 2 days. Got: {v}")
        return v
    
    @field_validator('city')
    @classmethod
    def validate_city(cls, v):
        v = v.strip()
        if not v:
            raise ValueError("City name cannot be empty")
        if not all(c.isalnum() or c in " -'," for c in v):
            raise ValueError("City name contains invalid characters")
        return v

# Test validation
location = WeatherLocation(city="Tokyo")
forecast_input = ForecastWeatherInput(city="Tokyo", days=2)
print(f"Validation models working: {location.city}, {forecast_input.days} days")
```

    Validation models working: Tokyo, 2 days


### Step 3: Weather API Client with Retry Logic

Production-grade client with:
- Connection pooling for performance
- Automatic retries with exponential backoff
- Comprehensive error handling
- Request timing and logging


```python
class WeatherAPIClient:
    """
    Production-grade Weather API client with resilience and observability.
    
    Features:
    - Connection pooling via requests.Session
    - Automatic retries with exponential backoff
    - Structured logging
    - Comprehensive error handling
    """
    
    def __init__(self, api_key: str, timeout: int = 10, max_retries: int = 3):
        self.api_key = api_key
        self.timeout = timeout
        self.max_retries = max_retries
        
        # Connection pooling
        self.session = requests.Session()
        adapter = requests.adapters.HTTPAdapter(
            pool_connections=10,
            pool_maxsize=20,
            max_retries=0
        )
        self.session.mount('http://', adapter)
        self.session.mount('https://', adapter)
        
        weather_logger.info("Weather API Client initialized")
    
    def _log_request(self, endpoint: str, params: Dict[str, Any], 
                     duration: float, success: bool):
        """Log API request with sanitized parameters."""
        safe_params = {k: v for k, v in params.items() if k != 'appid'}
        safe_params['appid'] = '***REDACTED***'
        status = "SUCCESS" if success else "FAILURE"
        weather_logger.info(
            f"API [{status}] - {endpoint} - {duration:.2f}s - {safe_params}"
        )
    
    @retry(
        retry=retry_if_exception_type((requests.exceptions.Timeout, 
                                      requests.exceptions.ConnectionError)),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        before_sleep=before_sleep_log(weather_logger, logging.WARNING)
    )
    def _make_request(self, endpoint: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """Make API request with retry logic."""
        start_time = time.time()
        
        try:
            params['appid'] = self.api_key
            response = self.session.get(endpoint, params=params, timeout=self.timeout)
            duration = time.time() - start_time
            
            if response.status_code == 401:
                self._log_request(endpoint, params, duration, False)
                raise ValueError("Authentication failed. Check API key.")
            
            if response.status_code == 404:
                self._log_request(endpoint, params, duration, False)
                raise ValueError("Location not found.")
            
            if response.status_code == 429:
                self._log_request(endpoint, params, duration, False)
                raise ValueError("Rate limit exceeded.")
            
            response.raise_for_status()
            data = response.json()
            self._log_request(endpoint, params, duration, True)
            return data
            
        except requests.exceptions.Timeout:
            duration = time.time() - start_time
            self._log_request(endpoint, params, duration, False)
            raise TimeoutError(f"Request timed out after {self.timeout}s")
        
        except requests.exceptions.ConnectionError as e:
            duration = time.time() - start_time
            self._log_request(endpoint, params, duration, False)
            raise ConnectionError("Unable to connect to weather service")
    
    def get_current_weather(self, lat: float, lon: float) -> Dict[str, Any]:
        """Get current weather for coordinates."""
        endpoint = "https://api.openweathermap.org/data/2.5/weather"
        params = {"lat": lat, "lon": lon, "units": "metric"}
        return self._make_request(endpoint, params)
    
    def get_forecast(self, lat: float, lon: float) -> Dict[str, Any]:
        """Get 2-day forecast using free tier 5 Day / 3 Hour Forecast API."""
        endpoint = "https://api.openweathermap.org/data/2.5/forecast"
        params = {
            "lat": lat, 
            "lon": lon, 
            "units": "metric",
            "cnt": 16  # 2 days * 8 forecasts per day
        }
        return self._make_request(endpoint, params)

# Initialize the API client
weather_api_client = WeatherAPIClient(
    api_key=os.getenv("OPENWEATHER_API_KEY"),
    timeout=10,
    max_retries=3
)

print("Weather API Client initialized with connection pooling and retries")
```

    2025-11-17 15:31:40 - weather_agent - INFO - Weather API Client initialized


    Weather API Client initialized with connection pooling and retries


### Step 4: Current Weather Tool

This tool will be used by our Weather Agent to get current conditions.


```python
def get_current_weather(city: str) -> str:
    """
    Get current weather conditions for a city.
    
    Use this when asked about current weather, present conditions,
    or "right now" weather.
    
    Args:
        city: City name (e.g., "Tokyo", "London")
        
    Returns:
        str: Human-readable current weather description
    """
    weather_logger.info(f"get_current_weather called for: {city}")
    start_time = time.time()
    
    try:
        # Validate input
        location = WeatherLocation(city=city)
        
        # Geocode
        lat, lon = geocode_city(location.city)
        
        # Fetch weather
        weather_data = weather_api_client.get_current_weather(lat, lon)
        
        # Format response
        temp = weather_data['main']['temp']
        feels_like = weather_data['main']['feels_like']
        description = weather_data['weather'][0]['description']
        humidity = weather_data['main']['humidity']
        wind_speed = weather_data['wind']['speed']
        
        result = (
            f"Current weather in {location.city}:\n"
            f"  Temperature: {temp}°C (feels like {feels_like}°C)\n"
            f"  Conditions: {description.capitalize()}\n"
            f"  Humidity: {humidity}%\n"
            f"  Wind Speed: {wind_speed} m/s"
        )
        
        duration = time.time() - start_time
        weather_logger.info(f"Tool completed in {duration:.2f}s")
        return result
        
    except Exception as e:
        duration = time.time() - start_time
        weather_logger.error(f"Error after {duration:.2f}s: {str(e)}")
        return f"Error: {str(e)}"

# Test the tool
result = get_current_weather("Tokyo")
print(result)
```

    2025-11-17 15:31:40 - weather_agent - INFO - get_current_weather called for: Tokyo
    2025-11-17 15:31:40 - weather_agent - INFO - Geocoding city: Tokyo
    2025-11-17 15:31:40 - weather_agent - INFO - Geocoded Tokyo to (35.6828387, 139.7594549)
    2025-11-17 15:31:41 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/weather - 0.93s - {'lat': 35.6828387, 'lon': 139.7594549, 'units': 'metric', 'appid': '***REDACTED***'}
    2025-11-17 15:31:41 - weather_agent - INFO - Tool completed in 1.05s


    Current weather in Tokyo:
      Temperature: 19.41°C (feels like 18.61°C)
      Conditions: Clear sky
      Humidity: 46%
      Wind Speed: 5.14 m/s


### Step 5: Weather Forecast Tool

This tool provides 2-day weather forecasts.


```python
def get_weather_forecast(city: str) -> str:
    """
    Get weather forecast for the next 2 days.
    
    Use this when asked about future weather, tomorrow's weather,
    or weather predictions.
    
    Args:
        city: City name (e.g., "Tokyo", "London")
        
    Returns:
        str: 2-day forecast with temperatures and conditions
    """
    weather_logger.info(f"get_weather_forecast called for: {city}")
    start_time = time.time()
    
    try:
        # Validate
        input_data = ForecastWeatherInput(city=city, days=2)
        
        # Geocode
        lat, lon = geocode_city(input_data.city)
        
        # Fetch forecast
        forecast_data = weather_api_client.get_forecast(lat, lon)
        
        # Process forecast data (group by day)
        forecast_list = forecast_data['list']
        daily_forecasts = defaultdict(list)
        
        for forecast in forecast_list:
            forecast_date = datetime.fromtimestamp(forecast['dt']).date()
            daily_forecasts[forecast_date].append(forecast)
        
        # Get next 2 days
        sorted_dates = sorted(daily_forecasts.keys())[:2]
        results = []
        
        for i, date in enumerate(sorted_dates):
            day_forecasts = daily_forecasts[date]
            day_label = "tomorrow" if i == 0 else "day after tomorrow"
            
            # Calculate daily statistics
            temps = [f['main']['temp'] for f in day_forecasts]
            temp_min = min(f['main']['temp_min'] for f in day_forecasts)
            temp_max = max(f['main']['temp_max'] for f in day_forecasts)
            avg_temp = sum(temps) / len(temps)
            
            # Most common condition
            conditions = [f['weather'][0]['description'] for f in day_forecasts]
            most_common = max(set(conditions), key=conditions.count)
            
            # Averages
            avg_humidity = sum(f['main']['humidity'] for f in day_forecasts) / len(day_forecasts)
            avg_wind = sum(f['wind']['speed'] for f in day_forecasts) / len(day_forecasts)
            
            results.append(
                f"  {date.strftime('%Y-%m-%d')} ({day_label}):\n"
                f"    Temp: {avg_temp:.1f}°C (High: {temp_max:.1f}°C, Low: {temp_min:.1f}°C)\n"
                f"    Conditions: {most_common.capitalize()}\n"
                f"    Humidity: {avg_humidity:.0f}%\n"
                f"    Wind: {avg_wind:.1f} m/s"
            )
        
        result = f"Weather forecast for {input_data.city} (next 2 days):\n\n" + "\n\n".join(results)
        
        duration = time.time() - start_time
        weather_logger.info(f"Tool completed in {duration:.2f}s")
        return result
        
    except Exception as e:
        duration = time.time() - start_time
        weather_logger.error(f"Error after {duration:.2f}s: {str(e)}")
        return f"Error: {str(e)}"

# Test the tool
result = get_weather_forecast("Tokyo")
print(result)
```

    2025-11-17 15:31:41 - weather_agent - INFO - get_weather_forecast called for: Tokyo
    2025-11-17 15:31:41 - weather_agent - INFO - Geocoding city: Tokyo
    2025-11-17 15:31:41 - weather_agent - INFO - Geocoded Tokyo to (35.6828387, 139.7594549)
    2025-11-17 15:31:42 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/forecast - 0.28s - {'lat': 35.6828387, 'lon': 139.7594549, 'units': 'metric', 'cnt': 16, 'appid': '***REDACTED***'}
    2025-11-17 15:31:42 - weather_agent - INFO - Tool completed in 0.77s


    Weather forecast for Tokyo (next 2 days):
    
      2025-11-17 (tomorrow):
        Temp: 16.1°C (High: 18.8°C, Low: 13.5°C)
        Conditions: Clear sky
        Humidity: 46%
        Wind: 5.9 m/s
    
      2025-11-18 (day after tomorrow):
        Temp: 11.4°C (High: 12.3°C, Low: 9.5°C)
        Conditions: Light rain
        Humidity: 60%
        Wind: 3.3 m/s


## Part 3: Building the Research Agent

The Research Agent specializes in web search using Tavily. It has a focused system prompt and only one tool.

### Step 1: Create the Tavily Search Tool


```python
# Initialize Tavily client
tavily_client = TavilyClient(api_key=os.getenv("TAVILY_API_KEY"))

def internet_search(query: str, max_results: int = 5) -> dict:
    """
    Search the web for information using Tavily.
    
    Args:
        query: Search query string
        max_results: Maximum results to return (default: 5)
    
    Returns:
        dict: Search results with titles, URLs, and content
    """
    research_logger.info(f"Searching: {query}")
    try:
        results = tavily_client.search(query, max_results=max_results)
        research_logger.info(f"Found {len(results.get('results', []))} results")
        return results
    except Exception as e:
        research_logger.error(f"Search failed: {str(e)}")
        return {"error": f"Search failed: {str(e)}"}

print("Tavily search tool configured")
```

    Tavily search tool configured


### Step 2: Create the Research Agent

Notice the specialized system prompt - it only knows about research, not weather.


```python
# Create workspace for shared files
workspace_dir = Path("./multi_agent_workspace")
workspace_dir.mkdir(exist_ok=True)

# Research Agent system prompt - focused on research only
research_system_prompt = """You are an expert research assistant specializing in web search and information gathering.

YOUR EXPERTISE:
- Conduct thorough web research using internet search
- Synthesize information from multiple sources
- Provide well-organized research summaries
- Cite sources when presenting findings

RESEARCH WORKFLOW:
1. Break down complex research questions into specific search queries
2. Search for information systematically
3. Analyze and synthesize findings
4. Save comprehensive research reports to files

TOOL USAGE:
- Use internet_search for all web research needs
- Start with broad searches, then narrow based on results
- Always verify facts from multiple sources
- Save detailed findings to files for later reference

You do NOT handle weather queries - focus only on research tasks.
"""

# Create Research Agent
research_model = ChatOpenAI(model="gpt-4o", temperature=0.1)

research_agent = create_deep_agent(
    model=research_model,
    tools=[internet_search],
    system_prompt=research_system_prompt,
    backend=FilesystemBackend(root_dir=str(workspace_dir), virtual_mode=True)
)

research_logger.info("Research Agent created")
print("Research Agent created successfully!")
print("Capabilities: Web search, file I/O, planning")
print(f"Shared workspace: {workspace_dir.absolute()}")
```

    2025-11-17 15:31:42 - research_agent - INFO - Research Agent created


    Research Agent created successfully!
    Capabilities: Web search, file I/O, planning
    Shared workspace: /Users/sajal/code/active/projects/oreilly_ai_agent_skill/level_4/multi_agent_workspace


## Part 4: Building the Weather Agent

The Weather Agent specializes in weather data. It only knows about weather tools.


```python
# Weather Agent system prompt - focused on weather only
weather_system_prompt = """You are an expert weather information specialist.

YOUR EXPERTISE:
- Provide current weather conditions for any city
- Provide 2-day weather forecasts
- Interpret weather data clearly for users

AVAILABLE TOOLS:
- get_current_weather: Use for current/present conditions
- get_weather_forecast: Use for future weather (next 2 days)

TOOL SELECTION:
- "What's the weather now?" → get_current_weather
- "What's the forecast?" → get_weather_forecast
- "Weather this week?" → get_weather_forecast (only have 2 days)

RESPONSE FORMAT:
- Present temperature in Celsius
- Include all relevant conditions (humidity, wind, etc.)
- Be concise but complete
- Save weather reports to files when requested

You do NOT handle research queries - focus only on weather tasks.
"""

# Create Weather Agent
weather_model = ChatOpenAI(model="gpt-4o", temperature=0.1)

weather_agent = create_deep_agent(
    model=weather_model,
    tools=[get_current_weather, get_weather_forecast],
    system_prompt=weather_system_prompt,
    backend=FilesystemBackend(root_dir=str(workspace_dir), virtual_mode=True)
)

weather_logger.info("Weather Agent created")
print("Weather Agent created successfully!")
print("Capabilities: Current weather, 2-day forecast, file I/O")
print(f"Shared workspace: {workspace_dir.absolute()}")
```

    2025-11-17 15:31:42 - weather_agent - INFO - Weather Agent created


    Weather Agent created successfully!
    Capabilities: Current weather, 2-day forecast, file I/O
    Shared workspace: /Users/sajal/code/active/projects/oreilly_ai_agent_skill/level_4/multi_agent_workspace


## Part 5: Building the Orchestrator Agent

The Orchestrator coordinates sub-agents. It doesn't have domain tools - it has the `task` tool to spawn sub-agents.


```python
# Orchestrator system prompt - coordination and delegation
orchestrator_system_prompt = """You are an orchestrator agent that coordinates specialized sub-agents.

YOUR ROLE:
- Analyze user requests and break them into sub-tasks
- Delegate sub-tasks to specialized agents
- Synthesize results from multiple agents into comprehensive responses

AVAILABLE SUB-AGENTS:
1. **Research Agent** (subagent_type='research')
   - Use for: Web research, information gathering, learning about cities/topics
   - Capabilities: Internet search, synthesis, citation

2. **Weather Agent** (subagent_type='weather')
   - Use for: Current weather, weather forecasts
   - Capabilities: Current conditions, 2-day forecasts

ORCHESTRATION PATTERN:
1. Analyze the user's request
2. Identify which sub-agents are needed
3. Use the 'task' tool to spawn sub-agents (can run in parallel)
4. Collect results from sub-agents
5. Synthesize into a coherent, comprehensive response

TASK TOOL USAGE:
- description: Clear description of what the sub-agent should do
- subagent_type: 'research' or 'weather'

Example:
User: "I'm traveling to Tokyo. Research the city and tell me the weather."
→ Spawn research agent: "Research Tokyo: culture, attractions, travel tips"
→ Spawn weather agent: "Get current weather and 2-day forecast for Tokyo"
→ Synthesize both results into travel brief

RESPONSE QUALITY:
- Combine all sub-agent results into a cohesive response
- Organize information logically
- Ensure completeness - answer all parts of the user's query
"""

# Create Orchestrator Agent with access to sub-agents
orchestrator_model = ChatOpenAI(model="gpt-4o", temperature=0.1)

# Define subagents in the correct format: list of dictionaries
subagent_list = [
    {
        'name': 'research',
        'description': 'Expert research assistant for web search and information gathering',
        'runnable': research_agent
    },
    {
        'name': 'weather',
        'description': 'Weather specialist for current conditions and forecasts',
        'runnable': weather_agent
    }
]

orchestrator = create_deep_agent(
    model=orchestrator_model,
    tools=[],  # No domain tools - only uses task tool for sub-agents
    system_prompt=orchestrator_system_prompt,
    backend=FilesystemBackend(root_dir=str(workspace_dir), virtual_mode=True),
    subagents=subagent_list  # Pass as list of dicts with name, description, runnable
)

orchestrator_logger.info("Orchestrator Agent created")
print("Orchestrator Agent created successfully!")
print("Capabilities: Task decomposition, sub-agent coordination, synthesis")
print(f"Sub-agents: research, weather")
print(f"Shared workspace: {workspace_dir.absolute()}")
```

    2025-11-17 15:31:42 - orchestrator - INFO - Orchestrator Agent created


    Orchestrator Agent created successfully!
    Capabilities: Task decomposition, sub-agent coordination, synthesis
    Sub-agents: research, weather
    Shared workspace: /Users/sajal/code/active/projects/oreilly_ai_agent_skill/level_4/multi_agent_workspace


## Part 6: Testing the Multi-Agent System

Now let's test our orchestrator with a complex query that requires both research and weather.

### Helper Function: Stream Orchestrator Responses

This helper shows the orchestration process including sub-agent spawning.


```python
def stream_orchestrator(query: str, show_details: bool = True):
    """
    Stream orchestrator execution with detailed logging.
    
    Args:
        query: User query
        show_details: Show detailed tool calls and sub-agent spawning
    """
    print("=" * 80)
    print(f"QUERY: {query}")
    print("=" * 80)
    print()
    
    seen_message_ids = set()
    
    for chunk in orchestrator.stream(
        {"messages": [{"role": "user", "content": query}]},
        stream_mode="values"
    ):
        if "messages" in chunk:
            for message in chunk["messages"]:
                msg_id = message.id
                if msg_id in seen_message_ids:
                    continue
                seen_message_ids.add(msg_id)
                
                msg_type = getattr(message, 'type', 'unknown')
                
                if msg_type == 'human':
                    print(f"[USER] {message.content}")
                    print("-" * 80)
                
                elif msg_type == 'ai':
                    if hasattr(message, 'tool_calls') and message.tool_calls:
                        print(f"[ORCHESTRATOR] Calling {len(message.tool_calls)} tool(s):")
                        for tc in message.tool_calls:
                            tool_name = tc.get('name', 'unknown')
                            tool_args = tc.get('args', {})
                            
                            if tool_name == 'task' and show_details:
                                subagent_type = tool_args.get('subagent_type', 'unknown')
                                description = tool_args.get('description', '')[:80]
                                print(f"  - Spawning {subagent_type} agent: {description}...")
                            elif tool_name == 'write_todos' and show_details:
                                todos = tool_args.get('todos', [])
                                print(f"  - Planning {len(todos)} tasks")
                            else:
                                print(f"  - {tool_name}()")
                    elif message.content:
                        print(f"[ORCHESTRATOR] {message.content}")
                    print("-" * 80)
                
                elif msg_type == 'tool':
                    tool_name = getattr(message, 'name', 'unknown')
                    if show_details:
                        print(f"[TOOL: {tool_name}] Completed")
                        print("-" * 80)
    
    print()
    print("=" * 80)
    print("ORCHESTRATION COMPLETED")
    print("=" * 80)

print("Helper function created!")
```

    Helper function created!


### Example 1: Travel Planning Query

This complex query requires both research and weather information.

**Expected Behavior:**
1. Orchestrator analyzes the query
2. Spawns research agent: "Research Tokyo travel information"
3. Spawns weather agent: "Get weather forecast for Tokyo"
4. Both agents run (potentially in parallel)
5. Orchestrator synthesizes results into comprehensive travel brief

**Watch For:**
- Task decomposition (planning todos)
- Sub-agent spawning (task tool calls)
- Parallel execution (both agents working simultaneously)
- Result synthesis (combining research + weather)


```python
# Example 1: Complex travel planning query
travel_query = """
I'm planning a trip to Tokyo next week. I need:
1. Research about Tokyo - culture, top attractions, and travel tips
2. Weather forecast for the next 2 days so I know what to pack

Please provide a comprehensive travel brief.
"""

stream_orchestrator(travel_query, show_details=True)
```

    ================================================================================
    QUERY: 
    I'm planning a trip to Tokyo next week. I need:
    1. Research about Tokyo - culture, top attractions, and travel tips
    2. Weather forecast for the next 2 days so I know what to pack
    
    Please provide a comprehensive travel brief.
    
    ================================================================================
    
    [USER] 
    I'm planning a trip to Tokyo next week. I need:
    1. Research about Tokyo - culture, top attractions, and travel tips
    2. Weather forecast for the next 2 days so I know what to pack
    
    Please provide a comprehensive travel brief.
    
    --------------------------------------------------------------------------------


    2025-11-17 15:31:45 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Calling 2 tool(s):
      - Spawning research agent: Research Tokyo: culture, top attractions, and travel tips...
      - Spawning weather agent: Get current weather and 2-day forecast for Tokyo...
    --------------------------------------------------------------------------------


    2025-11-17 15:31:47 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:31:47 - weather_agent - INFO - get_current_weather called for: Tokyo
    2025-11-17 15:31:47 - weather_agent - INFO - Geocoding city: Tokyo
    2025-11-17 15:31:47 - weather_agent - INFO - get_weather_forecast called for: Tokyo
    2025-11-17 15:31:47 - weather_agent - INFO - Geocoding city: Tokyo
    2025-11-17 15:31:47 - weather_agent - INFO - Geocoded Tokyo to (35.6828387, 139.7594549)
    2025-11-17 15:31:47 - weather_agent - INFO - Geocoded Tokyo to (35.6828387, 139.7594549)
    2025-11-17 15:31:47 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/weather - 0.06s - {'lat': 35.6828387, 'lon': 139.7594549, 'units': 'metric', 'appid': '***REDACTED***'}
    2025-11-17 15:31:47 - weather_agent - INFO - Tool completed in 0.19s
    2025-11-17 15:31:47 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/forecast - 0.19s - {'lat': 35.6828387, 'lon': 139.7594549, 'units': 'metric', 'cnt': 16, 'appid': '***REDACTED***'}
    2025-11-17 15:31:47 - weather_agent - INFO - Tool completed in 0.32s
    2025-11-17 15:31:50 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:31:50 - research_agent - INFO - Searching: Tokyo culture overview
    2025-11-17 15:31:50 - research_agent - INFO - Searching: Top attractions in Tokyo
    2025-11-17 15:31:50 - research_agent - INFO - Searching: Travel tips for visiting Tokyo
    2025-11-17 15:31:50 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:31:52 - research_agent - INFO - Found 5 results
    2025-11-17 15:31:52 - research_agent - INFO - Found 5 results
    2025-11-17 15:31:53 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:10 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [TOOL: task] Completed
    --------------------------------------------------------------------------------
    [TOOL: task] Completed
    --------------------------------------------------------------------------------


    2025-11-17 15:32:16 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Here's a comprehensive travel brief for your upcoming trip to Tokyo:
    
    ### Culture of Tokyo
    Tokyo is a city where tradition and modernity coexist harmoniously. The cultural landscape is a dynamic fusion of historical practices and contemporary innovations. Key cultural elements include:
    - **Traditional Arts**: Kabuki and Noh are prominent traditional performing arts in Tokyo.
    - **Modern Influences**: The city is a hub for pop culture phenomena, including anime and manga.
    - **Cultural Sites**: Tokyo offers a rich history with ancient temples and modern architectural marvels.
    - **Diverse Neighborhoods**: Each area in Tokyo has its unique cultural flavor, from the historic Asakusa to the bustling Shibuya.
    
    ### Top Attractions in Tokyo
    Tokyo is filled with must-see attractions that cater to a variety of interests:
    - **Shinjuku Gyoen National Garden**: A beautiful garden perfect for a relaxing stroll.
    - **Senso-ji Temple**: Tokyo's oldest temple, located in Asakusa.
    - **Tokyo Skytree and Tokyo Tower**: Iconic landmarks offering panoramic views of the city.
    - **Meiji Shrine**: A serene Shinto shrine surrounded by a forested area.
    - **TeamLab Planets**: An immersive digital art museum in Toyosu.
    - **Imperial Palace**: The primary residence of the Emperor of Japan.
    
    ### Travel Tips for Visiting Tokyo
    - **Best Time to Visit**: Spring and fall are ideal due to pleasant weather.
    - **Transportation**: Consider getting an IC card (like Suica) for convenient travel on public transport.
    - **Footwear**: Bring comfortable, waterproof shoes as you'll likely do a lot of walking.
    - **Connectivity**: While there are many Wi-Fi spots, they can be unreliable, so plan accordingly.
    - **Local Etiquette**: Familiarize yourself with basic Japanese phrases and customs to enhance your experience.
    
    ### Weather Forecast for Tokyo
    **Current Weather:**
    - Temperature: 19.41°C (feels like 18.61°C)
    - Conditions: Clear sky
    - Humidity: 46%
    - Wind Speed: 5.14 m/s
    
    **2-Day Forecast:**
    - **Tomorrow (2025-11-17):**
      - Temperature: 16.1°C (High: 18.8°C, Low: 13.5°C)
      - Conditions: Clear sky
      - Humidity: 46%
      - Wind: 5.9 m/s
    
    - **Day After Tomorrow (2025-11-18):**
      - Temperature: 11.4°C (High: 12.3°C, Low: 9.5°C)
      - Conditions: Light rain
      - Humidity: 60%
      - Wind: 3.3 m/s
    
    This information should help you plan your trip effectively, ensuring you experience the best of Tokyo's culture, attractions, and weather. Enjoy your trip!
    --------------------------------------------------------------------------------
    
    ================================================================================
    ORCHESTRATION COMPLETED
    ================================================================================


### Example 2: Multi-City Comparison

Test the orchestrator's ability to handle multiple locations.


```python
# Example 2: Multi-city comparison
comparison_query = """
I'm deciding between visiting Paris or London next month.
Research both cities and compare their weather forecasts.
Help me decide which would be better for a week-long trip.
"""

stream_orchestrator(comparison_query, show_details=True)
```

    ================================================================================
    QUERY: 
    I'm deciding between visiting Paris or London next month.
    Research both cities and compare their weather forecasts.
    Help me decide which would be better for a week-long trip.
    
    ================================================================================
    
    [USER] 
    I'm deciding between visiting Paris or London next month.
    Research both cities and compare their weather forecasts.
    Help me decide which would be better for a week-long trip.
    
    --------------------------------------------------------------------------------


    2025-11-17 15:32:18 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Calling 4 tool(s):
      - Spawning research agent: Research Paris: culture, attractions, travel tips...
      - Spawning research agent: Research London: culture, attractions, travel tips...
      - Spawning weather agent: Get current weather and 2-day forecast for Paris...
      - Spawning weather agent: Get current weather and 2-day forecast for London...
    --------------------------------------------------------------------------------


    2025-11-17 15:32:20 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:20 - weather_agent - INFO - get_current_weather called for: London
    2025-11-17 15:32:20 - weather_agent - INFO - get_weather_forecast called for: London
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoding city: London
    2025-11-17 15:32:20 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoding city: London
    2025-11-17 15:32:20 - weather_agent - INFO - get_current_weather called for: Paris
    2025-11-17 15:32:20 - weather_agent - INFO - get_weather_forecast called for: Paris
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoding city: Paris
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoding city: Paris
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoded London to (51.5073219, -0.1276474)
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoded Paris to (48.8588897, 2.3200410217200766)
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoded London to (51.5073219, -0.1276474)
    2025-11-17 15:32:20 - weather_agent - INFO - Geocoded Paris to (48.8588897, 2.3200410217200766)
    2025-11-17 15:32:21 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/weather - 0.06s - {'lat': 51.5073219, 'lon': -0.1276474, 'units': 'metric', 'appid': '***REDACTED***'}
    2025-11-17 15:32:21 - weather_agent - INFO - Tool completed in 0.46s
    2025-11-17 15:32:21 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/forecast - 0.07s - {'lat': 48.8588897, 'lon': 2.3200410217200766, 'units': 'metric', 'cnt': 16, 'appid': '***REDACTED***'}
    2025-11-17 15:32:21 - weather_agent - INFO - Tool completed in 0.47s
    2025-11-17 15:32:21 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/weather - 0.19s - {'lat': 48.8588897, 'lon': 2.3200410217200766, 'units': 'metric', 'appid': '***REDACTED***'}
    2025-11-17 15:32:21 - weather_agent - INFO - Tool completed in 0.61s
    2025-11-17 15:32:21 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/forecast - 0.20s - {'lat': 51.5073219, 'lon': -0.1276474, 'units': 'metric', 'cnt': 16, 'appid': '***REDACTED***'}
    2025-11-17 15:32:21 - weather_agent - INFO - Tool completed in 0.63s
    2025-11-17 15:32:21 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:21 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:23 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:23 - research_agent - INFO - Searching: London culture historical influences arts local customs
    2025-11-17 15:32:23 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:23 - research_agent - INFO - Searching: Paris culture history art lifestyle
    2025-11-17 15:32:23 - research_agent - INFO - Searching: Paris popular attractions landmarks museums parks
    2025-11-17 15:32:23 - research_agent - INFO - Searching: Paris travel tips best times to visit transportation local customs
    2025-11-17 15:32:24 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:25 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:25 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:25 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:25 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:25 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:30 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:31 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:31 - research_agent - INFO - Searching: major attractions in London landmarks museums parks
    2025-11-17 15:32:32 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:34 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:36 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:36 - research_agent - INFO - Searching: travel tips for visiting London transportation accommodation dining
    2025-11-17 15:32:36 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:38 - research_agent - INFO - Found 5 results
    2025-11-17 15:32:38 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:45 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:46 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [TOOL: task] Completed
    --------------------------------------------------------------------------------
    [TOOL: task] Completed
    --------------------------------------------------------------------------------
    [TOOL: task] Completed
    --------------------------------------------------------------------------------
    [TOOL: task] Completed
    --------------------------------------------------------------------------------


    2025-11-17 15:32:54 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] I've gathered detailed information on both Paris and London, including their cultural highlights, attractions, and travel tips, as well as their current weather conditions and forecasts. Here's a summary to help you decide which city might be better for your week-long trip:
    
    ### Paris
    - **Culture & Attractions**: Paris is renowned for its rich history, art, and architecture. Key attractions include the Eiffel Tower, Louvre Museum, Notre-Dame Cathedral, and charming neighborhoods like Montmartre.
    - **Travel Tips**: Paris offers a vibrant café culture, excellent public transport, and numerous parks and gardens.
    - **Weather**:
      - **Current**: 7.36°C, feels like 4.23°C, broken clouds, 88% humidity.
      - **Forecast**:
        - **Tomorrow**: 7.5°C, scattered clouds, 63% humidity.
        - **Day After Tomorrow**: 6.6°C, clear sky, 55% humidity.
    
    For more detailed insights, you can review the [Paris Research Report](sandbox:/Paris_Research_Report.txt).
    
    ### London
    - **Culture & Attractions**: London is a bustling metropolis with a mix of modern and historical sites. Highlights include the British Museum, Tower of London, Buckingham Palace, and vibrant areas like Camden and Shoreditch.
    - **Travel Tips**: London is known for its diverse food scene, extensive public transport, and numerous parks.
    - **Weather**:
      - **Current**: 5.84°C, feels like 2.32°C, broken clouds, 80% humidity.
      - **Forecast**:
        - **Tomorrow**: 6.7°C, few clouds, 67% humidity.
        - **Day After Tomorrow**: 4.5°C, scattered clouds, 73% humidity.
    
    For a comprehensive guide, you can check the [London Travel Guide](sandbox:/research/London_Travel_Guide.txt).
    
    ### Decision Factors
    - **Weather**: Both cities have similar weather conditions, with Paris being slightly warmer and clearer in the coming days.
    - **Cultural Experience**: Both cities offer rich cultural experiences, but your preference for art, history, or modern attractions might sway your decision.
    
    Consider what type of experiences you value most and the weather conditions that suit your preferences. Both cities have a lot to offer, so you can't go wrong with either choice!
    --------------------------------------------------------------------------------
    
    ================================================================================
    ORCHESTRATION COMPLETED
    ================================================================================


### Example 3: Single Sub-Agent Query

Test that the orchestrator correctly handles queries requiring only one sub-agent.


```python
# Example 3: Weather-only query
weather_only_query = "What's the weather forecast for Singapore for the next 2 days?"

stream_orchestrator(weather_only_query, show_details=True)
```

    ================================================================================
    QUERY: What's the weather forecast for Singapore for the next 2 days?
    ================================================================================
    
    [USER] What's the weather forecast for Singapore for the next 2 days?
    --------------------------------------------------------------------------------


    2025-11-17 15:32:55 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Calling 1 tool(s):
      - Spawning weather agent: Get current weather and 2-day forecast for Singapore...
    --------------------------------------------------------------------------------


    2025-11-17 15:32:56 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:32:56 - weather_agent - INFO - get_weather_forecast called for: Singapore
    2025-11-17 15:32:56 - weather_agent - INFO - get_current_weather called for: Singapore
    2025-11-17 15:32:56 - weather_agent - INFO - Geocoding city: Singapore
    2025-11-17 15:32:56 - weather_agent - INFO - Geocoding city: Singapore
    2025-11-17 15:32:56 - weather_agent - INFO - Geocoded Singapore to (1.2899175, 103.8519072)
    2025-11-17 15:32:56 - weather_agent - INFO - Geocoded Singapore to (1.2899175, 103.8519072)
    2025-11-17 15:32:56 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/weather - 0.07s - {'lat': 1.2899175, 'lon': 103.8519072, 'units': 'metric', 'appid': '***REDACTED***'}
    2025-11-17 15:32:56 - weather_agent - INFO - Tool completed in 0.45s
    2025-11-17 15:32:56 - weather_agent - INFO - API [SUCCESS] - https://api.openweathermap.org/data/2.5/forecast - 0.08s - {'lat': 1.2899175, 'lon': 103.8519072, 'units': 'metric', 'cnt': 16, 'appid': '***REDACTED***'}
    2025-11-17 15:32:56 - weather_agent - INFO - Tool completed in 0.46s
    2025-11-17 15:32:59 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [TOOL: task] Completed
    --------------------------------------------------------------------------------


    2025-11-17 15:33:03 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Here's the current weather and 2-day forecast for Singapore:
    
    **Current Weather in Singapore:**
    - Temperature: 27.81°C (feels like 31.77°C)
    - Conditions: Scattered clouds
    - Humidity: 81%
    - Wind Speed: 2.06 m/s
    
    **Weather Forecast for the Next 2 Days:**
    
    **November 17, 2025 (Tomorrow):**
    - Temperature: 28.5°C (High: 29.3°C, Low: 28.3°C)
    - Conditions: Light rain
    - Humidity: 80%
    - Wind Speed: 2.8 m/s
    
    **November 18, 2025 (Day After Tomorrow):**
    - Temperature: 27.7°C (High: 28.9°C, Low: 26.8°C)
    - Conditions: Light rain
    - Humidity: 78%
    - Wind Speed: 4.3 m/s
    
    Stay prepared for some light rain over the next couple of days!
    --------------------------------------------------------------------------------
    
    ================================================================================
    ORCHESTRATION COMPLETED
    ================================================================================



```python
# Example 4: Research-only query
research_only_query = "Research the best time to visit Iceland and what activities are popular there."

stream_orchestrator(research_only_query, show_details=True)
```

    ================================================================================
    QUERY: Research the best time to visit Iceland and what activities are popular there.
    ================================================================================
    
    [USER] Research the best time to visit Iceland and what activities are popular there.
    --------------------------------------------------------------------------------


    2025-11-17 15:33:04 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Calling 1 tool(s):
      - Spawning research agent: Research the best time to visit Iceland, considering weather, tourist seasons, a...
    --------------------------------------------------------------------------------


    2025-11-17 15:33:06 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-17 15:33:06 - research_agent - INFO - Searching: best time to visit Iceland weather tourist seasons special events
    2025-11-17 15:33:06 - research_agent - INFO - Searching: popular activities attractions Iceland natural wonders cultural experiences adventure activities
    2025-11-17 15:33:07 - research_agent - INFO - Found 5 results
    2025-11-17 15:33:08 - research_agent - INFO - Found 5 results
    2025-11-17 15:33:14 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [TOOL: task] Completed
    --------------------------------------------------------------------------------


    2025-11-17 15:33:20 - httpx - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    [ORCHESTRATOR] Here's a comprehensive overview of the best time to visit Iceland and popular activities you can enjoy there:
    
    ### Best Time to Visit Iceland
    
    1. **Summer (June-August):**
       - **Weather:** Warmest months with temperatures reaching the low 20°Cs. Enjoy long daylight hours.
       - **Tourist Season:** Peak tourist season due to favorable weather and extended daylight.
       - **Events:** Summer festivals and dry weather inland make it a prime time for travel.
    
    2. **Autumn (September-November):**
       - **Weather:** Cooler temperatures with vibrant autumn colors.
       - **Tourist Season:** Fewer crowds, ideal for budget travel.
       - **Events:** Northern Lights begin to appear.
    
    3. **Winter (November-December):**
       - **Weather:** Cold, with Iceland transforming into a winter wonderland.
       - **Tourist Season:** Popular for festive season activities.
       - **Events:** Christmas and New Year celebrations.
    
    4. **Spring (May):**
       - **Weather:** Nature comes back to life, and temperatures start to rise.
       - **Tourist Season:** Considered off-season, offering a quieter experience.
       - **Events:** Puffin watching and blooming landscapes.
    
    ### Popular Activities and Attractions in Iceland
    
    1. **Natural Wonders:**
       - **Waterfalls:** Visit iconic sites like Skógafoss and Seljalandsfoss.
       - **Northern Lights:** Best viewed in autumn and winter.
       - **Glaciers:** Explore Vatnajökull Glacier for hiking opportunities.
    
    2. **Cultural Experiences:**
       - **Festivals:** Enjoy summer festivals and Christmas celebrations.
       - **Architecture:** Don't miss Hallgrimskirkja in Reykjavik.
    
    3. **Adventure Activities:**
       - **Whale Watching:** Popular in Dalvik.
       - **Snorkeling:** Experience the clear waters at Silfra.
       - **Ice Caving and Snowmobiling:** Available in various locations.
       - **Hiking:** Explore remote destinations and black sand beaches like Reynisfjara.
    
    4. **Relaxation:**
       - **Blue Lagoon:** A famous geothermal spa for relaxation.
    
    These insights should help you plan a trip to Iceland that aligns with your interests and preferences. Enjoy your adventure!
    --------------------------------------------------------------------------------
    
    ================================================================================
    ORCHESTRATION COMPLETED
    ================================================================================


## Part 7: Understanding Sub-Agent Architecture Benefits

Let's examine what we've built and why it's powerful.

### Architecture Analysis

**What We Built:**

```
Orchestrator Agent
├── System Prompt: Coordination and delegation
├── Tools: task (for spawning sub-agents), planning, file I/O
└── Sub-agents:
    ├── Research Agent
    │   ├── System Prompt: Web research expertise
    │   └── Tools: internet_search, file I/O, planning
    └── Weather Agent
        ├── System Prompt: Weather data expertise
        └── Tools: get_current_weather, get_weather_forecast, file I/O
```

**Key Architectural Patterns:**

1. **Cognitive Specialization**
   - Each agent has a focused domain
   - Specialized system prompts
   - Minimal tool sets per agent

2. **Clear Responsibility Boundaries**
   - Orchestrator: Planning and coordination
   - Research Agent: Web search and synthesis
   - Weather Agent: Weather data retrieval

3. **Artifact-Based Communication**
   - Shared filesystem for results
   - Lightweight references passed between agents
   - No heavy data in messages

4. **Parallel Execution Capability**
   - Orchestrator can spawn multiple sub-agents
   - Independent tasks run simultaneously
   - Results collected and synthesized

**Benefits Over Monolithic Design:**

| Aspect | Monolithic Agent | Sub-Agent Architecture |
|--------|------------------|------------------------|
| Context Window | Filled with all tools | Only relevant tools per agent |
| System Prompt | Conflicting instructions | Focused expertise |
| Tool Selection | Confused with 20+ tools | Clear with 1-3 tools |
| Debugging | Hard to trace | Clear agent boundaries |
| Testing | Test everything together | Test each agent separately |
| Maintenance | Update affects everything | Update individual agents |
| Parallelization | Sequential execution | Parallel sub-agents |

**Production Readiness:**

This architecture includes:
- Structured logging for observability
- Error handling at each layer
- Input validation (Pydantic models)
- Retry logic with exponential backoff
- Connection pooling for performance
- Shared filesystem for state management

## Part 8: Design Patterns for Sub-Agent Boundaries

When designing your own multi-agent systems, use these patterns:

### Pattern 1: Functional Specialization
Divide by **task type**:
- Research Agent: Information gathering
- Analysis Agent: Data processing
- Writing Agent: Report generation

### Pattern 2: Domain Specialization
Divide by **knowledge domain**:
- Financial Agent: Stock data, analysis
- Medical Agent: Health information
- Legal Agent: Regulatory compliance

### Pattern 3: Process Stage Specialization
Divide by **pipeline stage**:
- Ingestion Agent: Data collection
- Cleaning Agent: Data validation
- Analysis Agent: Processing
- Output Agent: Report generation

### Pattern 4: Capability-Based Specialization
Divide by **technical capability**:
- Web Scraper Agent: HTTP requests
- Database Agent: SQL queries
- File Agent: File operations
- API Agent: External API calls

### Good Boundary Indicators

✅ **Good boundaries:**
- Distinct toolsets with minimal overlap
- Natural handoff points between agents
- Clear expertise separation
- Agents can be tested independently
- Reduces prompt complexity

❌ **Poor boundaries:**
- Many overlapping tools
- Frequent back-and-forth communication
- Tight coupling between agents
- Unclear ownership of tasks
- Shared context dependencies

## Part 9: Exercises and Extensions

Try these exercises to deepen your understanding:

### Exercise 1: Add a New Sub-Agent
Create a **Translation Agent** that:
- Uses a translation API or LLM
- Translates research findings into different languages
- Integrates with the orchestrator

### Exercise 2: Implement Error Recovery
Enhance the orchestrator to:
- Detect when a sub-agent fails
- Retry with modified parameters
- Fall back to alternative sub-agents
- Log failure patterns

### Exercise 3: Add Caching
Implement semantic caching:
- Cache research results for similar queries
- Cache weather data with TTL (time-to-live)
- Reduce API calls and improve response time

### Exercise 4: Metrics and Monitoring
Add observability:
- Track execution time per sub-agent
- Monitor tool call frequency
- Measure parallel vs sequential execution gains
- Log orchestration decisions

### Exercise 5: Build a Different System
Apply the patterns to a new domain:
- E-commerce: Product Agent + Inventory Agent + Recommendation Agent
- Healthcare: Symptom Agent + Research Agent + Appointment Agent
- Finance: Market Agent + Portfolio Agent + News Agent

## Summary

Congratulations! You've built a production-grade multi-agent system using the orchestrator pattern.

### What You Learned

1. **The Problem**: Monolithic agents with too many tools get confused and inefficient

2. **The Solution**: Sub-agent architectures with:
   - Cognitive specialization (focused expertise)
   - Clear responsibility boundaries
   - Parallel execution capability
   - Maintainable, testable components

3. **The Orchestrator Pattern**:
   - Coordinator agent handles planning and delegation
   - Worker agents execute specialized tasks
   - Artifact-based communication via shared filesystem
   - Results synthesized into cohesive responses

4. **Production-Grade Implementation**:
   - Input validation with Pydantic
   - Retry logic with exponential backoff
   - Structured logging for observability
   - Error handling at every layer
   - Connection pooling for performance

5. **Design Patterns**:
   - Functional specialization (by task type)
   - Domain specialization (by knowledge area)
   - Process stage specialization (by pipeline stage)
   - Capability-based specialization (by technical ability)

### Key Takeaways

- **Divide and conquer**: Break complex systems into specialized components
- **Clear boundaries**: Each agent should have focused expertise and minimal tools
- **Parallel execution**: Orchestrator pattern enables concurrent sub-agent work
- **Production patterns**: Validation, retry logic, logging, and error handling are essential
- **Real-world usage**: This pattern powers Claude Code, LangGraph, and enterprise systems

### Next Steps

- Apply these patterns to your own domain
- Experiment with different sub-agent configurations
- Add monitoring and metrics to track performance
- Consider implementing caching and optimization strategies
- Explore advanced coordination patterns (hierarchical orchestrators, bidding systems)

You now have the knowledge to build sophisticated, scalable multi-agent systems!
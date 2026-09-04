# Prompt Optimization Techniques for AI Agents

## Introduction

Prompt optimization is the art of crafting instructions that guide AI agents toward better reasoning and more reliable behavior. While modern LLMs like GPT-4o and Claude Sonnet 4.5 are highly capable and less prone to hallucination than earlier models, **strategic prompt optimization still significantly improves agent performance**.

**Why Prompt Optimization Matters:**
- Improves reasoning accuracy and tool selection
- Reduces hallucinations and made-up information
- Ensures consistent agent behavior across varied inputs
- Prevents premature stopping in multi-step workflows

**What You'll Learn:**

In this notebook, you'll learn the **Plan and Reflect** prompt optimization technique - a research-backed approach that encourages agents to think through tool selection before acting, then verify results.

We'll use streaming to make the agent's reasoning process visible, so you can see exactly how this optimization technique affects behavior.

**Prerequisites:**
- OpenAI API key stored in a `.env` file
- Familiarity with LangChain agents (from Level 2)
- Basic understanding of tool calling

## Setup: Load Dependencies

First, let's load our environment variables and import the necessary libraries.


```python
from dotenv import load_dotenv

load_dotenv()
```




    True




```python
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

print("Dependencies loaded successfully!")
```

    Dependencies loaded successfully!


## Create Weather Tools

Let's create two weather-related tools to give our agent more interesting capabilities:
1. **get_weather**: Returns current weather for a location
2. **get_forecast**: Returns a 3-day forecast for a location

These tools will help us demonstrate how prompt optimization affects tool selection and information handling.


```python
@tool
def get_weather(location: str) -> str:
    """Get current weather information for a specific location.
    
    Args:
        location: The city or location to get weather for
    
    Returns:
        A string describing the current weather conditions, or a message if data is not available
    """
    # Mock implementation with limited data
    weather_data = {
        "new york": "Sunny, 72°F",
        "london": "Cloudy, 59°F",
        "tokyo": "Clear, 68°F",
        "paris": "Rainy, 55°F",
    }
    
    location_key = location.lower().strip()
    
    if location_key in weather_data:
        return f"Weather in {location}: {weather_data[location_key]}"
    else:
        return f"Location not found"


@tool
def get_forecast(location: str) -> str:
    """Get 3-day weather forecast for a specific location.
    
    Args:
        location: The city or location to get forecast for
    
    Returns:
        A string with the 3-day forecast, or a message if data is not available
    """
    # Mock implementation with limited data
    forecast_data = {
        "new york": "Day 1: Sunny, 72°F | Day 2: Partly cloudy, 68°F | Day 3: Rainy, 65°F",
        "london": "Day 1: Cloudy, 59°F | Day 2: Rainy, 57°F | Day 3: Foggy, 56°F",
        "tokyo": "Day 1: Clear, 68°F | Day 2: Sunny, 70°F | Day 3: Partly cloudy, 67°F",
        "paris": "Day 1: Rainy, 55°F | Day 2: Cloudy, 58°F | Day 3: Sunny, 62°F",
    }
    
    location_key = location.lower().strip()
    
    if location_key in forecast_data:
        return f"3-day forecast for {location}: {forecast_data[location_key]}"
    else:
        return f"Forecast data not available for {location}"


tools = [get_weather, get_forecast]
print(f"Tools created: {[tool.name for tool in tools]}")
```

    Tools created: ['get_weather', 'get_forecast']


## Create Streaming Helper Function

To understand how prompt optimization affects agent behavior, we need to **see the agent's reasoning process**. We'll create a helper function that uses streaming to display:

- Which tools the agent decides to call
- What results the tools return
- The agent's final response

This visibility is crucial for understanding how different prompt optimizations change agent behavior.


```python
def ask_agent_with_streaming(agent, question: str):
    """Ask the agent a question and stream the response to show reasoning.
    
    Args:
        agent: The LangChain agent to query
        question: The question to ask
    """
    print(f"\n{'='*60}")
    print(f"Question: {question}")
    print(f"{'='*60}\n")
    
    # Stream with 'updates' mode to see what changes at each step
    for chunk in agent.stream(
        {"messages": [{"role": "user", "content": question}]},
        stream_mode="updates"
    ):
        # Check if this is a model update (includes tool calls or final response)
        if "model" in chunk:
            messages = chunk["model"].get("messages", [])
            for msg in messages:
                # Check for tool calls
                if hasattr(msg, 'tool_calls') and msg.tool_calls:
                    for tool_call in msg.tool_calls:
                        print(f"🔧 Tool Call: {tool_call['name']}")
                        print(f"   Input: {tool_call['args']}")
                # Check for final content
                elif hasattr(msg, 'content') and msg.content:
                    print(f"\n💬 Agent Response:\n{msg.content}")
        
        # Check if this is a tool result
        if "tools" in chunk:
            messages = chunk["tools"].get("messages", [])
            for msg in messages:
                if hasattr(msg, 'content'):
                    print(f"   Result: {msg.content}\n")
    
    print(f"\n{'='*60}\n")


print("Streaming helper function created!")
```

    Streaming helper function created!


---

# Technique: Plan and Reflect

## What is Plan and Reflect?

**Plan and Reflect** is a prompt optimization technique that encourages agents to:
1. **Plan**: Think through which tool to use and why before taking action
2. **Reflect**: Check if tool outputs make sense before proceeding

**Research shows** this technique provides a ~4% performance improvement by reducing impulsive tool selection and catching errors early.

## Creating an Agent with Plan and Reflect

Let's create an agent with a system prompt that includes Plan and Reflect instructions.


```python
# Configure the language model
model = ChatOpenAI(
    model="gpt-4o",
    temperature=0.1
)

# System prompt with Plan and Reflect optimization
plan_and_reflect_prompt = """You are a helpful weather assistant.

Before using any tool:
1. Think through which tool is most appropriate for the user's question
2. Consider whether you need current weather or forecast data

After receiving tool results:
1. Check if the output makes sense and answers the user's question
2. If the tool returns "not available", acknowledge this to the user
3. Only provide information that you received from tools
"""

# Create agent with custom system prompt
agent_with_planning = create_agent(
    model=model.with_config(configurable={"system_prompt": plan_and_reflect_prompt}),
    tools=tools
)

print("Agent with Plan and Reflect created!")
```

    Agent with Plan and Reflect created!


## Example 1: Current Weather Query

Let's test with a simple current weather question. Watch how the agent:
- Selects the appropriate tool
- Uses the tool result to answer


```python
ask_agent_with_streaming(
    agent_with_planning,
    "What's the weather like in Tokyo right now?"
)
```

    
    ============================================================
    Question: What's the weather like in Tokyo right now?
    ============================================================
    
    🔧 Tool Call: get_weather
       Input: {'location': 'Tokyo'}
       Result: Weather in Tokyo: Clear, 68°F
    
    
    💬 Agent Response:
    The current weather in Tokyo is clear with a temperature of 68°F.
    
    ============================================================
    


## Example 2: Forecast Query

Now let's ask for a forecast. Notice how the agent should select the forecast tool instead of the current weather tool.


```python
ask_agent_with_streaming(
    agent_with_planning,
    "What will the weather be like in London over the next few days?"
)
```

    
    ============================================================
    Question: What will the weather be like in London over the next few days?
    ============================================================
    
    🔧 Tool Call: get_forecast
       Input: {'location': 'London'}
       Result: 3-day forecast for London: Day 1: Cloudy, 59°F | Day 2: Rainy, 57°F | Day 3: Foggy, 56°F
    
    
    💬 Agent Response:
    The 3-day weather forecast for London is as follows:
    
    - **Day 1:** Cloudy, 59°F
    - **Day 2:** Rainy, 57°F
    - **Day 3:** Foggy, 56°F
    
    ============================================================
    


## Example 3: Complex Query

Let's try a more complex question that requires understanding which tool is appropriate.


```python
ask_agent_with_streaming(
    agent_with_planning,
    "Should I bring an umbrella to Paris this week?"
)
```

    
    ============================================================
    Question: Should I bring an umbrella to Paris this week?
    ============================================================
    
    🔧 Tool Call: get_weather
       Input: {'location': 'Paris'}
    🔧 Tool Call: get_forecast
       Input: {'location': 'Paris'}
       Result: Weather in Paris: Rainy, 55°F
    
       Result: 3-day forecast for Paris: Day 1: Rainy, 55°F | Day 2: Cloudy, 58°F | Day 3: Sunny, 62°F
    
    
    💬 Agent Response:
    Yes, you should bring an umbrella to Paris this week. The current weather is rainy, and the forecast for the next few days includes rain on the first day, followed by cloudy and then sunny weather.
    
    ============================================================
    


## What Does Plan and Reflect Improve?

The Plan and Reflect optimization helps agents:

- **Better tool selection**: Think through which tool is appropriate before acting
- **Error detection**: Catch when tool outputs don't make sense
- **Appropriate responses**: Acknowledge when data isn't available
- **Multi-step reasoning**: Plan sequences of tool calls more effectively

**Research shows** this technique provides a ~4% performance improvement by reducing impulsive tool selection and catching errors early.

---

## Summary

In this notebook, you learned the **Plan and Reflect** prompt optimization technique:

1. **Planning Phase**: Instruct the agent to think through which tool to use before acting
2. **Reflection Phase**: Have the agent verify tool outputs make sense before responding

This technique improves tool selection accuracy and helps agents handle edge cases like unavailable data more gracefully.

**Next Steps:**
- In the "Try It Yourself" exercise, you'll implement another powerful technique: **Tool Usage Over Guessing**
- This technique prevents agents from making up information when tool data is unavailable
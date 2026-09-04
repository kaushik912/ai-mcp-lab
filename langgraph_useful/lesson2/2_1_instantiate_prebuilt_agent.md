# Instantiating Prebuilt Agents with LangChain

## Introduction

Prebuilt agents are ready-to-use agent implementations that handle tool selection and execution automatically. In this notebook, you'll learn the basic pattern for creating and using an agent with LangChain's `create_agent` function.

**Prerequisites**: An OpenAI API key stored in a `.env` file

**What You'll Learn**:
- How to create an agent with `create_agent`
- How to define a simple tool
- How to invoke the agent with a request

## Step 1: Environment Setup

Load environment variables to access your API key.


```python
from dotenv import load_dotenv

load_dotenv()
```




    True



## Step 2: Import Required Libraries


```python
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
```

## Step 3: Configure the Language Model

Configure the model with a low temperature for consistent tool selection.


```python
model = ChatOpenAI(
    model="gpt-5",
    temperature=0.1
)
```

## Step 4: Create a Simple Tool

Tools are functions decorated with `@tool` that have type hints and a docstring. The agent reads the docstring to understand what the tool does.


```python
@tool
def get_weather(location: str) -> str:
    """Get current weather information for a specific location.
    
    Args:
        location: The city or location to get weather for
    
    Returns:
        A string describing the current weather conditions, or a message if data is not available
    """
    # Mock implementation - in production, call a real weather API
    weather_data = {
        "new york": "Sunny, 72°F",
        "london": "Cloudy, 59°F",
        "tokyo": "Clear, 68°F",
    }
    
    location_key = location.lower().strip()
    
    if location_key in weather_data:
        return f"Weather in {location}: {weather_data[location_key]}"
    else:
        return f"Weather data not available for {location}"
```

## Step 5: Create the Agent

Use `create_agent` to create the agent with the model and tools.


```python
agent = create_agent(
    model=model,
    tools=[get_weather]
)

print("Agent created successfully!")
```

    Agent created successfully!


## Step 6: Create a Helper Function & Run the Agent

Let's create a simple helper function that makes it easy to ask the agent questions and get responses.


```python
def ask_agent(question: str):
    """Helper function to ask the agent a question and print the answer."""
    result = agent.invoke({
        "messages": [{"role": "user", "content": question}]
    })
    
    # Extract and print the final answer
    final_message = result["messages"][-1]
    print(final_message.content)
```


```python
# Test with London
ask_agent("What's the weather like in London?")
```

    It’s currently cloudy in London, around 59°F (15°C). Would you like a short forecast for the rest of the day?



```python
ask_agent("What's the weather like in Singapore?")
```

    I’m not able to retrieve live weather for Singapore right now. Typically in November it’s hot and very humid (around 25–31°C/77–88°F, often feeling warmer) with frequent afternoon/evening thunderstorms and showers on many days. For up-to-the-minute conditions, check the Meteorological Service Singapore (weather.gov.sg) or the myENV app.

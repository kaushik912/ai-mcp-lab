# Evaluating Agent Trajectories with AgentEvals

## Introduction

When building AI agents, it's not enough to just evaluate the final output. We need to understand **how** the agent arrived at its answer - the intermediate steps it took, the tools it called, and the reasoning path it followed. This is called **agent trajectory evaluation**.

### What is an Agent Trajectory?

An agent trajectory is the complete sequence of actions an agent takes to solve a task:
- The user's initial request
- The agent's decision to call specific tools
- The arguments passed to those tools
- The responses from tools
- The agent's final answer

### Why Evaluate Trajectories?

Evaluating trajectories helps you:
- **Catch reasoning errors**: An agent might get the right answer for the wrong reason
- **Verify tool usage**: Ensure the agent calls appropriate tools with correct parameters
- **Improve reliability**: Identify when agents take inefficient or incorrect paths
- **Enable Evaluation-Driven Development**: Write tests for agent behavior before implementation

### What You'll Learn

In this notebook, you'll learn to:
1. Capture agent trajectories from LangChain agents
2. Use LLM-as-judge evaluation (no reference trajectory needed)
3. Compare trajectories against reference trajectories
4. Use strict matching for precise tool call verification

**Prerequisites**: 
- OpenAI API key in `.env` file
- Basic understanding of LangChain agents
- Familiarity with tool calling

## Section 1: Setup

First, we'll install the agentevals library and import all required dependencies.


```python
# Install agentevals (run this once)
!pip install agentevals -q
```

    
    [1m[[0m[34;49mnotice[0m[1;39;49m][0m[39;49m A new release of pip is available: [0m[31;49m24.2[0m[39;49m -> [0m[32;49m25.3[0m
    [1m[[0m[34;49mnotice[0m[1;39;49m][0m[39;49m To update, run: [0m[32;49mpip install --upgrade pip[0m



```python
# Load environment variables
from dotenv import load_dotenv

load_dotenv()
```




    True




```python
# Import required libraries
import json
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain_core.messages import AIMessage, ToolMessage, HumanMessage

# Import agentevals components
from agentevals.trajectory.llm import (
    create_trajectory_llm_as_judge,
    TRAJECTORY_ACCURACY_PROMPT,
    TRAJECTORY_ACCURACY_PROMPT_WITH_REFERENCE
)
from agentevals.trajectory.match import create_trajectory_match_evaluator

print("All libraries imported successfully!")
```

    All libraries imported successfully!


## Section 2: Create the Agent

We'll create a simple weather agent that uses a `get_weather` tool. This is the same agent from our earlier tutorial on instantiating prebuilt agents.


```python
# Configure the language model for the AGENT
# Using gpt-4o (standard model, no reasoning effort needed for the agent)
model = ChatOpenAI(
    model="gpt-4o",
    temperature=0.1
)

print("Model configured for agent")
```

    Model configured for agent



```python
# Create a simple weather tool
@tool
def get_weather(location: str) -> str:
    """Get current weather information for a specific location.
    
    Args:
        location: The city or location to get weather for
    
    Returns:
        A string describing the current weather conditions
    """
    # Mock implementation - in production, call a real weather API
    weather_data = {
        "san francisco": "Sunny, 72°F with light winds",
        "new york": "Partly cloudy, 65°F",
        "london": "Rainy, 55°F",
        "tokyo": "Clear, 68°F",
    }
    
    location_key = location.lower().strip()
    
    if location_key in weather_data:
        return f"Weather in {location}: {weather_data[location_key]}"
    else:
        return f"Weather data not available for {location}"

print("Weather tool created")
```

    Weather tool created



```python
# Create the agent
agent = create_agent(
    model=model,
    tools=[get_weather]
)

print("Agent created successfully!")
```

    Agent created successfully!


## Section 3: Capturing Agent Trajectories

An agent trajectory captures the complete sequence of messages during an agent's execution. Let's run the agent and examine what a trajectory looks like.


```python
# Run the agent and capture the trajectory
user_question = "What's the weather like in San Francisco?"

result = agent.invoke({
    "messages": [{"role": "user", "content": user_question}]
})

# Extract all messages from the agent's execution
trajectory_messages = result["messages"]

print(f"Agent executed {len(trajectory_messages)} steps")
print(f"\nFinal answer: {trajectory_messages[-1].content}")
```

    Agent executed 4 steps
    
    Final answer: The weather in San Francisco is currently sunny with a temperature of 72°F and light winds.



```python
# Let's examine the trajectory in detail
print("=== AGENT TRAJECTORY ===")
print()

for i, msg in enumerate(trajectory_messages):
    print(f"Step {i+1}: {type(msg).__name__}")
    
    if isinstance(msg, HumanMessage):
        print(f"  User: {msg.content}")
    elif isinstance(msg, AIMessage):
        if msg.tool_calls:
            print(f"  Agent decided to call tools:")
            for tc in msg.tool_calls:
                print(f"    - {tc['name']}({tc['args']})")
        if msg.content:
            print(f"  Agent response: {msg.content}")
    elif isinstance(msg, ToolMessage):
        print(f"  Tool result: {msg.content}")
    print()
```

    === AGENT TRAJECTORY ===
    
    Step 1: HumanMessage
      User: What's the weather like in San Francisco?
    
    Step 2: AIMessage
      Agent decided to call tools:
        - get_weather({'location': 'San Francisco'})
    
    Step 3: ToolMessage
      Tool result: Weather in San Francisco: Sunny, 72°F with light winds
    
    Step 4: AIMessage
      Agent response: The weather in San Francisco is currently sunny with a temperature of 72°F and light winds.
    


### Converting to AgentEvals Format

The agentevals library expects trajectories in OpenAI message format. Let's create a helper function to convert LangChain messages to this format.


```python
def convert_to_openai_format(messages):
    """Convert LangChain messages to OpenAI format for agentevals."""
    openai_messages = []
    
    for msg in messages:
        if isinstance(msg, HumanMessage):
            openai_messages.append({
                "role": "user",
                "content": msg.content
            })
        elif isinstance(msg, AIMessage):
            msg_dict = {"role": "assistant", "content": msg.content or ""}
            
            # Add tool calls if present
            if msg.tool_calls:
                msg_dict["tool_calls"] = [
                    {
                        "function": {
                            "name": tc["name"],
                            "arguments": json.dumps(tc["args"])
                        }
                    }
                    for tc in msg.tool_calls
                ]
            
            openai_messages.append(msg_dict)
        elif isinstance(msg, ToolMessage):
            openai_messages.append({
                "role": "tool",
                "content": msg.content
            })
    
    return openai_messages

# Convert the trajectory
trajectory = convert_to_openai_format(trajectory_messages)

print("Trajectory converted to OpenAI format:")
print(json.dumps(trajectory, indent=2))
```

    Trajectory converted to OpenAI format:
    [
      {
        "role": "user",
        "content": "What's the weather like in San Francisco?"
      },
      {
        "role": "assistant",
        "content": "",
        "tool_calls": [
          {
            "function": {
              "name": "get_weather",
              "arguments": "{\"location\": \"San Francisco\"}"
            }
          }
        ]
      },
      {
        "role": "tool",
        "content": "Weather in San Francisco: Sunny, 72\u00b0F with light winds"
      },
      {
        "role": "assistant",
        "content": "The weather in San Francisco is currently sunny with a temperature of 72\u00b0F and light winds."
      }
    ]


## Section 4: Trajectory LLM-as-Judge Evaluation

The most flexible evaluation method is to use an LLM as a judge. The judge evaluates whether the agent:
- Called appropriate tools
- Used correct arguments
- Provided an accurate final answer

**Key advantage**: You don't need a reference trajectory - the judge evaluates based on the user's question and the agent's behavior.


```python
# Create an LLM-as-judge evaluator
# Using gpt-5-mini with reasoning effort for the EVALUATOR
# Pass the ChatOpenAI instance to the 'judge' parameter
judge_model = ChatOpenAI(
    model='gpt-5-mini',
    reasoning_effort='medium'  # Options: "low", "medium", "high"
)

trajectory_evaluator = create_trajectory_llm_as_judge(
    prompt=TRAJECTORY_ACCURACY_PROMPT,
    judge=judge_model  # Pass ChatOpenAI instance here!
)

print("Evaluator created successfully with gpt-5-mini + reasoning_effort!")
```

    Evaluator created successfully with gpt-5-mini + reasoning_effort!


### Understanding the Model Architecture

Notice that we're using **different models** for different purposes:

- **Agent Model** (`gpt-4o`): Handles the actual task - answering user questions
- **Judge Model** (`gpt-5-mini` with `reasoning_effort`): Evaluates the agent's trajectory

This separation allows you to:
1. Use faster/cheaper models for the agent during development
2. Use more powerful reasoning models for thorough evaluation
3. Optimize each component independently

The `judge` parameter in `create_trajectory_llm_as_judge` accepts a `ChatOpenAI` instance, which lets us configure reasoning effort and other parameters specifically for evaluation.


```python
# Evaluate the trajectory
evaluation_result = trajectory_evaluator(outputs=trajectory)

print("=== EVALUATION RESULT ===")
print(f"Score: {evaluation_result['score']}")
print(f"\nReasoning:")
print(evaluation_result['comment'])
```

    === EVALUATION RESULT ===
    Score: True
    
    Reasoning:
    The goal is to answer the user's question about the weather in San Francisco. The trajectory shows the assistant calling a weather tool with the correct location, receiving a clear tool response (Sunny, 72°F with light winds), and then communicating that information back to the user. The steps are logically connected (question → tool call → tool result → user-facing reply), show clear progression, and are efficient (no unnecessary steps). The assistant's final message accurately restates the tool output. Thus, the score should be: true.

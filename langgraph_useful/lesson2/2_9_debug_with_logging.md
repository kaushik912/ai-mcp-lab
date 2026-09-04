# Debugging Agent Executions with Logging

When building AI agents, one of the biggest challenges is understanding what's happening inside the agent's decision-making process. Unlike traditional software where execution is deterministic, agents involve:

- Non-deterministic LLM outputs
- Multi-step workflows with hidden reasoning
- Multiple failure points (LLM, tools, APIs, state transitions)

**The solution: Comprehensive logging at every decision point.**

## What You'll Learn

In this notebook, you'll learn:

1. **Part A: Custom Agent Logging** - How to add structured logging to a custom ReAct agent
   - What to log: LLM inputs/outputs, tool calls, observations, state transitions
   - How to implement structured logging with JSON
   - Analyzing logs to debug agent behavior

2. **Part B: Framework Debug Modes** - How to use LangChain's built-in debugging capabilities
   - Global debug and verbose modes
   - Per-invocation configuration
   - Comparing custom vs. framework logging approaches

## Prerequisites

- An OpenAI API key stored in a `.env` file
- Basic understanding of ReAct agents and tool calling
- Familiarity with Python's logging module (helpful but not required)

---

# Part A: Adding Structured Logging to Custom ReAct Agent

When you build a custom agent from scratch, you have complete control over the agent loop. This means you also need to manually implement logging to understand what's happening at each step.

**Why custom agents need manual logging:**
- No built-in observability from frameworks
- You control what gets logged and how
- Critical for production debugging and monitoring
- Enables structured analysis of agent behavior

We'll start with a basic ReAct agent that helps users calculate fruit prices, then add comprehensive logging to track its execution.

## Setup: Import Dependencies and Configure Logging

First, let's import our dependencies and set up Python's logging system. We'll configure it to show INFO level messages with timestamps.


```python
from openai import OpenAI
import re
import logging
import json
from datetime import datetime, timezone
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Initialize OpenAI client
client = OpenAI()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

print("Dependencies loaded and logging configured.")
```

    Dependencies loaded and logging configured.


## Implement Structured Logging Helper

Structured logging means logging data in a consistent format (like JSON) rather than free-form text. This makes logs:
- **Machine-readable**: Easy to parse and analyze programmatically
- **Searchable**: Query specific fields (e.g., all tool calls)
- **Consistent**: Same format across all log entries

Let's create a helper function that logs events in JSON format with timestamps.


```python
def log_event(event_type, data):
    """Log an event with structured data in JSON format.
    
    Args:
        event_type: Type of event (e.g., 'llm_input', 'tool_call', 'observation')
        data: Dictionary containing event-specific data
    """
    log_entry = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "event": event_type,
        "data": data
    }
    logger.info(json.dumps(log_entry))

# Test the logging function
log_event("test_event", {"message": "Logging system ready"})
```

    2025-11-05 12:06:08 - INFO - {"timestamp": "2025-11-05T04:06:08.243333+00:00", "event": "test_event", "data": {"message": "Logging system ready"}}


## Define Agent Class with Logging

The `Agent` class handles interactions with the OpenAI API. We'll add logging to track:
1. **Messages added** to the conversation
2. **LLM inputs** (how many messages, preview of the last message)
3. **LLM outputs** (response content and token usage)

This helps us understand:
- What context the LLM receives
- How the LLM responds
- Token consumption patterns


```python
class Agent:
    """Agent class that handles OpenAI API interactions with logging."""
    
    def __init__(self, system=""):
        self.system = system
        self.messages = []
        if self.system:
            self.messages.append({"role": "system", "content": system})
            log_event("agent_initialized", {
                "system_prompt_length": len(system)
            })
    
    def __call__(self, message):
        """Add user message and get agent response."""
        # Log incoming user message
        log_event("message_added", {
            "role": "user",
            "content_preview": message[:100] + "..." if len(message) > 100 else message
        })
        
        self.messages.append({"role": "user", "content": message})
        result = self.execute()
        self.messages.append({"role": "assistant", "content": result})
        
        # Log assistant response
        log_event("message_added", {
            "role": "assistant",
            "content_preview": result[:100] + "..." if len(result) > 100 else result
        })
        
        return result
    
    def execute(self):
        """Execute API call to OpenAI with logging."""
        # Log LLM input
        log_event("llm_input", {
            "message_count": len(self.messages),
            "last_message_preview": str(self.messages[-1])[:200]
        })
        
        completion = client.chat.completions.create(
            model="gpt-4o",
            temperature=0,
            messages=self.messages
        )
        
        result = completion.choices[0].message.content
        
        # Log LLM output with token usage
        log_event("llm_output", {
            "content_length": len(result),
            "content_preview": result[:200] + "..." if len(result) > 200 else result,
            "tokens": {
                "prompt": completion.usage.prompt_tokens,
                "completion": completion.usage.completion_tokens,
                "total": completion.usage.total_tokens
            },
            "model": completion.model
        })
        
        return result

print("Agent class defined with logging capabilities.")
```

    Agent class defined with logging capabilities.


## Define ReAct System Prompt

This prompt instructs the agent to follow the ReAct (Reasoning and Acting) pattern:
- **Thought**: Reason about the problem
- **Action**: Call a tool
- **PAUSE**: Wait for observation
- **Observation**: Receive tool result
- **Answer**: Provide final response

The agent will use two tools: `get_fruit_price` and `calculate_total_price`.


```python
prompt = """
You run in a loop of Thought, Action, PAUSE, Observation.
At the end of the loop you output an Answer
Use Thought to describe your thoughts about the question you have been asked.
Use Action to run one of the actions available to you - then return PAUSE.
Observation will be the result of running those actions.

Your available actions are:

get_fruit_price:
e.g. get_fruit_price: apple
Returns the price of a fruit

calculate_total_price:
e.g. calculate_total_price: 2 apples, 3 oranges
Calculates the total price for the given fruits and quantities

Example session:

Question: What is the price of 2 bananas and 3 oranges?
Thought: I need to find the price of bananas and oranges first
Action: get_fruit_price: banana
PAUSE

You will be called again with this:

Observation: $1.2

You then output:

Action: get_fruit_price: orange
PAUSE

You will be called again with this:

Observation: $1.3

You then output:

Action: calculate_total_price: 2 bananas, 3 oranges
PAUSE

You will be called again with this:

Observation: $6.3

You then output:

Answer: The total price for 2 bananas and 3 oranges is $6.3
""".strip()

print("ReAct system prompt defined.")
```

    ReAct system prompt defined.


## Define Tool Functions

These are the tools the agent can use:
- `get_fruit_price`: Looks up the price of a single fruit
- `calculate_total_price`: Calculates the total cost for multiple fruits


```python
def get_fruit_price(fruit):
    """Get the price of a specific fruit."""
    prices = {
        "apple": 1.5,
        "banana": 1.2,
        "orange": 1.3,
        "grape": 2.0
    }
    return prices.get(fruit.lower(), "Unknown fruit")

def calculate_total_price(fruits_str):
    """Calculate total price for multiple fruits.
    
    Args:
        fruits_str: String like "2 bananas, 3 oranges"
    """
    items = fruits_str.split(',')
    total = 0
    for item in items:
        parts = item.strip().split()
        quantity = int(parts[0])
        fruit = parts[1].rstrip('s')  # Remove plural 's'
        price = get_fruit_price(fruit)
        if isinstance(price, str):
            return price
        total += quantity * price
    return total

# Test the tools
print(f"Apple price: ${get_fruit_price('apple')}")
print(f"Total for 2 apples, 3 bananas: ${calculate_total_price('2 apples, 3 bananas')}")
```

    Apple price: $1.5
    Total for 2 apples, 3 bananas: $6.6


## Define Query Function with Comprehensive Logging

The `query` function implements the agent loop. We'll add logging for:
1. **Loop iterations**: Track which turn we're on
2. **Tool planning**: What action the LLM wants to take
3. **Tool execution**: What the tool returned
4. **Final answer**: When the agent completes

This gives us a complete trace of the agent's decision-making process.


```python
# Regex to parse action lines from LLM output
action_re = re.compile(r'^Action: (\w+): (.*)$', re.MULTILINE)

# Map of available actions
known_actions = {
    "get_fruit_price": get_fruit_price,
    "calculate_total_price": calculate_total_price
}

def query(question, max_turns=5):
    """Run agent loop with comprehensive logging.
    
    Args:
        question: User's question
        max_turns: Maximum number of agent turns
    """
    log_event("query_started", {
        "question": question,
        "max_turns": max_turns
    })
    
    agent = Agent(prompt)
    next_prompt = question
    i = 0
    
    while i < max_turns:
        i += 1
        
        # Log loop iteration
        log_event("turn_started", {
            "turn": i,
            "max_turns": max_turns,
            "prompt": next_prompt
        })
        
        result = agent(next_prompt)
        print(f"\n=== Turn {i}/{max_turns} ===")
        print(result)
        
        # Parse actions from result
        actions = [
            action_re.match(a)
            for a in result.split('\n')
            if action_re.match(a)
        ]
        
        if actions:
            # Extract action and input
            action, action_input = actions[0].groups()
            
            # Log tool planning
            log_event("tool_planned", {
                "action": action,
                "action_input": action_input,
                "turn": i
            })
            
            if action not in known_actions:
                error_msg = f"Unknown action: {action}: {action_input}"
                log_event("error", {
                    "type": "unknown_action",
                    "action": action,
                    "message": error_msg
                })
                raise Exception(error_msg)
            
            print(f"\n-- Running {action} with input: {action_input}")
            
            # Execute tool
            observation = known_actions[action](action_input)
            
            # Log tool execution result
            log_event("tool_executed", {
                "action": action,
                "action_input": action_input,
                "observation": observation,
                "turn": i
            })
            
            print(f"Observation: {observation}")
            next_prompt = f"Observation: {observation}"
        else:
            # No more actions - agent is done
            log_event("query_completed", {
                "total_turns": i,
                "final_answer": result
            })
            return

print("Query function defined with comprehensive logging.")
```

    Query function defined with comprehensive logging.


## Run Example with Full Logging

Now let's run our agent with the logging system enabled. Watch how the logs provide visibility into:
- Each turn of the agent loop
- What the LLM receives and responds
- Which tools are called and what they return
- Token usage for each LLM call


```python
query("What is the price of 2 bananas and 3 oranges?")
```

    2025-11-05 12:06:12 - INFO - {"timestamp": "2025-11-05T04:06:12.997493+00:00", "event": "query_started", "data": {"question": "What is the price of 2 bananas and 3 oranges?", "max_turns": 5}}
    2025-11-05 12:06:12 - INFO - {"timestamp": "2025-11-05T04:06:12.999280+00:00", "event": "agent_initialized", "data": {"system_prompt_length": 1107}}
    2025-11-05 12:06:13 - INFO - {"timestamp": "2025-11-05T04:06:13.001319+00:00", "event": "turn_started", "data": {"turn": 1, "max_turns": 5, "prompt": "What is the price of 2 bananas and 3 oranges?"}}
    2025-11-05 12:06:13 - INFO - {"timestamp": "2025-11-05T04:06:13.003543+00:00", "event": "message_added", "data": {"role": "user", "content_preview": "What is the price of 2 bananas and 3 oranges?"}}
    2025-11-05 12:06:13 - INFO - {"timestamp": "2025-11-05T04:06:13.004151+00:00", "event": "llm_input", "data": {"message_count": 2, "last_message_preview": "{'role': 'user', 'content': 'What is the price of 2 bananas and 3 oranges?'}"}}
    2025-11-05 12:06:14 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.557311+00:00", "event": "llm_output", "data": {"content_length": 101, "content_preview": "Thought: I need to find the price of bananas and oranges first.\nAction: get_fruit_price: banana\nPAUSE", "tokens": {"prompt": 297, "completion": 25, "total": 322}, "model": "gpt-4o-2024-08-06"}}
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.558008+00:00", "event": "message_added", "data": {"role": "assistant", "content_preview": "Thought: I need to find the price of bananas and oranges first.\nAction: get_fruit_price: banana\nPAUS..."}}
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.558543+00:00", "event": "tool_planned", "data": {"action": "get_fruit_price", "action_input": "banana", "turn": 1}}
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.559036+00:00", "event": "tool_executed", "data": {"action": "get_fruit_price", "action_input": "banana", "observation": 1.2, "turn": 1}}
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.559802+00:00", "event": "turn_started", "data": {"turn": 2, "max_turns": 5, "prompt": "Observation: 1.2"}}
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.560840+00:00", "event": "message_added", "data": {"role": "user", "content_preview": "Observation: 1.2"}}
    2025-11-05 12:06:14 - INFO - {"timestamp": "2025-11-05T04:06:14.562698+00:00", "event": "llm_input", "data": {"message_count": 4, "last_message_preview": "{'role': 'user', 'content': 'Observation: 1.2'}"}}


    
    === Turn 1/5 ===
    Thought: I need to find the price of bananas and oranges first.
    Action: get_fruit_price: banana
    PAUSE
    
    -- Running get_fruit_price with input: banana
    Observation: 1.2


    2025-11-05 12:06:15 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.221319+00:00", "event": "llm_output", "data": {"content_length": 37, "content_preview": "Action: get_fruit_price: orange\nPAUSE", "tokens": {"prompt": 336, "completion": 11, "total": 347}, "model": "gpt-4o-2024-08-06"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.222372+00:00", "event": "message_added", "data": {"role": "assistant", "content_preview": "Action: get_fruit_price: orange\nPAUSE"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.222972+00:00", "event": "tool_planned", "data": {"action": "get_fruit_price", "action_input": "orange", "turn": 2}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.224034+00:00", "event": "tool_executed", "data": {"action": "get_fruit_price", "action_input": "orange", "observation": 1.3, "turn": 2}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.224969+00:00", "event": "turn_started", "data": {"turn": 3, "max_turns": 5, "prompt": "Observation: 1.3"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.225761+00:00", "event": "message_added", "data": {"role": "user", "content_preview": "Observation: 1.3"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.226284+00:00", "event": "llm_input", "data": {"message_count": 6, "last_message_preview": "{'role': 'user', 'content': 'Observation: 1.3'}"}}


    
    === Turn 2/5 ===
    Action: get_fruit_price: orange
    PAUSE
    
    -- Running get_fruit_price with input: orange
    Observation: 1.3


    2025-11-05 12:06:15 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.967681+00:00", "event": "llm_output", "data": {"content_length": 57, "content_preview": "Action: calculate_total_price: 2 bananas, 3 oranges\nPAUSE", "tokens": {"prompt": 361, "completion": 16, "total": 377}, "model": "gpt-4o-2024-08-06"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.969541+00:00", "event": "message_added", "data": {"role": "assistant", "content_preview": "Action: calculate_total_price: 2 bananas, 3 oranges\nPAUSE"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.970646+00:00", "event": "tool_planned", "data": {"action": "calculate_total_price", "action_input": "2 bananas, 3 oranges", "turn": 3}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.971838+00:00", "event": "tool_executed", "data": {"action": "calculate_total_price", "action_input": "2 bananas, 3 oranges", "observation": 6.300000000000001, "turn": 3}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.972933+00:00", "event": "turn_started", "data": {"turn": 4, "max_turns": 5, "prompt": "Observation: 6.300000000000001"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.973518+00:00", "event": "message_added", "data": {"role": "user", "content_preview": "Observation: 6.300000000000001"}}
    2025-11-05 12:06:15 - INFO - {"timestamp": "2025-11-05T04:06:15.974028+00:00", "event": "llm_input", "data": {"message_count": 8, "last_message_preview": "{'role': 'user', 'content': 'Observation: 6.300000000000001'}"}}


    
    === Turn 3/5 ===
    Action: calculate_total_price: 2 bananas, 3 oranges
    PAUSE
    
    -- Running calculate_total_price with input: 2 bananas, 3 oranges
    Observation: 6.300000000000001


    2025-11-05 12:06:18 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-05 12:06:18 - INFO - {"timestamp": "2025-11-05T04:06:18.292621+00:00", "event": "llm_output", "data": {"content_length": 61, "content_preview": "Answer: The total price for 2 bananas and 3 oranges is $6.30.", "tokens": {"prompt": 395, "completion": 19, "total": 414}, "model": "gpt-4o-2024-08-06"}}
    2025-11-05 12:06:18 - INFO - {"timestamp": "2025-11-05T04:06:18.293958+00:00", "event": "message_added", "data": {"role": "assistant", "content_preview": "Answer: The total price for 2 bananas and 3 oranges is $6.30."}}
    2025-11-05 12:06:18 - INFO - {"timestamp": "2025-11-05T04:06:18.294731+00:00", "event": "query_completed", "data": {"total_turns": 4, "final_answer": "Answer: The total price for 2 bananas and 3 oranges is $6.30."}}


    
    === Turn 4/5 ===
    Answer: The total price for 2 bananas and 3 oranges is $6.30.


## Analyzing the Logs

The structured logs above tell us a detailed story:

### What We Can Learn from Logs:

1. **Agent Initialization**: See the system prompt length and setup

2. **Each Turn**:
   - What prompt the agent receives
   - How many messages are in the context
   - Token usage (prompt + completion tokens)
   - What the LLM decided to do

3. **Tool Execution**:
   - Which tool was called
   - What input it received
   - What observation was returned

4. **Debugging Scenarios**:
   - **Agent gets stuck in a loop?** Check turn logs to see if it's repeating actions
   - **Wrong final answer?** Trace through tool executions to find incorrect observations
   - **High costs?** Look at token usage per turn
   - **Unexpected tool calls?** Review LLM output to understand reasoning

### Best Practices for Custom Agent Logging:

- Use appropriate log levels (INFO for normal flow, WARNING for issues, ERROR for failures)
- Include timestamps for performance analysis
- Log both inputs and outputs for reproducibility
- Balance detail with cost (too much logging can slow things down)
- Structure logs consistently (JSON format) for easy parsing
- Consider logging to files for production systems

---

# Part B: Using Debug Mode with LangChain Prebuilt Agents

When using frameworks like LangChain, you get built-in debugging capabilities. Instead of manually adding logging, you can enable debug modes to automatically see what's happening.

**Advantages of framework debug modes:**
- No manual instrumentation needed
- Consistent logging across all components
- Shows internal framework operations
- Easy to toggle on/off

**Trade-offs:**
- Less control over what's logged
- Can be very verbose
- May not log custom metrics you care about

Let's explore LangChain's debug capabilities.

## Setup: Import LangChain Components

We'll create a simple LangChain agent using the `create_agent` function with a weather tool to demonstrate debug modes.


```python
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

# Initialize LLM
model = ChatOpenAI(model="gpt-4o", temperature=0.1)

print("LangChain components imported.")
```

    LangChain components imported.


## Define a Simple Tool

Let's create a weather lookup tool using LangChain's `@tool` decorator.


```python
@tool
def get_weather(location: str) -> str:
    """Get current weather information for a specific location.
    
    Args:
        location: The city or location to get weather for
    
    Returns:
        A string describing the current weather conditions
    """
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

tools = [get_weather]
print(f"Tool defined: {get_weather.name}")
```

    Tool defined: get_weather


## Create Agent and Helper Function

First, let's create the agent and a helper function, then run it normally without any debug modes to see the baseline output.


```python
# Create agent with create_agent function
agent = create_agent(model=model, tools=tools)

# Helper function to invoke the agent
def ask_agent(question: str):
    """Helper function to ask the agent a question and print the answer."""
    result = agent.invoke({
        "messages": [{"role": "user", "content": question}]
    })
    
    # Extract and print the final answer
    final_message = result["messages"][-1]
    print(final_message.content)

# Run without debug mode
print("=== Running WITHOUT Debug Mode ===\n")
ask_agent("What's the weather like in London?")
```

    === Running WITHOUT Debug Mode ===
    


    2025-11-05 12:33:30 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"
    2025-11-05 12:33:30 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    The current weather in London is cloudy with a temperature of 59°F.


Notice how we only see the final answer. We don't know:
- What the agent was thinking during its ReAct loop
- Which tool was called
- What the tool returned
- How many reasoning steps it took

Let's fix that with debug modes!

## Enable Debug Mode with Streaming

Since `create_agent` creates a LangGraph-based agent, we use **streaming with debug mode** to see what's happening internally. LangGraph supports different stream modes:

- **`values`**: Shows the state at each step (default)
- **`updates`**: Shows what changed at each step
- **`debug`**: Most verbose - shows all internal operations

Let's see the difference between normal invocation and streaming with debug.


```python
# First, let's see streaming with "values" mode (shows state at each step)
print("=== Streaming with 'values' mode ===\n")

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What's the weather like in Tokyo?"}]},
    stream_mode="values"
):
    # Each chunk is the full state at that step
    # Let's print just the last message from each step
    if "messages" in chunk:
        last_msg = chunk["messages"][-1]
        print(f"Step: {last_msg}")
        print("---")
```

    === Streaming with 'values' mode ===
    
    Step: content="What's the weather like in Tokyo?" additional_kwargs={} response_metadata={} id='20ecefdd-3aa2-4872-8ca3-2d3d7dc6737a'
    ---


    2025-11-05 12:33:43 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    Step: content='' additional_kwargs={'refusal': None} response_metadata={'token_usage': {'completion_tokens': 14, 'prompt_tokens': 77, 'total_tokens': 91, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPVRk3m0I6seCR4bBh2uIZPboeH5', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None} id='lc_run--f934de3c-c9de-415f-97f1-b639c3d6c193-0' tool_calls=[{'name': 'get_weather', 'args': {'location': 'Tokyo'}, 'id': 'call_6hWc1m9wEL3tMlholy8qsK2I', 'type': 'tool_call'}] usage_metadata={'input_tokens': 77, 'output_tokens': 14, 'total_tokens': 91, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}}
    ---
    Step: content='Weather in Tokyo: Clear, 68°F' name='get_weather' id='dd74dfc8-7fd9-47a6-8f38-1e119c047659' tool_call_id='call_6hWc1m9wEL3tMlholy8qsK2I'
    ---


    2025-11-05 12:33:43 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    Step: content='The current weather in Tokyo is clear with a temperature of 68°F.' additional_kwargs={'refusal': None} response_metadata={'token_usage': {'completion_tokens': 16, 'prompt_tokens': 108, 'total_tokens': 124, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPVSA2MkX9Bl33nblKA7qBfvqe9A', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None} id='lc_run--3c6edc7e-649f-4962-b179-4fd18c864f14-0' usage_metadata={'input_tokens': 108, 'output_tokens': 16, 'total_tokens': 124, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}}
    ---


## Run Agent with Debug Stream Mode

Now let's use `stream_mode="debug"` to see the most verbose output, including all internal LangGraph operations.


```python
print("=== Streaming with 'debug' mode ===\n")

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What's the weather like in New York?"}]},
    stream_mode="debug"
):
    # Debug mode shows all internal operations
    print(chunk)
    print("---")
```

    === Streaming with 'debug' mode ===
    
    {'step': 1, 'timestamp': '2025-11-05T04:33:58.636503+00:00', 'type': 'task', 'payload': {'id': '59c6df2b-0080-21db-bd18-400267fb7617', 'name': 'model', 'input': {'messages': [HumanMessage(content="What's the weather like in New York?", additional_kwargs={}, response_metadata={}, id='29778a80-c7da-4ae0-bba9-ed57b89067c9')]}, 'triggers': ('branch:to:model',)}}
    ---


    2025-11-05 12:33:59 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    {'step': 1, 'timestamp': '2025-11-05T04:33:59.520770+00:00', 'type': 'task_result', 'payload': {'id': '59c6df2b-0080-21db-bd18-400267fb7617', 'name': 'model', 'error': None, 'result': {'messages': [AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 15, 'prompt_tokens': 78, 'total_tokens': 93, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPVh9xChiXvALhhK7AnkECBBspwQ', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--a6ac782c-505a-48b8-bed0-70dc704f4449-0', tool_calls=[{'name': 'get_weather', 'args': {'location': 'New York'}, 'id': 'call_9VUEfXoFDSeHemMcyvpKhSSJ', 'type': 'tool_call'}], usage_metadata={'input_tokens': 78, 'output_tokens': 15, 'total_tokens': 93, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}, 'interrupts': []}}
    ---
    {'step': 2, 'timestamp': '2025-11-05T04:33:59.521031+00:00', 'type': 'task', 'payload': {'id': 'ca7feb42-821e-a847-801c-f08bd4eb5992', 'name': 'tools', 'input': {'__type': 'tool_call_with_context', 'tool_call': {'name': 'get_weather', 'args': {'location': 'New York'}, 'id': 'call_9VUEfXoFDSeHemMcyvpKhSSJ', 'type': 'tool_call'}, 'state': {'messages': [HumanMessage(content="What's the weather like in New York?", additional_kwargs={}, response_metadata={}, id='29778a80-c7da-4ae0-bba9-ed57b89067c9'), AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 15, 'prompt_tokens': 78, 'total_tokens': 93, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPVh9xChiXvALhhK7AnkECBBspwQ', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--a6ac782c-505a-48b8-bed0-70dc704f4449-0', tool_calls=[{'name': 'get_weather', 'args': {'location': 'New York'}, 'id': 'call_9VUEfXoFDSeHemMcyvpKhSSJ', 'type': 'tool_call'}], usage_metadata={'input_tokens': 78, 'output_tokens': 15, 'total_tokens': 93, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}}, 'triggers': ('__pregel_push',)}}
    ---
    {'step': 2, 'timestamp': '2025-11-05T04:33:59.524138+00:00', 'type': 'task_result', 'payload': {'id': 'ca7feb42-821e-a847-801c-f08bd4eb5992', 'name': 'tools', 'error': None, 'result': {'messages': [ToolMessage(content='Weather in New York: Sunny, 72°F', name='get_weather', id='9c9701ce-60c7-4ea7-9142-051fd1a32ac9', tool_call_id='call_9VUEfXoFDSeHemMcyvpKhSSJ')]}, 'interrupts': []}}
    ---
    {'step': 3, 'timestamp': '2025-11-05T04:33:59.524513+00:00', 'type': 'task', 'payload': {'id': '9d0ce063-6c00-f618-f094-b942eedc1487', 'name': 'model', 'input': {'messages': [HumanMessage(content="What's the weather like in New York?", additional_kwargs={}, response_metadata={}, id='29778a80-c7da-4ae0-bba9-ed57b89067c9'), AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 15, 'prompt_tokens': 78, 'total_tokens': 93, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPVh9xChiXvALhhK7AnkECBBspwQ', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--a6ac782c-505a-48b8-bed0-70dc704f4449-0', tool_calls=[{'name': 'get_weather', 'args': {'location': 'New York'}, 'id': 'call_9VUEfXoFDSeHemMcyvpKhSSJ', 'type': 'tool_call'}], usage_metadata={'input_tokens': 78, 'output_tokens': 15, 'total_tokens': 93, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}}), ToolMessage(content='Weather in New York: Sunny, 72°F', name='get_weather', id='9c9701ce-60c7-4ea7-9142-051fd1a32ac9', tool_call_id='call_9VUEfXoFDSeHemMcyvpKhSSJ')]}, 'triggers': ('branch:to:model',)}}
    ---


    2025-11-05 12:34:00 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    {'step': 3, 'timestamp': '2025-11-05T04:34:00.599523+00:00', 'type': 'task_result', 'payload': {'id': '9d0ce063-6c00-f618-f094-b942eedc1487', 'name': 'model', 'error': None, 'result': {'messages': [AIMessage(content='The current weather in New York is sunny with a temperature of 72°F.', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 17, 'prompt_tokens': 111, 'total_tokens': 128, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPVibuBrRvOsHK84yK1qmBY5qUxY', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None}, id='lc_run--e35963d9-9481-4203-88f1-f3a6af8e9463-0', usage_metadata={'input_tokens': 111, 'output_tokens': 17, 'total_tokens': 128, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}, 'interrupts': []}}
    ---


## Understanding the Debug Output

With `stream_mode="debug"`, you can see:

1. **Task Execution**: Each task/node in the LangGraph being executed
2. **Payload Information**: The data being passed between nodes
3. **Task Results**: What each node returns
4. **Metadata**: Timing, task IDs, and execution details

The debug output shows the internal LangGraph operations, which is useful for:
- Understanding the agent's execution flow
- Debugging graph node issues
- Seeing exactly when and how tools are called
- Tracking state changes through the graph

**Note**: Debug mode is very verbose! Use `stream_mode="values"` for a cleaner view focused on state changes, or `stream_mode="updates"` to see just what changed at each step.

## Using "updates" Stream Mode

For a middle ground between minimal and verbose output, use `stream_mode="updates"` to see only what changed at each step.


```python
print("=== Streaming with 'updates' mode ===\n")

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What's the weather like in London?"}]},
    stream_mode="updates"
):
    # Updates mode shows only what changed
    print(chunk)
    print("---")
```

    === Streaming with 'updates' mode ===
    


    2025-11-05 12:34:18 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    {'model': {'messages': [AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 14, 'prompt_tokens': 77, 'total_tokens': 91, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPW05qcTDvKytaa2Qm6ZqL1BzMfC', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--dc0a0bd3-fbc5-46ac-afaf-483f097abe02-0', tool_calls=[{'name': 'get_weather', 'args': {'location': 'London'}, 'id': 'call_NjCJnualclh47mZCxjaZhnsH', 'type': 'tool_call'}], usage_metadata={'input_tokens': 77, 'output_tokens': 14, 'total_tokens': 91, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}}
    ---
    {'tools': {'messages': [ToolMessage(content='Weather in London: Cloudy, 59°F', name='get_weather', id='e6bbae2e-1949-482f-8131-e4953b790b8e', tool_call_id='call_NjCJnualclh47mZCxjaZhnsH')]}}
    ---


    2025-11-05 12:34:20 - INFO - HTTP Request: POST https://api.openai.com/v1/chat/completions "HTTP/1.1 200 OK"


    {'model': {'messages': [AIMessage(content='The current weather in London is cloudy with a temperature of 59°F.', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 16, 'prompt_tokens': 109, 'total_tokens': 125, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-4o-2024-08-06', 'system_fingerprint': 'fp_cbf1785567', 'id': 'chatcmpl-CYPW2r7CPf484pNobEbb2NvI0hqLH', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None}, id='lc_run--46ab745f-1457-412e-bc37-f34a697a6672-0', usage_metadata={'input_tokens': 109, 'output_tokens': 16, 'total_tokens': 125, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}}
    ---


## Comparing Stream Modes

Let's understand when to use each stream mode:

| Stream Mode | Verbosity | Use Case | Output |
|-------------|-----------|----------|--------|
| **`values`** | Low | See state at each step | Full state snapshot |
| **`updates`** | Medium | See what changed | Only changes/deltas |
| **`debug`** | Maximum | Deep troubleshooting | All internal operations |
| *None (invoke)* | Minimal | Production | Just final result |

**Recommendations:**
- **Development**: Start with `values` to understand the flow
- **Debugging**: Use `debug` when something goes wrong
- **Production monitoring**: Use `updates` with logging
- **End users**: Use `invoke()` (no streaming) for clean output
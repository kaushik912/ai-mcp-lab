# Swarm Multi-Agent Architecture with OpenAI Agents SDK

## Introduction

Welcome to this tutorial on **Swarm Multi-Agent Architecture** using the **OpenAI Agents SDK**. This SDK is the production-ready evolution of OpenAI's experimental Swarm library, designed for building lightweight, scalable, and Pythonic multi-agent systems.

### What You'll Learn

- What the OpenAI Agents SDK is and how it differs from other frameworks
- How to create specialized agents using the `Agent()` constructor
- How to define tools with the `@function_tool` decorator
- How to implement agent handoffs for agent-to-agent coordination
- How to use Sessions for automatic conversation history management
- How to run multi-agent systems with `await Runner.run()` (Jupyter) or `Runner.run_sync()` (scripts)
- How to build a practical customer service multi-agent system

### Prerequisites

- Basic Python knowledge
- Understanding of LLMs and AI agents
- OpenAI API key

### Important Note for Jupyter Notebooks

This notebook uses `await Runner.run()` throughout because Jupyter notebooks already have an event loop running. If you're adapting this code for regular Python scripts, use `Runner.run_sync()` instead.

### What is OpenAI Agents SDK?

The **OpenAI Agents SDK** is a lightweight, production-ready framework for orchestrating multiple AI agents. Key characteristics:

- **Python-first**: Feels natural to Python developers, no complex graph definitions
- **Lightweight**: Minimal abstraction, easy to understand and debug
- **Production-ready**: Built for real-world applications with proper error handling
- **Flexible orchestration**: Supports handoffs for agent-to-agent delegation
- **Automatic history**: Sessions manage conversation state transparently

### Swarm Architecture Overview

In a **swarm architecture**, agents operate in a **decentralized** manner:

- **No central supervisor**: Agents communicate directly with each other
- **Peer-to-peer collaboration**: Agents decide when to transfer control to another agent
- **Specialization**: Each agent has expertise in a specific domain
- **Dynamic routing**: Tasks flow naturally between agents based on their capabilities

```
Swarm Architecture:

                    User Query
                         |
                         v
                  ┌─────────────┐
            ┌────►│  Agent A    │◄────┐
            │     └─────────────┘     │
            │            │            │
         handoff      handoff      handoff
            │            │            │
     ┌──────┴────┐       │      ┌────┴──────┐
     │  Agent B  │◄──────┼─────►│  Agent C  │
     └───────────┘       │      └───────────┘
                         v
                    Collaborative Result
```

## Step 1: Installation and Setup

First, let's install the OpenAI Agents SDK and set up our environment.


```python
# Install the OpenAI Agents SDK
# Uncomment the following line if you need to install the package
# !pip install openai-agents python-dotenv
```


```python
# Import necessary libraries
import os
from dotenv import load_dotenv
from agents import Agent, Runner, function_tool, SQLiteSession

# Load environment variables (including OPENAI_API_KEY)
load_dotenv()

# Verify API key is loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment variables")

print("Environment setup complete!")
print("OpenAI Agents SDK imported successfully!")
```

    Environment setup complete!
    OpenAI Agents SDK imported successfully!


## Step 2: Understanding the Core Components

Before we build our swarm, let's understand the key components of the OpenAI Agents SDK:

### 1. **Agent**
The core building block. Created with `Agent()`, each agent has:
- `name`: A unique identifier for the agent
- `instructions`: A system prompt defining its role and behavior
- `model`: The LLM to use (defaults to "gpt-4o")
- `tools`: Python functions the agent can call
- `handoffs`: List of other agents this agent can transfer control to

```python
agent = Agent(
    name="Assistant",
    instructions="You are a helpful assistant",
    model="gpt-4o-mini",  # optional
    tools=[my_tool],       # optional
    handoffs=[other_agent] # optional
)
```

### 2. **Function Tools**
Python functions decorated with `@function_tool` become callable tools for agents:
- The function docstring becomes the tool description
- Type hints define expected parameters
- Return values are passed back to the agent

```python
@function_tool
def get_weather(city: str) -> str:
    """Get the current weather for a city."""
    return f"Weather in {city}: Sunny"
```

### 3. **Handoffs**
Agents can transfer control to other agents by:
- Including target agents in their `handoffs` parameter
- The agent loop automatically decides when to transfer control
- Context and conversation history are preserved

### 4. **Runner**
Executes agents and manages the agent loop:
- `Runner.run_sync(agent, messages)`: Synchronous execution (for scripts)
- `Runner.run(agent, messages)`: Async execution (for Jupyter/async contexts)
- Returns a `RunResult` object with `final_output` and metadata

### 5. **Session**
Manages conversation history automatically:
- `SQLiteSession(session_id)`: In-memory storage
- `SQLiteSession(session_id, db_path)`: Persistent file-based storage
- Automatically tracks messages across runs
- Enables multi-turn conversations without manual state management

## Step 3: Create Simple Tools for Our Agents

Let's create some tools that our agents will use. We'll build a customer service swarm with agents that handle different types of inquiries.

The `@function_tool` decorator exposes Python functions as tools that agents can call. The function's docstring becomes the tool description that helps the agent understand when to use it.


```python
# Define tools for our agents using the @function_tool decorator

@function_tool
def get_order_status(order_id: str) -> str:
    """Look up the status of an order by order ID."""
    # Simulated order lookup
    orders = {
        "ORD001": "Shipped - Expected delivery: Nov 15",
        "ORD002": "Processing - Will ship tomorrow",
        "ORD003": "Delivered on Nov 10"
    }
    return orders.get(order_id, f"Order {order_id} not found")

@function_tool
def calculate_refund(amount: float, reason: str) -> str:
    """Calculate refund amount based on purchase amount and return reason."""
    # Simplified refund logic
    if "defective" in reason.lower():
        refund = amount  # Full refund
        return f"Full refund approved: ${refund:.2f}"
    elif "changed mind" in reason.lower():
        refund = amount * 0.85  # 15% restocking fee
        return f"Refund with restocking fee: ${refund:.2f} (85% of ${amount:.2f})"
    else:
        return f"Refund of ${amount:.2f} pending review by supervisor"

@function_tool
def check_product_availability(product_name: str) -> str:
    """Check if a product is currently in stock."""
    # Simulated inventory check
    inventory = {
        "laptop": "In stock - 45 units available",
        "phone": "Low stock - 3 units remaining",
        "tablet": "Out of stock - Expected restock: Nov 20"
    }
    return inventory.get(product_name.lower(), f"Product '{product_name}' not found in catalog")

print("Tools created successfully!")
print(f"- get_order_status: {get_order_status.__doc__}")
print(f"- calculate_refund: {calculate_refund.__doc__}")
print(f"- check_product_availability: {check_product_availability.__doc__}")
```

    Tools created successfully!
    - get_order_status: A tool that wraps a function. In most cases, you should use  the `function_tool` helpers to
        create a FunctionTool, as they let you easily wrap a Python function.
        
    - calculate_refund: A tool that wraps a function. In most cases, you should use  the `function_tool` helpers to
        create a FunctionTool, as they let you easily wrap a Python function.
        
    - check_product_availability: A tool that wraps a function. In most cases, you should use  the `function_tool` helpers to
        create a FunctionTool, as they let you easily wrap a Python function.
        


## Step 4: Create Specialized Agents

Now we'll create three specialized agents for our customer service swarm:

1. **Triage Agent**: First point of contact, routes to appropriate specialist
2. **Order Agent**: Handles order tracking and status inquiries
3. **Refund Agent**: Processes returns and refunds

### Important: Creating Agents with Handoffs

In the OpenAI Agents SDK, handoffs work differently than in other frameworks:
- You define which agents an agent can hand off to via the `handoffs` parameter
- The agent's instructions should mention when to transfer control
- The agent loop automatically handles the handoff mechanism
- No explicit "handoff tool" creation is needed


```python
# Note: We need to create agents in the right order since they reference each other
# We'll use a forward declaration pattern

# First, create the Order Agent
order_agent = Agent(
    name="OrderAgent",
    instructions="""
You are the Order Agent, specializing in order tracking and delivery information.

Your responsibilities:
- Look up order status using the order ID
- Provide tracking and delivery information
- Answer questions about shipping

If the customer has issues with their order (damaged, defective, want to return):
- Transfer to the RefundAgent

If the customer has unrelated questions:
- Transfer back to the TriageAgent

Be clear, concise, and helpful.
""",
    model="gpt-4o-mini",
    tools=[get_order_status]
)

print("Order Agent created!")
```

    Order Agent created!



```python
# Create the Refund Agent
refund_agent = Agent(
    name="RefundAgent",
    instructions="""
You are the Refund Agent, specializing in returns and refunds.

Your responsibilities:
- Process return requests
- Calculate refund amounts based on the reason
- Explain refund policies clearly

Refund policy:
- Defective products: Full refund
- Changed mind: 85% refund (15% restocking fee)
- Other reasons: Case-by-case review

If the customer needs to check their order first:
- Transfer to the OrderAgent

If the customer has unrelated questions:
- Transfer back to the TriageAgent

Be empathetic and solution-oriented.
""",
    model="gpt-4o-mini",
    tools=[calculate_refund]
)

print("Refund Agent created!")
```

    Refund Agent created!



```python
# Now add handoff capabilities to the agents
# In OpenAI Agents SDK, we update the handoffs after creating all agents

# Order Agent can hand off to Refund Agent
order_agent.handoffs = [refund_agent]

# Refund Agent can hand off to Order Agent
refund_agent.handoffs = [order_agent]

print("Handoffs configured!")
print(f"OrderAgent can hand off to: {[agent.name for agent in order_agent.handoffs]}")
print(f"RefundAgent can hand off to: {[agent.name for agent in refund_agent.handoffs]}")
```

    Handoffs configured!
    OrderAgent can hand off to: ['RefundAgent']
    RefundAgent can hand off to: ['OrderAgent']



```python
# Finally, create the Triage Agent (entry point) with handoffs to both specialists
triage_agent = Agent(
    name="TriageAgent",
    instructions="""
You are the Triage Agent, the first point of contact for customer inquiries.

Your role:
- Greet customers warmly
- Understand what they need help with
- Route them to the appropriate specialist:
  * OrderAgent: for order status, tracking, delivery questions
  * RefundAgent: for returns, refunds, or product issues
- You can also help with product availability questions directly

Always be helpful and professional. If you're not sure which agent to transfer to, ask clarifying questions.
""",
    model="gpt-4o-mini",
    tools=[check_product_availability],
    handoffs=[order_agent, refund_agent]
)

# Also allow specialists to hand back to triage
order_agent.handoffs.append(triage_agent)
refund_agent.handoffs.append(triage_agent)

print("Triage Agent created!")
print(f"TriageAgent can hand off to: {[agent.name for agent in triage_agent.handoffs]}")
print("\nSwarm architecture complete!")
```

    Triage Agent created!
    TriageAgent can hand off to: ['OrderAgent', 'RefundAgent']
    
    Swarm architecture complete!


## Step 5: Run the Swarm - Single Interaction

Let's test our swarm with a simple query. Since we're in a Jupyter notebook (which already has an event loop), we'll use `await Runner.run()` instead of `Runner.run_sync()`.

The `Runner.run()` method:
- Takes an agent as the starting point
- Accepts a message (string) or list of messages
- Returns a `RunResult` object with `final_output` and other metadata
- Automatically handles the agent loop and any handoffs

**Note**: Jupyter notebooks support top-level `await`, so we can use the async API directly!


```python
# Run a simple query through the swarm
# Using await since we're in a Jupyter notebook
result = await Runner.run(
    triage_agent,  # Start with the triage agent
    "Hi! I need to check on my order ORD001"
)

# Display the response
print("=== Swarm Response ===")
print(f"\nFinal Output: {result.final_output}")

# Try to detect which agent responded
agent_name = "Unknown Agent"
if hasattr(result, 'new_items') and result.new_items:
    for item in reversed(result.new_items):
        if hasattr(item, 'role') and item.role == 'assistant':
            if hasattr(item, 'agent_name'):
                agent_name = item.agent_name
                break
            elif hasattr(item, 'name'):
                agent_name = item.name
                break

print(f"\nAgent that responded: {agent_name}")
print(f"\nNote: The swarm automatically handled the handoff from TriageAgent to OrderAgent!")
```

    === Swarm Response ===
    
    Final Output: Your order **ORD001** has been shipped and is expected to be delivered on **November 15**. If you have any other questions, feel free to ask!
    
    Agent that responded: Unknown Agent
    
    Note: The swarm automatically handled the handoff from TriageAgent to OrderAgent!


## Step 6: Create a Helper Function for Swarm Invocation

Let's create a reusable helper function that makes it easy to invoke the swarm and track important information:

- **Logs handoffs**: Shows which agents were involved in handling the request
- **Tracks the final agent**: Identifies which agent provided the final response
- **Displays usage statistics**: Shows token consumption and API calls
- **Pretty output**: Formats the response in a clear, readable way

This function will be useful for testing different queries and understanding how the swarm routes requests.


```python
async def invoke_swarm(starting_agent: Agent, user_message: str, verbose: bool = True):
    """
    Invoke the swarm with comprehensive logging and tracking.
    
    Args:
        starting_agent: The agent to start with (usually the triage agent)
        user_message: The user's query or request
        verbose: If True, prints detailed information about handoffs and usage
    
    Returns:
        The RunResult object from the agent execution
    """
    # Run the swarm
    result = await Runner.run(starting_agent, user_message)
    
    if verbose:
        print("=" * 70)
        print("SWARM EXECUTION REPORT")
        print("=" * 70)
        
        # Track agents involved
        agents_involved = [starting_agent.name]
        
        # Examine items to find handoffs and agent switches
        if hasattr(result, 'new_items') and result.new_items:
            for item in result.new_items:
                # Check for agent handoffs in the items
                if hasattr(item, 'type'):
                    if item.type == 'agent_switch_item' or 'handoff' in str(item.type).lower():
                        if hasattr(item, 'agent'):
                            agent_name = item.agent.name if hasattr(item.agent, 'name') else str(item.agent)
                            if agent_name not in agents_involved:
                                agents_involved.append(agent_name)
                                print(f"\n  [HANDOFF] Transferred to: {agent_name}")
                
                # Alternative: check if item has an agent attribute
                if hasattr(item, 'agent') and hasattr(item.agent, 'name'):
                    agent_name = item.agent.name
                    if agent_name not in agents_involved:
                        agents_involved.append(agent_name)
                        print(f"\n  [HANDOFF] Transferred to: {agent_name}")
        
        # Display the agent path
        print(f"\nAgent Path: {' -> '.join(agents_involved)}")
        # Display the final output
        print(f"\n--- Final Response ---")
        print(f"{result.final_output}")
        print("=" * 70)
    
    return result

print("Helper function 'invoke_swarm' created successfully!")
```

    Helper function 'invoke_swarm' created successfully!


## Summary: Understanding the Helper Function

The `invoke_swarm` helper function provides several key benefits:

### 1. **Handoff Tracking**
The function examines `result.new_items` to detect when agents transfer control to each other. This helps you understand the flow of execution through your swarm.

### 2. **Agent Identification**
It uses `result.current_agent.name` to identify which agent provided the final response, making debugging and monitoring easier.

### 3. **Usage Metrics**
By accessing `result.context_wrapper.usage`, it displays:
- Number of API requests made
- Input tokens consumed
- Output tokens generated
- Total token usage

### 4. **Flexible Configuration**
The `verbose` parameter lets you toggle detailed logging on/off:
```python
# Detailed output
result = await invoke_swarm(triage_agent, "Hello")

# Silent execution (returns result only)
result = await invoke_swarm(triage_agent, "Hello", verbose=False)
```

### Key Insights from the OpenAI Agents SDK

Based on the documentation:
- **`result.current_agent`**: Contains the agent that produced the final output
- **`result.new_items`**: List of all items generated during the run (messages, tool calls, handoffs)
- **`result.context_wrapper.usage`**: Contains token usage statistics
- **`result.final_output`**: The final response text from the agent

This helper function makes it easy to monitor and debug your swarm architecture!


```python
# Test 1: Product availability - Triage Agent should handle this directly (no handoff)
print("\n\nTEST 1: Product Availability (No Handoff Expected)")
print("-" * 70)
result3 = await invoke_swarm(
    triage_agent,
    "Is the tablet in stock? I want to buy one."
)
print("\n")
```

    
    
    TEST 3: Product Availability (No Handoff Expected)
    ----------------------------------------------------------------------
    ======================================================================
    SWARM EXECUTION REPORT
    ======================================================================
    
    Agent Path: TriageAgent
    
    --- Final Response ---
    Could you please specify the name of the tablet you're interested in? That way, I can check its availability for you.
    ======================================================================
    
    



```python
# Test 2: Refund request - should trigger handoff from Triage to Refund Agent
print("\n\nTEST 2: Refund Request")
print("-" * 70)
result2 = await invoke_swarm(
    triage_agent,
    "I want to return my laptop because it's defective. It cost $1200. What's my refund?"
)
print("\n")
```

    
    
    TEST 2: Refund Request
    ----------------------------------------------------------------------
    ======================================================================
    SWARM EXECUTION REPORT
    ======================================================================
    
      [HANDOFF] Transferred to: RefundAgent
    
    Agent Path: TriageAgent -> RefundAgent
    
    --- Final Response ---
    Since the laptop is defective, you are eligible for a full refund. Given that your purchase amount was $1200, your refund will be the full amount of **$1200**. 
    
    I recommend starting the return process as soon as possible. If you need further assistance, feel free to ask!
    ======================================================================
    
    



```python
# Test 3: Order status inquiry - should trigger handoff from Triage to Order Agent
print("TEST 3: Order Status Query")
print("-" * 70)
result1 = await invoke_swarm(
    triage_agent,
    "Hi! I need to check on my order ORD002. When will it ship?"
)
print("\n")
```

    TEST 1: Order Status Query
    ----------------------------------------------------------------------
    ======================================================================
    SWARM EXECUTION REPORT
    ======================================================================
    
      [HANDOFF] Transferred to: OrderAgent
    
    Agent Path: TriageAgent -> OrderAgent
    
    --- Final Response ---
    Your order (ORD002) is currently processing and is expected to ship tomorrow. If you have any more questions, feel free to ask!
    ======================================================================
    
    

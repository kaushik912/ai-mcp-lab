# Debugging Agentic Workflows in LangGraph

## Tutorial Overview

Debugging agentic workflows presents unique challenges compared to traditional software. With LLM-based agents:
- Behavior emerges from state transformations, not deterministic code paths
- You need to observe what's happening at each step in real-time
- Traditional debugging techniques (breakpoints, print statements) aren't sufficient
- You must validate LLM decisions, routing logic, and tool invocations

In this tutorial, you'll learn professional debugging techniques for LangGraph workflows:

1. **Streaming for observation**: Watch state evolve in real-time as your agent executes
2. **Interrupts for inspection**: Pause execution at critical points to validate behavior
3. **Practical debugging scenarios**: Apply these techniques to real problems

## Learning Objectives

By the end of this tutorial, you will be able to:

1. Use LangGraph's streaming modes (`values`, `updates`, `debug`) to observe execution
2. Trace execution through state history to understand agent behavior
3. Use interrupts to pause and inspect state at critical decision points
4. Debug failed agent runs by identifying where things went wrong
5. Monitor and log agent behavior for analysis

## Prerequisites

- Completion of `3_2_custom_agentic_workflows_tutorial.ipynb`
- Understanding of the Smart Query Router workflow
- API keys for:
  - OpenAI (or another LLM provider)
  - Tavily (for web search)

## What We'll Debug

We'll use the **Smart Query Router** from the previous tutorial as our debugging target. This workflow has several decision points that benefit from debugging:
- Intent detection (is it working correctly?)
- Routing logic (are queries routed to the right node?)
- Tool invocations (is web search returning relevant results?)
- Response generation (is the final answer appropriate?)

## Part 1: Setup - Recreate the Smart Query Router

We'll reuse the Smart Query Router from the previous tutorial. This gives us a working workflow to debug.

The workflow:
1. Detects intent (search vs. direct response)
2. Routes based on intent
3. Either searches the web or generates a direct response
4. Returns the final result


```python
# Load environment variables
from dotenv import load_dotenv
import os

load_dotenv()

assert os.getenv("OPENAI_API_KEY"), "OPENAI_API_KEY not found in environment"
assert os.getenv("TAVILY_API_KEY"), "TAVILY_API_KEY not found in environment"

print("Environment variables loaded successfully!")
```

    Environment variables loaded successfully!



```python
# Import dependencies
from typing import TypedDict, Literal
from langgraph.graph import StateGraph, START, END
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_tavily import TavilySearch
from pydantic import BaseModel, Field
from langgraph.checkpoint.memory import MemorySaver
from langgraph.types import Command, interrupt
import json

print("All imports successful!")
```

    All imports successful!



```python
# Define state schema
class AgentState(TypedDict):
    """State schema for our agentic workflow."""
    query: str
    intent: str
    search_results: str
    response: str

print("State schema defined!")
```

    State schema defined!



```python
# Initialize LLM and tools
class IntentClassification(BaseModel):
    """Schema for intent classification results."""
    intent: Literal["search", "direct"] = Field(
        description="The detected intent: 'search' for queries requiring current/real-time information, 'direct' for general knowledge questions"
    )
    reasoning: str = Field(
        description="Brief explanation of why this intent was chosen"
    )

llm = ChatOpenAI(model="gpt-4o", temperature=0)
intent_classifier = llm.with_structured_output(IntentClassification)
tavily_search = TavilySearch(max_results=3, topic="general")

print("LLM and tools initialized!")
```

    LLM and tools initialized!



```python
# Node 1: Intent Detection
def intent_detection_node(state: AgentState) -> AgentState:
    """Analyzes the user query and determines the appropriate processing path."""
    query = state["query"]
    
    system_prompt = """You are an intent classifier for a query routing system.

Analyze the user's query and determine if it requires:
- SEARCH: Current/real-time information (news, weather, stock prices, recent events, current facts)
- DIRECT: General knowledge, definitions, explanations, coding help, historical facts

Provide your classification with reasoning.

Examples:
- "What's the weather in Paris today?" -> search (requires current data)
- "Explain how neural networks work" -> direct (general knowledge)
- "Latest news about AI" -> search (requires current information)
- "How do I write a for loop in Python?" -> direct (coding help from training)
"""
    
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"Query: {query}"}
    ]
    
    result = intent_classifier.invoke(messages)
    
    print(f"Intent detected: {result.intent}")
    print(f"Reasoning: {result.reasoning}")
    
    # Return ONLY the field we're updating (not the entire state)
    # This makes "updates" stream mode show only what changed
    return {"intent": result.intent}

print("Intent detection node created!")
```

    Intent detection node created!



```python
# Node 2: Web Search
def web_search_node(state: AgentState) -> AgentState:
    """Performs web search using Tavily and generates an augmented response."""
    query = state["query"]
    
    print(f"Performing web search for: {query}")
    
    search_response = tavily_search.invoke({"query": query})
    search_results = search_response.get("results", [])
    
    print(f"Found {len(search_results)} search results")
    
    formatted_results = "\n\n".join([
        f"Title: {r.get('title', 'N/A')}\nURL: {r.get('url', 'N/A')}\nContent: {r.get('content', '')}"
        for r in search_results
    ])
    
    system_prompt = """You are a helpful assistant that answers questions using web search results.
    
Use the provided search results to give an accurate, informative answer.
Always cite your sources by mentioning the titles and URLs.
If the search results don't contain relevant information, say so.
"""
    
    user_prompt = f"""Question: {query}

Search Results:
{formatted_results}

Please provide a clear, concise answer based on these search results."""
    
    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_prompt)
    ]
    
    response = llm.invoke(messages)
    
    # Return ONLY the fields we're updating
    return {
        "search_results": formatted_results,
        "response": response.content
    }

print("Web search node created!")
```

    Web search node created!



```python
# Node 3: Direct Response
def direct_response_node(state: AgentState) -> AgentState:
    """Generates a direct response using the LLM's knowledge."""
    query = state["query"]
    
    print(f"Generating direct response for: {query}")
    
    system_prompt = """You are a helpful assistant that provides clear, accurate answers.
    
Answer questions using your knowledge and training.
Be concise but thorough.
If you're not certain about something, acknowledge the uncertainty.
"""
    
    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=query)
    ]
    
    response = llm.invoke(messages)
    
    # Return ONLY the field we're updating
    return {"response": response.content}

print("Direct response node created!")
```

    Direct response node created!



```python
# Router function
def route_by_intent(state: AgentState) -> Literal["web_search", "direct_response"]:
    """Routes to the appropriate processing node based on detected intent."""
    intent = state["intent"]
    
    if intent == "search":
        print("Routing to: web_search node")
        return "web_search"
    else:
        print("Routing to: direct_response node")
        return "direct_response"

print("Router function created!")
```

    Router function created!



```python
# Build and compile the graph
workflow = StateGraph(AgentState)

workflow.add_node("intent_detection", intent_detection_node)
workflow.add_node("web_search", web_search_node)
workflow.add_node("direct_response", direct_response_node)

workflow.add_edge(START, "intent_detection")
workflow.add_conditional_edges(
    "intent_detection",
    route_by_intent,
    {
        "web_search": "web_search",
        "direct_response": "direct_response"
    }
)
workflow.add_edge("web_search", END)
workflow.add_edge("direct_response", END)

app = workflow.compile()

print("Graph compiled successfully!")
print("\nReady for debugging!")
```

    Graph compiled successfully!
    
    Ready for debugging!


## Part 2: Debugging with Streaming - State Inspection

### Why Streaming for Debugging?

Traditional debugging (breakpoints, print statements) works well for deterministic code, but agentic workflows are different:

- **Emergent behavior**: Agent behavior emerges from state transformations and LLM decisions
- **Non-deterministic**: LLM outputs can vary, making reproduction challenging
- **Multiple decision points**: Intent detection, routing, tool calls - each can fail silently
- **State evolution**: You need to see how state changes through the workflow

**Streaming** gives you real-time visibility into what's happening at each step without modifying your code.

### LangGraph Streaming Modes

LangGraph provides three streaming modes, each offering different insights:

1. **`values`**: Complete state snapshot after each node
2. **`updates`**: Only what changed in each node (deltas)
3. **`debug`**: Execution metadata and framework internals

Let's explore each mode to understand when to use them.

### Stream Mode: `values`

The `values` mode returns the complete state after each node executes. This is useful for:
- Understanding overall state evolution
- Seeing accumulated data across nodes
- Verifying that state is building up correctly

Let's stream a query that requires web search and observe the full state at each step.


```python
# Test query that should trigger web search
test_query = "What are the current stock prices for Apple?"

print(f"Query: {test_query}")
print("=" * 80)
print("\nSTREAMING MODE: values (complete state snapshots)")
print("=" * 80)

initial_state = {
    "query": test_query,
    "intent": "",
    "search_results": "",
    "response": ""
}

# Stream with 'values' mode
for i, chunk in enumerate(app.stream(initial_state, stream_mode="values")):
    print(f"\n--- Chunk {i+1} ---")
    print(f"Query: {chunk.get('query', 'N/A')}")
    print(f"Intent: {chunk.get('intent', 'N/A')}")
    print(f"Search Results: {chunk.get('search_results', 'N/A')[:100]}...") if chunk.get('search_results') else print(f"Search Results: N/A")
    print(f"Response: {chunk.get('response', 'N/A')[:150]}...") if chunk.get('response') else print(f"Response: N/A")
    print()
```

    Query: What are the current stock prices for Apple?
    ================================================================================
    
    STREAMING MODE: values (complete state snapshots)
    ================================================================================
    
    --- Chunk 1 ---
    Query: What are the current stock prices for Apple?
    Intent: 
    Search Results: N/A
    Response: N/A
    
    Intent detected: search
    Reasoning: The query asks for the current stock prices of Apple, which requires real-time financial data that can change frequently throughout the trading day.
    Routing to: web_search node
    
    --- Chunk 2 ---
    Query: What are the current stock prices for Apple?
    Intent: search
    Search Results: N/A
    Response: N/A
    
    Performing web search for: What are the current stock prices for Apple?
    Found 3 search results
    
    --- Chunk 3 ---
    Query: What are the current stock prices for Apple?
    Intent: search
    Search Results: Title: Buy or Sell Apple Stock - AAPL Stock Price Quote & News | Robinhood
    URL: https://robinhood.co...
    Response: The current stock price for Apple Inc. (AAPL) is approximately $269.43 according to Seeking Alpha [source](https://seekingalpha.com/symbol/AAPL). Mark...
    


### Observations from `values` Mode

Notice how:
1. **Initial state** appears in the first chunk
2. **After intent detection**: `intent` field is populated
3. **After web search**: Both `search_results` and `response` are populated
4. **Each chunk shows the complete state**, not just what changed

This mode is great for:
- Getting the full picture at each step
- Debugging state accumulation issues
- Understanding the complete flow from start to finish

**Limitation**: Can be verbose for complex states with many fields.

### Stream Mode: `updates`

The `updates` mode returns only what changed in each node (the delta). This is useful for:
- Focusing on individual node outputs
- Reducing noise in complex workflows
- Identifying which nodes are modifying which fields

Let's run the same query with `updates` mode and compare.


```python
print(f"Query: {test_query}")
print("=" * 80)
print("\nSTREAMING MODE: updates (only changes from each node)")
print("=" * 80)

# Stream with 'updates' mode
for i, chunk in enumerate(app.stream(initial_state, stream_mode="updates")):
    print(f"\n--- Update {i+1} ---")
    print(f"Node: {list(chunk.keys())[0] if chunk else 'N/A'}")
    print(f"Updates: {json.dumps(chunk, indent=2, default=str)[:500]}...")
    print()
```

    Query: What are the current stock prices for Apple?
    ================================================================================
    
    STREAMING MODE: updates (only changes from each node)
    ================================================================================
    Intent detected: search
    Reasoning: The query asks for the current stock prices of Apple, which requires real-time financial data.
    Routing to: web_search node
    
    --- Update 1 ---
    Node: intent_detection
    Updates: {
      "intent_detection": {
        "intent": "search"
      }
    }...
    
    Performing web search for: What are the current stock prices for Apple?
    Found 3 search results
    
    --- Update 2 ---
    Node: web_search
    Updates: {
      "web_search": {
        "search_results": "Title: Buy or Sell Apple Stock - AAPL Stock Price Quote & News | Robinhood\nURL: https://robinhood.com/us/en/stocks/AAPL/\nContent: Shares are currently priced at $268.69, which is +0.1% above the low and -2.6% below the high. Apple(AAPL) shares are trading with a volume of 46.21M, against a\n\nTitle: Apple Inc. (AAPL) Stock Price, Quote, News & Analysis\nURL: https://seekingalpha.com/symbol/AAPL\nContent: Apple Inc.'s stock symbol is AAPL and currently...
    


### Comparing `values` vs `updates`

**`values` mode:**
- Shows complete state after each node
- Includes all fields, even unchanged ones
- Best for understanding overall state evolution

**`updates` mode:**
- Shows only what changed in each node
- Highlights node-specific transformations
- Best for identifying which node does what

**Important**: The `updates` mode shows what the node **returns**. For this to be truly useful, nodes should return only the fields they're modifying:

```python
# ✅ Good: Returns only what changed
def intent_detection_node(state):
    # ... classification logic ...
    return {"intent": result.intent}  # Only the modified field

# ❌ Less useful for "updates" mode: Returns entire state
def intent_detection_node(state):
    # ... classification logic ...
    return {**state, "intent": result.intent}  # All fields
```

Our nodes follow the first pattern, which is why `updates` mode shows clean, focused output.

Use `updates` when:
- You want to isolate node behavior
- Your state has many fields and you want to reduce noise
- You're debugging a specific node's output

### Stream Mode: `debug`

The `debug` mode returns execution metadata and framework internals. This includes:
- Which nodes executed
- Routing decisions
- Timing information
- Framework-level events

This is useful for:
- Understanding the execution path
- Debugging routing logic
- Performance analysis
- Identifying framework-level issues


```python
print(f"Query: {test_query}")
print("=" * 80)
print("\nSTREAMING MODE: debug (execution metadata)")
print("=" * 80)

# Stream with 'debug' mode
for i, chunk in enumerate(app.stream(initial_state, stream_mode="debug")):
    print(f"\n--- Debug Event {i+1} ---")
    print(f"Type: {chunk.get('type', 'N/A')}")
    print(f"Timestamp: {chunk.get('timestamp', 'N/A')}")
    
    # Print relevant details based on event type
    if 'payload' in chunk:
        payload = chunk['payload']
        if 'name' in payload:
            print(f"Node: {payload['name']}")
        if 'input' in payload:
            print(f"Input keys: {list(payload['input'].keys()) if isinstance(payload['input'], dict) else 'N/A'}")
        if 'output' in payload:
            print(f"Output keys: {list(payload['output'].keys()) if isinstance(payload['output'], dict) else 'N/A'}")
    print()
```

    Query: What are the current stock prices for Apple?
    ================================================================================
    
    STREAMING MODE: debug (execution metadata)
    ================================================================================
    
    --- Debug Event 1 ---
    Type: task
    Timestamp: 2025-11-12T05:20:49.631886+00:00
    Node: intent_detection
    Input keys: ['query', 'intent', 'search_results', 'response']
    
    Intent detected: search
    Reasoning: The query asks for the current stock prices of Apple, which requires real-time financial data.
    Routing to: web_search node
    
    --- Debug Event 2 ---
    Type: task_result
    Timestamp: 2025-11-12T05:20:51.080943+00:00
    Node: intent_detection
    
    
    --- Debug Event 3 ---
    Type: task
    Timestamp: 2025-11-12T05:20:51.081346+00:00
    Node: web_search
    Input keys: ['query', 'intent', 'search_results', 'response']
    
    Performing web search for: What are the current stock prices for Apple?
    Found 3 search results
    
    --- Debug Event 4 ---
    Type: task_result
    Timestamp: 2025-11-12T05:20:54.287124+00:00
    Node: web_search
    


## Part 3: Debugging with Interrupts - Interactive Inspection

### Why Interrupts for Debugging?

Streaming lets you observe, but sometimes you need to:
- **Pause at critical decision points**
- **Inspect state before it changes**
- **Validate assumptions about agent behavior**
- **Interactively explore different paths**

**Interrupts** give you "debug mode" - the ability to pause execution, inspect state, and then resume.


### Key Concept: Checkpointing

Interrupts require **checkpointing** to save state. We'll use `MemorySaver` for this tutorial:
- Keeps state in memory (good for development)
- Use `SqliteSaver` or `PostgresSaver` for production
- Each execution has a `thread_id` to track state

### Adding an Interrupt for Debugging

Let's modify the `intent_detection_node` to add an interrupt AFTER intent classification. This lets us:
- See what intent was detected
- Validate the reasoning
- Decide whether to continue or investigate further

We'll create a new version of the node with an interrupt.


```python
# Modified intent detection node with interrupt
def intent_detection_node_with_interrupt(state: AgentState) -> AgentState:
    """Analyzes the user query and determines the appropriate processing path.
    
    Includes an interrupt for debugging: pauses after intent detection to allow inspection.
    """
    query = state["query"]
    
    system_prompt = """You are an intent classifier for a query routing system.

Analyze the user's query and determine if it requires:
- SEARCH: Current/real-time information (news, weather, stock prices, recent events, current facts)
- DIRECT: General knowledge, definitions, explanations, coding help, historical facts

Provide your classification with reasoning.

Examples:
- "What's the weather in Paris today?" -> search (requires current data)
- "Explain how neural networks work" -> direct (general knowledge)
- "Latest news about AI" -> search (requires current information)
- "How do I write a for loop in Python?" -> direct (coding help from training)
"""
    
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"Query: {query}"}
    ]
    
    result = intent_classifier.invoke(messages)
    
    print(f"Intent detected: {result.intent}")
    print(f"Reasoning: {result.reasoning}")
    
    # INTERRUPT: Pause here for inspection
    # This allows us to validate the intent detection before routing
    interrupt({
        "message": "Intent detection complete. Review and resume to continue.",
        "detected_intent": result.intent,
        "reasoning": result.reasoning,
        "query": query
    })
    
    # Return ONLY the field we're updating
    return {"intent": result.intent}

print("Intent detection node with interrupt created!")
```

    Intent detection node with interrupt created!


### Recompile Graph with Checkpointer

Now we need to:
1. Create a new graph with the modified node
2. Add a checkpointer (MemorySaver)
3. Compile the graph

The checkpointer will save state at each step, allowing us to pause and resume.


```python
# Create a new workflow with the interrupt-enabled node
workflow_with_interrupt = StateGraph(AgentState)

# Add nodes (using the modified intent detection node)
workflow_with_interrupt.add_node("intent_detection", intent_detection_node_with_interrupt)
workflow_with_interrupt.add_node("web_search", web_search_node)
workflow_with_interrupt.add_node("direct_response", direct_response_node)

# Add edges (same as before)
workflow_with_interrupt.add_edge(START, "intent_detection")
workflow_with_interrupt.add_conditional_edges(
    "intent_detection",
    route_by_intent,
    {
        "web_search": "web_search",
        "direct_response": "direct_response"
    }
)
workflow_with_interrupt.add_edge("web_search", END)
workflow_with_interrupt.add_edge("direct_response", END)

# Compile with checkpointer
# NOTE: Always run this cell after modifying node functions to recompile the graph
checkpointer = MemorySaver()
app_with_interrupt = workflow_with_interrupt.compile(checkpointer=checkpointer)

print("Graph with interrupts compiled successfully!")
print("Checkpointer: MemorySaver (in-memory state persistence)")
print("\nIMPORTANT: Re-run this cell if you modify any node functions above!")
```

    Graph with interrupts compiled successfully!
    Checkpointer: MemorySaver (in-memory state persistence)
    
    IMPORTANT: Re-run this cell if you modify any node functions above!


### Testing the Interrupt: Pause and Inspect

Let's test the interrupt by:
1. Starting execution with a test query
2. Observing when the interrupt triggers
3. Inspecting the state at the pause point
4. Examining the interrupt payload
5. Resuming execution to see the final result

**Important**: We need to provide a `thread_id` in the config to track this execution.


```python
# Test query
test_query_interrupt = "What's the weather like in Tokyo right now?"

print(f"Query: {test_query_interrupt}")
print("=" * 80)
print("\nPhase 1: Initial execution (will hit interrupt)")
print("=" * 80)

# Configuration with thread_id for state tracking
config = {"configurable": {"thread_id": "debug-session-1"}}

# Initial state
initial_state = {
    "query": test_query_interrupt,
    "intent": "",
    "search_results": "",
    "response": ""
}

# Invoke the graph - it will pause at the interrupt
result = app_with_interrupt.invoke(initial_state, config)

print("\n" + "=" * 80)
print("Execution paused at interrupt!")
print("=" * 80)
```

    Query: What's the weather like in Tokyo right now?
    ================================================================================
    
    Phase 1: Initial execution (will hit interrupt)
    ================================================================================
    Intent detected: search
    Reasoning: The query asks for the current weather in Tokyo, which requires real-time information.
    
    ================================================================================
    Execution paused at interrupt!
    ================================================================================


### Inspecting State at the Interrupt

Now that execution is paused, let's inspect:
1. The current state
2. The interrupt payload (what information was passed)
3. What we can learn before deciding to resume


```python
print("INSPECTING STATE AT INTERRUPT")
print("=" * 80)

# Check current state
print("\nCurrent State:")
print(f"  Query: {result['query']}")
print(f"  Intent: {result['intent']}")
print(f"  Search Results: {result['search_results'] or 'Not yet executed'}")
print(f"  Response: {result['response'] or 'Not yet executed'}")

# Check interrupt information
if '__interrupt__' in result:
    print("\nInterrupt Payload:")
    interrupts = result['__interrupt__']
    for interrupt_info in interrupts:
        interrupt_value = interrupt_info.value
        print(f"  Message: {interrupt_value.get('message')}")
        print(f"  Detected Intent: {interrupt_value.get('detected_intent')}")
        print(f"  Reasoning: {interrupt_value.get('reasoning')}")

print("\n" + "=" * 80)
print("Analysis: Intent detection shows 'search' - this is correct for a weather query.")
print("Decision: Resume execution to see the web search results.")
print("=" * 80)
```

    INSPECTING STATE AT INTERRUPT
    ================================================================================
    
    Current State:
      Query: What's the weather like in Tokyo right now?
      Intent: 
      Search Results: Not yet executed
      Response: Not yet executed
    
    Interrupt Payload:
      Message: Intent detection complete. Review and resume to continue.
      Detected Intent: search
      Reasoning: The query asks for the current weather in Tokyo, which requires real-time information.
    
    ================================================================================
    Analysis: Intent detection shows 'search' - this is correct for a weather query.
    Decision: Resume execution to see the web search results.
    ================================================================================


### Resuming Execution

After inspecting the state and validating the intent, we can resume execution using `Command(resume=True)`.

The graph will:
1. Continue from where it paused
2. Route based on the detected intent
3. Execute the appropriate node (web search in this case)
4. Return the final result


```python
print("Phase 2: Resuming execution")
print("=" * 80)

# Resume execution with the same config (same thread_id)
final_result = app_with_interrupt.invoke(Command(resume=True), config)

print("\n" + "=" * 80)
print("FINAL RESULT")
print("=" * 80)
print(f"\nIntent: {final_result['intent']}")
print(f"\nResponse Preview:")
print(final_result['response'][:300] + "..." if len(final_result['response']) > 300 else final_result['response'])
```

    Phase 2: Resuming execution
    ================================================================================
    Intent detected: search
    Reasoning: The query asks for the current weather in Tokyo, which requires real-time information that can change frequently.
    Routing to: web_search node
    Performing web search for: What's the weather like in Tokyo right now?
    Found 3 search results
    
    ================================================================================
    FINAL RESULT
    ================================================================================
    
    Intent: search
    
    Response Preview:
    The current weather in Tokyo is partly cloudy with a temperature of 15.1°C (59.2°F). The wind is blowing from the south-southeast at 8.3 kph (5.1 mph), and the humidity is at 39%. There is no precipitation, and the visibility is 10 km (6 miles) [source](https://www.weatherapi.com/).

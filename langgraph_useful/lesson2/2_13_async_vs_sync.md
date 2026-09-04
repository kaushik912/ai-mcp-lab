# Async vs Sync Agent Execution in LangGraph

## Introduction

Understanding the difference between synchronous and asynchronous execution is crucial for building high-performance AI agents. In this notebook, you'll learn when and how to use each approach.

**Key Concepts**:
- **Synchronous (Sync)**: Operations execute one at a time, blocking until each completes
- **Asynchronous (Async)**: Operations can execute concurrently, not blocking each other

**Why This Matters for AI Agents**:
- LLM API calls are I/O-bound operations (waiting for network responses)
- Async execution can dramatically improve performance when making multiple API calls
- Production chatbots and web services benefit greatly from async patterns

**Prerequisites**: 
- An OpenAI API key stored in a `.env` file
- Basic understanding of LangGraph agents

**What You'll Learn**:
- How to execute agents synchronously with `.invoke()`
- How to execute agents asynchronously with `.ainvoke()`
- Measuring and comparing performance differences
- When to use each approach

## Step 1: Environment Setup

Load environment variables and import required libraries.


```python
from dotenv import load_dotenv
import os

load_dotenv()
```




    True



## Step 2: Import Required Libraries

We'll use LangGraph for agent creation and timing utilities to measure performance.


```python
import time
import asyncio
from typing_extensions import TypedDict

from langgraph.graph import StateGraph, START, END
from langchain_openai import ChatOpenAI

# Initialize OpenAI LLM
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
print("Using OpenAI GPT-4o-mini")
```

    Using OpenAI GPT-4o-mini


## Step 3: Define Agent State

We'll create a simple agent state with input and response fields.


```python
class AgentState(TypedDict):
    """State for our simple agent."""
    input: str
    response: str
```

## Step 4: Create a Simple LangGraph Agent

We'll build a basic agent that processes messages through an LLM. This agent will be simple enough to understand easily while still demonstrating the performance differences between sync and async execution.


```python
def llm_call_node(state: AgentState):
    """Node that processes input through the LLM."""
    response = llm.invoke(state["input"])
    return {"response": response.content}

# Build the graph
workflow = StateGraph(AgentState)
workflow.add_node("llm_call", llm_call_node)
workflow.add_edge(START, "llm_call")
workflow.add_edge("llm_call", END)

# Compile the graph
app = workflow.compile()

print("Agent created successfully!")
```

    Agent created successfully!


## Step 5: Synchronous Execution Example

Let's execute the agent **synchronously** using `.invoke()`. In synchronous execution:
- Each operation blocks until it completes
- Operations execute one at a time
- Simple to understand and debug
- Total time = sum of all operation times


```python
# Test questions for our agent
questions = [
    "What is the capital of France?",
    "What is 15 + 27?",
    "Name a famous scientist."
]

print("=== SYNCHRONOUS EXECUTION ===")
print("Executing 3 agent calls one at a time...\n")

start_time = time.time()

# Execute each call synchronously (one after another)
sync_results = []
for i, question in enumerate(questions, 1):
    call_start = time.time()
    
    result = app.invoke({"input": question})
    
    call_duration = time.time() - call_start
    sync_results.append(result)
    
    answer = result["response"]
    print(f"Call {i} ({call_duration:.2f}s): {question}")
    print(f"Answer: {answer[:100]}...\n")

sync_total_time = time.time() - start_time
print(f"Total synchronous execution time: {sync_total_time:.2f} seconds")
```

    === SYNCHRONOUS EXECUTION ===
    Executing 3 agent calls one at a time...
    
    Call 1 (0.84s): What is the capital of France?
    Answer: The capital of France is Paris....
    
    Call 2 (1.15s): What is 15 + 27?
    Answer: 15 + 27 equals 42....
    
    Call 3 (1.15s): Name a famous scientist.
    Answer: Albert Einstein is a famous scientist known for his contributions to theoretical physics, particular...
    
    Total synchronous execution time: 3.14 seconds


## Step 6: Asynchronous Execution Example

Now let's execute the same agent **asynchronously** using `.ainvoke()`. In asynchronous execution:
- Operations don't block each other
- Multiple operations can run concurrently
- Better for I/O-bound operations (like LLM API calls)
- Total time ≈ time of the slowest operation

**Note**: The async method names are prefixed with 'a': `.ainvoke()`, `.astream()`, etc.


```python
async def run_async_example():
    """Run the asynchronous execution example."""
    print("\n=== ASYNCHRONOUS EXECUTION ===")
    print("Executing 3 agent calls concurrently...\n")
    
    start_time = time.time()
    
    # Create all tasks to run concurrently
    tasks = [
        app.ainvoke({"input": q})
        for q in questions
    ]
    
    # Execute all tasks concurrently using asyncio.gather()
    async_results = await asyncio.gather(*tasks)
    
    async_total_time = time.time() - start_time
    
    # Display results
    for i, (question, result) in enumerate(zip(questions, async_results), 1):
        answer = result["response"]
        print(f"Call {i}: {question}")
        print(f"Answer: {answer[:100]}...\n")
    
    print(f"Total asynchronous execution time: {async_total_time:.2f} seconds")
    
    return async_total_time

# Run the async function
# In Jupyter, we can use await directly in cells
async_total_time = await run_async_example()
```

    
    === ASYNCHRONOUS EXECUTION ===
    Executing 3 agent calls concurrently...
    
    Call 1: What is the capital of France?
    Answer: The capital of France is Paris....
    
    Call 2: What is 15 + 27?
    Answer: 15 + 27 equals 42....
    
    Call 3: Name a famous scientist.
    Answer: Albert Einstein is a famous scientist known for his contributions to physics, particularly for his t...
    
    Total asynchronous execution time: 1.09 seconds


## Step 7: Performance Comparison

Let's visualize the performance difference between synchronous and asynchronous execution.


```python
print("\n" + "="*50)
print("PERFORMANCE COMPARISON")
print("="*50)
print(f"Synchronous total time:  {sync_total_time:.2f} seconds")
print(f"Asynchronous total time: {async_total_time:.2f} seconds")
print(f"\nSpeedup: {sync_total_time / async_total_time:.2f}x faster")
print(f"Time saved: {sync_total_time - async_total_time:.2f} seconds")

improvement_pct = ((sync_total_time - async_total_time) / sync_total_time) * 100
print(f"Performance improvement: {improvement_pct:.1f}%")

print("\n" + "="*50)
```

    
    ==================================================
    PERFORMANCE COMPARISON
    ==================================================
    Synchronous total time:  3.14 seconds
    Asynchronous total time: 1.09 seconds
    
    Speedup: 2.87x faster
    Time saved: 2.04 seconds
    Performance improvement: 65.1%
    
    ==================================================


## Understanding the Results

### Why Async is Faster

**Synchronous execution**:
```
Call 1: [===Wait===] → Response
Call 2:             [===Wait===] → Response  
Call 3:                         [===Wait===] → Response
Total time: Time1 + Time2 + Time3
```

**Asynchronous execution**:
```
Call 1: [===Wait===] → Response
Call 2: [===Wait===] → Response
Call 3: [===Wait===] → Response
Total time: ≈ Longest(Time1, Time2, Time3)
```

### Key Insights

1. **I/O-Bound Operations**: LLM API calls involve network waiting time, not CPU processing
2. **Concurrent Execution**: Async allows multiple API calls to happen at the same time
3. **Scalability**: The performance benefit increases with more concurrent operations
4. **Real-World Impact**: In production with many users, async can handle much higher throughput


```python
from langchain_core.prompts import ChatPromptTemplate

template = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful {role}"),
    ("user", "{input}")
])

messages = template.invoke({
    "role": "coding assistant",
    "input": "How do I reverse a string?"
})
```


```python

```
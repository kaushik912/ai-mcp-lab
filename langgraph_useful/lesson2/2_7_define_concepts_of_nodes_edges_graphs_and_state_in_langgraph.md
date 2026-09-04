# Understanding Nodes, Edges, Graphs, and State in LangGraph

## Introduction

LangGraph is a framework for building stateful applications using a graph-based approach. In this notebook, you'll learn the fundamental building blocks:

- **State**: A shared data structure that holds your application's data
- **Nodes**: Functions that perform computational work
- **Edges**: Connections that define execution flow between nodes
- **Graph**: The container that combines nodes and edges into an executable workflow

**What You'll Learn**:
- How to define State using TypedDict
- How to create nodes as simple Python functions
- How to connect nodes with edges
- How to use START and END to define entry and exit points
- How to compile and run a graph

## Step 1: Import Required Libraries

LangGraph provides the `StateGraph` class for building graphs, and special constants `START` and `END` to define entry and exit points.


```python
from typing_extensions import TypedDict
from langgraph.graph import StateGraph, START, END
```

## Step 2: Define the State

State is a shared data structure that represents a snapshot of your application's data at any point during execution. Nodes read from and write to this state, enabling communication between different parts of the graph.

We define state using Python's `TypedDict` to specify the structure and types of our data.


```python
class State(TypedDict):
    """The state of our graph - a simple counter example."""
    value: int
    message: str
```

Our state has two fields:
- `value`: An integer we'll manipulate
- `message`: A string to track what happened

## Step 3: Create Nodes

Nodes are Python functions that:
1. Receive the current state as input
2. Perform some computation
3. Return a dictionary with state updates

The returned dictionary is **merged** into the existing state - it doesn't replace the entire state.


```python
def add_ten(state: State) -> dict:
    """A node that adds 10 to the value."""
    new_value = state["value"] + 10
    return {
        "value": new_value,
        "message": f"Added 10: {state['value']} -> {new_value}"
    }


def multiply_by_two(state: State) -> dict:
    """A node that multiplies the value by 2."""
    new_value = state["value"] * 2
    return {
        "value": new_value,
        "message": f"Multiplied by 2: {state['value']} -> {new_value}"
    }
```

Notice how each node:
- Reads from `state["value"]` to get the current value
- Computes a new value
- Returns only the fields it wants to update

## Step 4: Create the Graph Builder

The `StateGraph` class is your graph builder. You initialize it with your State class, then add nodes and edges to define your workflow.


```python
# Create the graph builder with our State type
builder = StateGraph(State)

print(f"Created StateGraph builder: {type(builder)}")
```

    Created StateGraph builder: <class 'langgraph.graph.state.StateGraph'>


## Step 5: Add Nodes to the Graph

Use `add_node(name, function)` to register your node functions with the graph. The name is a string identifier used when defining edges.


```python
# Add nodes to the graph
builder.add_node("add_ten", add_ten)
builder.add_node("multiply_by_two", multiply_by_two)

print("Added nodes: 'add_ten' and 'multiply_by_two'")
```

    Added nodes: 'add_ten' and 'multiply_by_two'


## Step 6: Add Edges to Define Flow

Edges define how execution flows from one node to another. LangGraph provides two special constants:

- `START`: Marks where graph execution begins (the entry point)
- `END`: Marks where execution terminates

Use `add_edge(source, target)` to create connections between nodes.


```python
# Define the execution flow:
# START -> add_ten -> multiply_by_two -> END

builder.add_edge(START, "add_ten")           # Entry point: start with add_ten
builder.add_edge("add_ten", "multiply_by_two")  # After add_ten, run multiply_by_two
builder.add_edge("multiply_by_two", END)     # After multiply_by_two, we're done

print("Added edges: START -> add_ten -> multiply_by_two -> END")
```

    Added edges: START -> add_ten -> multiply_by_two -> END


## Step 7: Compile the Graph

Before you can execute a graph, you must compile it. Compilation converts the declarative structure (nodes and edges) into an executable graph that can process state and run nodes.

Without compilation, the builder is just a specification - not an executable program.


```python
# Compile the graph
graph = builder.compile()

print(f"Compiled graph: {type(graph)}")
print("Graph is ready for execution!")
```

    Compiled graph: <class 'langgraph.graph.state.CompiledStateGraph'>
    Graph is ready for execution!


## Step 8: Run the Graph

Now we can invoke the compiled graph with an initial state. The graph will:
1. Start with our input state
2. Execute `add_ten` (adding 10 to our value)
3. Execute `multiply_by_two` (doubling the result)
4. Return the final state


```python
# Create initial state
initial_state = {
    "value": 5,
    "message": "Starting value"
}

print(f"Initial state: {initial_state}")

# Run the graph
result = graph.invoke(initial_state)

print(f"\nFinal state: {result}")
```

    Initial state: {'value': 5, 'message': 'Starting value'}
    
    Final state: {'value': 30, 'message': 'Multiplied by 2: 15 -> 30'}

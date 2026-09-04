# Tutorial: Conditional Edges in LangGraph

## Learning Objectives

By the end of this tutorial, you will be able to:
- Understand what conditional edges are and how they differ from normal edges
- Create routing functions that evaluate state to determine the next node
- Implement conditional edges to build dynamic, adaptive workflows
- Apply conditional routing patterns to real-world agent scenarios

## Prerequisites

- Basic understanding of LangGraph workflows
- Python programming fundamentals
- Familiarity with state graphs and nodes

## What are Conditional Edges?

**Conditional edges** enable dynamic routing in LangGraph workflows by choosing the next node based on runtime conditions. Unlike normal edges that always route to the same destination, conditional edges evaluate the current state and make decisions about where to go next.

### Key Differences:

| Normal Edge | Conditional Edge |
|-------------|------------------|
| Always goes to the same next node | Chooses next node based on logic |
| Static workflow path | Dynamic workflow path |
| Simple linear flow | Branching flow with decision points |

### Use Cases:
- **Tool selection** based on query type (math vs. search vs. calculation)
- **Quality checks** that determine if revision is needed
- **Error handling** with retry logic
- **Different processing paths** for different data types

## Setup

Let's start by importing the necessary libraries and loading environment variables.


```python
# Install required packages if needed
# !pip install langgraph python-dotenv
```


```python
import os
from dotenv import load_dotenv
from typing import TypedDict, Literal
from langgraph.graph import StateGraph, START, END

# Load environment variables
load_dotenv()

print("Environment loaded successfully!")
```

    Environment loaded successfully!


## Anatomy of a Conditional Edge

A conditional edge consists of three core components:

1. **Source Node**: The node where the edge originates
2. **Routing Function**: A function that evaluates the state and returns the name of the next node
3. **Path Map** (optional): A dictionary that maps routing function return values to node names

### Basic Structure:

```python
workflow.add_conditional_edges(
    "source_node",          # Source node name
    routing_function,       # Function that determines next node
    {                       # Path map (optional)
        "option_a": "node_a",
        "option_b": "node_b"
    }
)
```

### Execution Flow:
1. Source node executes and updates state
2. Routing function evaluates the current state
3. Function returns a value indicating the next node
4. Graph routes to the determined node

## Example: Simple Query Routing System

Let's build a simple workflow that demonstrates conditional edges. Our system will:
1. Accept user input
2. Classify the input type
3. Route to different processing nodes based on the classification

### Scenario:
We'll create a query router that:
- Routes **math questions** to a math processor
- Routes **general questions** to a general processor
- Routes **greeting messages** to a greeting processor

### Step 1: Define the State

First, we define our state structure. The state will hold the user input and any processing results.


```python
class QueryState(TypedDict):
    """State structure for our query routing workflow."""
    user_input: str          # The original user query
    query_type: str          # Classified type: 'math', 'general', or 'greeting'
    result: str              # Final processing result

print("State defined successfully!")
```

    State defined successfully!


### Step 2: Create Node Functions

Now we'll create the node functions. Each node performs a specific task and updates the state.


```python
def classifier_node(state: QueryState) -> QueryState:
    """
    Classifies the user input into one of three categories.
    This is our first node that analyzes the input.
    """
    user_input = state["user_input"].lower()
    
    # Simple classification logic
    if any(word in user_input for word in ["hello", "hi", "hey", "greetings"]):
        query_type = "greeting"
    elif any(char in user_input for char in "0123456789+-*/") or any(
        word in user_input for word in ["calculate", "sum", "multiply", "divide"]
    ):
        query_type = "math"
    else:
        query_type = "general"
    
    print(f"Classifier: Input classified as '{query_type}'")
    
    return {
        "user_input": state["user_input"],
        "query_type": query_type,
        "result": state.get("result", "")
    }


def math_processor_node(state: QueryState) -> QueryState:
    """
    Processes math-related queries.
    """
    print("Math Processor: Processing mathematical query...")
    result = f"Math processing result for: '{state['user_input']}' - This would involve mathematical computation."
    
    return {
        "user_input": state["user_input"],
        "query_type": state["query_type"],
        "result": result
    }


def general_processor_node(state: QueryState) -> QueryState:
    """
    Processes general queries.
    """
    print("General Processor: Processing general query...")
    result = f"General processing result for: '{state['user_input']}' - This would involve general knowledge retrieval."
    
    return {
        "user_input": state["user_input"],
        "query_type": state["query_type"],
        "result": result
    }


def greeting_processor_node(state: QueryState) -> QueryState:
    """
    Processes greeting messages.
    """
    print("Greeting Processor: Processing greeting...")
    result = f"Hello! Thank you for your greeting: '{state['user_input']}'. How can I help you today?"
    
    return {
        "user_input": state["user_input"],
        "query_type": state["query_type"],
        "result": result
    }

print("Node functions created successfully!")
```

    Node functions created successfully!


### Step 3: Create the Routing Function

The **routing function** is the heart of conditional edges. It examines the state and returns a string that will be mapped to the next node.

**Key Requirements:**
- Must accept state as a parameter
- Must return a string that can be mapped to a node name via the path map
- Should contain clear, simple logic

**Why use path maps?** Path maps separate routing logic from node naming, making your code more maintainable and easier to understand.


```python
def route_query(state: QueryState) -> Literal["math", "general", "greeting"]:
    """
    Routing function that determines which processor to use based on query_type.
    
    This function evaluates the state and returns a category string.
    The path map will translate this to the actual node name.
    """
    query_type = state["query_type"]
    
    print(f"Router: Routing to '{query_type}' category")
    
    # Return simple category names that will be mapped to actual nodes
    if query_type == "math":
        return "math"
    elif query_type == "greeting":
        return "greeting"
    else:
        return "general"

print("Routing function created successfully!")
```

    Routing function created successfully!


### Step 4: Build the Workflow

Now we'll construct the complete workflow by:
1. Creating a StateGraph
2. Adding all nodes
3. Adding edges (normal and conditional)
4. Compiling the graph


```python
# Create the workflow
workflow = StateGraph(QueryState)

# Add all nodes to the workflow
workflow.add_node("classifier", classifier_node)
workflow.add_node("math_processor", math_processor_node)
workflow.add_node("general_processor", general_processor_node)
workflow.add_node("greeting_processor", greeting_processor_node)

# Add normal edge from START to classifier
# This edge always goes to the classifier node
workflow.add_edge(START, "classifier")

# Add CONDITIONAL EDGE from classifier to processors
# This is where the magic happens!
# The route_query function determines the category,
# and the path map translates it to the actual node name
workflow.add_conditional_edges(
    "classifier",      # Source node
    route_query,       # Routing function
    {                  # Path map: routing output → node name
        "math": "math_processor",
        "general": "general_processor",
        "greeting": "greeting_processor"
    }
)

# Add normal edges from all processors to END
workflow.add_edge("math_processor", END)
workflow.add_edge("general_processor", END)
workflow.add_edge("greeting_processor", END)

# Compile the workflow
app = workflow.compile()

print("Workflow built and compiled successfully!")
```

    Workflow built and compiled successfully!


## Demonstration: Testing the Conditional Routing

Let's test our workflow with different types of inputs to see how conditional edges route to different nodes.

### Test Case 1: Math Query

This should route to the math processor.


```python
print("=" * 60)
print("TEST CASE 1: Math Query")
print("=" * 60)

initial_state = {
    "user_input": "What is 25 + 37?",
    "query_type": "",
    "result": ""
}

result = app.invoke(initial_state)

print("\nFinal State:")
print(f"Input: {result['user_input']}")
print(f"Type: {result['query_type']}")
print(f"Result: {result['result']}")
```

    ============================================================
    TEST CASE 1: Math Query
    ============================================================
    Classifier: Input classified as 'math'
    Router: Routing to 'math' category
    Math Processor: Processing mathematical query...
    
    Final State:
    Input: What is 25 + 37?
    Type: math
    Result: Math processing result for: 'What is 25 + 37?' - This would involve mathematical computation.


### Test Case 2: General Query

This should route to the general processor.


```python
print("=" * 60)
print("TEST CASE 2: General Query")
print("=" * 60)

initial_state = {
    "user_input": "What is the capital of France?",
    "query_type": "",
    "result": ""
}

result = app.invoke(initial_state)

print("\nFinal State:")
print(f"Input: {result['user_input']}")
print(f"Type: {result['query_type']}")
print(f"Result: {result['result']}")
```

    ============================================================
    TEST CASE 2: General Query
    ============================================================
    Classifier: Input classified as 'general'
    Router: Routing to 'general' category
    General Processor: Processing general query...
    
    Final State:
    Input: What is the capital of France?
    Type: general
    Result: General processing result for: 'What is the capital of France?' - This would involve general knowledge retrieval.


### Test Case 3: Greeting

This should route to the greeting processor.


```python
print("=" * 60)
print("TEST CASE 3: Greeting")
print("=" * 60)

initial_state = {
    "user_input": "Hello there!",
    "query_type": "",
    "result": ""
}

result = app.invoke(initial_state)

print("\nFinal State:")
print(f"Input: {result['user_input']}")
print(f"Type: {result['query_type']}")
print(f"Result: {result['result']}")
```

    ============================================================
    TEST CASE 3: Greeting
    ============================================================
    Classifier: Input classified as 'greeting'
    Router: Routing to 'greeting' category
    Greeting Processor: Processing greeting...
    
    Final State:
    Input: Hello there!
    Type: greeting
    Result: Hello! Thank you for your greeting: 'Hello there!'. How can I help you today?

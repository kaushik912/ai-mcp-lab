# Tutorial: Using MCP Tools with LangChain Agents

## Learning Objectives

By the end of this tutorial, you will be able to:
- Connect a LangGraph agent to an MCP server
- Use MCP tools within an agent workflow
- Understand the `MultiServerMCPClient` pattern

## Prerequisites

- Understanding of MCP concepts from the slides
- Familiarity with LangGraph agents and tool calling
- Basic async/await knowledge

## Setup


```python
# !pip install langchain-mcp-adapters fastmcp langchain langchain-openai python-dotenv
```


```python
import os
import sys
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()
assert os.getenv("OPENAI_API_KEY"), "Please set OPENAI_API_KEY in your .env file"

print("Environment loaded!")
```

    Environment loaded!


## The MCP Server

This directory contains `knowledge_base_server.py` - a FastMCP server with these tools:

| Tool | Description |
|------|-------------|
| `create_note` | Create a note with title, content, tags |
| `search_notes` | Search notes by keyword |
| `get_note` | Get a note by ID |
| `list_notes` | List all notes |
| `update_note` | Update a note |
| `delete_note` | Delete a note |
| `get_statistics` | Get KB statistics |

## Connecting to the MCP Server

Use `MultiServerMCPClient` to connect to MCP servers. For local servers, use `stdio` transport.


```python
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI

# Path to our MCP server
SERVER_PATH = Path(".").resolve() / "knowledge_base_server.py"

# MCP client configuration
mcp_config = {
    "knowledge_base": {
        "transport": "stdio",
        "command": sys.executable,
        "args": [str(SERVER_PATH)],
    }
}

# Initialize LLM
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
```

## Running the Agent with MCP Tools

The pattern:
1. Create `MultiServerMCPClient` with server config
2. Get tools with `await client.get_tools()`
3. Pass tools to `create_agent()`


```python
async def run_agent(user_message: str):
    """Run a LangChain agent with MCP tools."""
    client = MultiServerMCPClient(mcp_config)
    tools = await client.get_tools()
    
    print(f"Available tools: {[t.name for t in tools]}\n")
    
    agent = create_agent(model=llm, tools=tools)
    result = await agent.ainvoke({
        "messages": [{"role": "user", "content": user_message}]
    })
    
    return result["messages"][-1].content
```

## Test: Creating Notes


```python
response = await run_agent(
    "Create a note titled 'Python Decorators' about how decorators work. "
    "Tag it with 'python' and 'tutorial'."
)
print(response)
```

    Available tools: ['create_note', 'search_notes', 'get_note', 'list_notes', 'update_note', 'delete_note', 'get_statistics']
    
    The note titled **"Python Decorators"** has been created successfully. Here are the details:
    
    ### Python Decorators
    **Content:**
    Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.
    
    #### How Decorators Work:
    1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.
    
    2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends or modifies the behavior of the original function.
    
    3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.
    
    #### Example:
    ```python
    def my_decorator(func):
        def wrapper():
            print("Something is happening before the function is called.")
            func()
            print("Something is happening after the function is called.")
        return wrapper
    
    @my_decorator
    def say_hello():
        print("Hello!")
    
    say_hello()
    ```
    
    #### Output:
    ```
    Something is happening before the function is called.
    Hello!
    Something is happening after the function is called.
    ```
    
    #### Use Cases:
    - Logging
    - Access control
    - Caching
    - Validation
    
    Decorators can also take arguments, allowing for even more flexibility. They are widely used in frameworks like Flask and Django for routing and middleware functionality.
    
    **Tags:** python, tutorial
    
    If you need any further modifications or additional notes, feel free to ask!



```python
response = await run_agent(
    "Create a note about LangGraph basics covering nodes, edges, and state. "
    "Tag it with 'langgraph' and 'agents'."
)
print(response)
```

    Available tools: ['create_note', 'search_notes', 'get_note', 'list_notes', 'update_note', 'delete_note', 'get_statistics']
    
    The note titled **"LangGraph Basics"** has been successfully created. Here are the details:
    
    ### LangGraph Basics
    LangGraph is a framework that allows for the representation and manipulation of knowledge in a graph structure. The fundamental components of LangGraph include:
    
    1. **Nodes**: These are the individual entities or concepts within the graph. Each node can represent a specific piece of information, such as a word, phrase, or even a more complex idea. Nodes can have attributes that provide additional context or metadata.
    
    2. **Edges**: Edges are the connections between nodes. They define the relationships between different entities in the graph. Edges can be directed or undirected, indicating the nature of the relationship (e.g., a parent-child relationship or a mutual connection).
    
    3. **State**: The state in LangGraph refers to the current configuration or condition of the graph at any given time. This includes the active nodes, the relationships that are currently being utilized, and any changes that have been made to the graph structure. Managing state is crucial for dynamic interactions and updates within the graph.
    
    Understanding these basic components is essential for effectively using LangGraph in various applications, including natural language processing, knowledge representation, and agent-based systems.
    
    **Tags**: langgraph, agents
    
    If you need any further modifications or additional notes, feel free to ask!


## Test: Searching and Listing


```python
response = await run_agent("Search for notes about Python.")
print(response)
```

    Available tools: ['create_note', 'search_notes', 'get_note', 'list_notes', 'update_note', 'delete_note', 'get_statistics']
    
    I found two notes related to Python:
    
    1. **Title:** Python Decorators
       - **Content:** 
         Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.
         
         ### How Decorators Work:
         1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.
         
         2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends the behavior of the original function.
         
         3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax just above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.
         
         ### Example:
         ```python
         def my_decorator(func):
             def wrapper():
                 print("Something is happening before the function is called.")
                 func()
                 print("Something is happening after the function is called.")
             return wrapper
         
         @my_decorator
         def say_hello():
             print("Hello!")
         
         say_hello()
         ```
         
         ### Output:
         ```
         Something is happening before the function is called.
         Hello!
         Something is happening after the function is called.
         ```
         
         ### Use Cases:
         - Logging
         - Access control
         - Caching
         - Validation
         
         Decorators are a key feature in Python that can help you write cleaner and more maintainable code by separating concerns and enhancing functionality without modifying the original code directly.
       - **Tags:** python, tutorial
       - **Created At:** December 10, 2025
    
    2. **Title:** Python Decorators
       - **Content:** 
         Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.
         
         ### How Decorators Work:
         1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.
         
         2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends or modifies the behavior of the original function.
         
         3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.
         
         ### Example:
         ```python
         def my_decorator(func):
             def wrapper():
                 print("Something is happening before the function is called.")
                 func()
                 print("Something is happening after the function is called.")
             return wrapper
         
         @my_decorator
         def say_hello():
             print("Hello!")
         
         say_hello()
         ```
         
         ### Output:
         ```
         Something is happening before the function is called.
         Hello!
         Something is happening after the function is called.
         ```
         
         ### Use Cases:
         - Logging
         - Access control
         - Caching
         - Validation
         
         Decorators can also take arguments, allowing for even more flexibility. They are widely used in frameworks like Flask and Django for routing and middleware functionality.
       - **Tags:** python, tutorial
       - **Created At:** December 10, 2025
    
    If you need more information or want to explore a specific note, let me know!



```python
response = await run_agent("List all my notes and show me the knowledge base statistics.")
print(response)
```

    Available tools: ['create_note', 'search_notes', 'get_note', 'list_notes', 'update_note', 'delete_note', 'get_statistics']
    
    ### Your Notes
    
    1. **Title**: LangGraph Basics
       - **Content**: LangGraph is a framework that allows for the representation and manipulation of knowledge in a graph structure. The fundamental components of LangGraph include:
         - **Nodes**: Individual entities or concepts within the graph.
         - **Edges**: Connections between nodes that define relationships.
         - **State**: The current configuration of the graph.
       - **Tags**: langgraph, agents
       - **Created At**: December 10, 2025
       - **Updated At**: December 10, 2025
    
    2. **Title**: Python Decorators
       - **Content**: Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.
         - **How Decorators Work**: Functions as first-class objects, defining a decorator, and using a decorator.
         - **Example**: Code snippet demonstrating a simple decorator.
         - **Use Cases**: Logging, access control, caching, validation.
       - **Tags**: python, tutorial
       - **Created At**: December 10, 2025
       - **Updated At**: December 10, 2025
    
    3. **Title**: Python Decorators
       - **Content**: (Same as above)
       - **Tags**: python, tutorial
       - **Created At**: December 10, 2025
       - **Updated At**: December 10, 2025
    
    ---
    
    ### Knowledge Base Statistics
    
    - **Total Notes**: 3
    - **Unique Tags**: 4
    - **Top Tags**:
      - python: 2
      - tutorial: 2
      - langgraph: 1
      - agents: 1
    - **Recent Notes**:
      - LangGraph Basics
      - Python Decorators
      - Python Decorators
    
    If you need any further information or actions regarding your notes, feel free to ask!


## Test: Multi-Tool Task


```python
response = await run_agent(
    "Check what notes I have, then create a 'Learning Roadmap' note "
    "that references the topics from my existing notes. Tag it with 'meta'."
)
print(response)
```

    Available tools: ['create_note', 'search_notes', 'get_note', 'list_notes', 'update_note', 'delete_note', 'get_statistics']
    
    I have checked your existing notes and created a new note titled **"Learning Roadmap"**. This note references the topics from your existing notes and is tagged with **"meta"**. 
    
    ### Learning Roadmap
    This roadmap outlines the key topics to explore based on existing notes:
    
    1. **LangGraph Basics**
       - Understanding nodes, edges, and state in graph structures.
       - Applications in natural language processing and knowledge representation.
    
    2. **Python Decorators**
       - How decorators work and their use cases.
       - Examples of defining and using decorators in Python.
    
    This roadmap serves as a guide for further learning and exploration in these areas. 
    
    If you need any further assistance or modifications, feel free to ask!


## Inspecting Tool Calls

Let's see the full trace of what the agent does.


```python
async def run_agent_verbose(user_message: str):
    """Run agent and show detailed tool call trace."""
    client = MultiServerMCPClient(mcp_config)
    tools = await client.get_tools()
    agent = create_agent(model=llm, tools=tools)
    
    result = await agent.ainvoke({
        "messages": [{"role": "user", "content": user_message}]
    })
    
    print("TRACE:")
    print("=" * 50)
    for i, msg in enumerate(result["messages"]):
        msg_type = type(msg).__name__
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                print(f"[{i}] Tool: {tc['name']}({tc['args']})")
        elif msg_type == "ToolMessage":
            content = msg.content[:100] + "..." if len(msg.content) > 100 else msg.content
            print(f"[{i}] Result: {content}")
    print("=" * 50)
    
    return result["messages"][-1].content
```


```python
response = await run_agent_verbose(
    "Find notes tagged 'python' and add 'programming' to their tags."
)
print(f"\nFinal response:\n{response}")
```

    TRACE:
    ==================================================
    [1] Tool: list_notes({'tag_filter': 'python'})
    [2] Result: [{'type': 'text', 'text': '{"status":"success","total_count":4,"returned_count":2,"filter_applied":"python","notes":[{"id":"309f851d","title":"Python Decorators","content":"Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.\\n\\n### How Decorators Work:\\n1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.\\n\\n2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends or modifies the behavior of the original function.\\n\\n3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.\\n\\n### Example:\\n```python\\ndef my_decorator(func):\\n    def wrapper():\\n        print(\\"Something is happening before the function is called.\\")\\n        func()\\n        print(\\"Something is happening after the function is called.\\")\\n    return wrapper\\n\\n@my_decorator\\ndef say_hello():\\n    print(\\"Hello!\\")\\n\\nsay_hello()\\n```\\n\\n### Output:\\n```\\nSomething is happening before the function is called.\\nHello!\\nSomething is happening after the function is called.\\n```\\n\\n### Use Cases:\\n- Logging\\n- Access control\\n- Caching\\n- Validation\\n\\nDecorators can also take arguments, allowing for even more flexibility. They are widely used in frameworks like Flask and Django for routing and middleware functionality.","tags":["python","tutorial"],"created_at":"2025-12-10T12:44:02.867233","updated_at":"2025-12-10T12:44:02.867233"},{"id":"beb35560","title":"Python Decorators","content":"Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.\\n\\n### How Decorators Work:\\n1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.\\n\\n2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends the behavior of the original function.\\n\\n3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax just above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.\\n\\n### Example:\\n```python\\ndef my_decorator(func):\\n    def wrapper():\\n        print(\\"Something is happening before the function is called.\\")\\n        func()\\n        print(\\"Something is happening after the function is called.\\")\\n    return wrapper\\n\\n@my_decorator\\ndef say_hello():\\n    print(\\"Hello!\\")\\n\\nsay_hello()\\n```\\n\\n### Output:\\n```\\nSomething is happening before the function is called.\\nHello!\\nSomething is happening after the function is called.\\n```\\n\\n### Use Cases:\\n- Logging\\n- Access control\\n- Caching\\n- Validation\\n\\nDecorators are a key feature in Python that can help you write cleaner and more maintainable code by separating concerns and enhancing functionality without modifying the original code directly.","tags":["python","tutorial"],"created_at":"2025-12-10T12:40:04.469786","updated_at":"2025-12-10T12:40:04.469786"}]}', 'id': 'lc_f725296e-e078-4208-8604-f5ad365926c1'}]
    [3] Tool: update_note({'note_id': '309f851d', 'tags': ['python', 'tutorial', 'programming']})
    [3] Tool: update_note({'note_id': 'beb35560', 'tags': ['python', 'tutorial', 'programming']})
    [4] Result: [{'type': 'text', 'text': '{"status":"success","message":"Note \'Python Decorators\' updated successfully","note":{"id":"309f851d","title":"Python Decorators","content":"Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.\\n\\n### How Decorators Work:\\n1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.\\n\\n2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends or modifies the behavior of the original function.\\n\\n3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.\\n\\n### Example:\\n```python\\ndef my_decorator(func):\\n    def wrapper():\\n        print(\\"Something is happening before the function is called.\\")\\n        func()\\n        print(\\"Something is happening after the function is called.\\")\\n    return wrapper\\n\\n@my_decorator\\ndef say_hello():\\n    print(\\"Hello!\\")\\n\\nsay_hello()\\n```\\n\\n### Output:\\n```\\nSomething is happening before the function is called.\\nHello!\\nSomething is happening after the function is called.\\n```\\n\\n### Use Cases:\\n- Logging\\n- Access control\\n- Caching\\n- Validation\\n\\nDecorators can also take arguments, allowing for even more flexibility. They are widely used in frameworks like Flask and Django for routing and middleware functionality.","tags":["python","tutorial","programming"],"created_at":"2025-12-10T12:44:02.867233","updated_at":"2025-12-10T12:45:32.211737"}}', 'id': 'lc_da6a3bbf-eccb-43c2-a7ea-9702485eabb3'}]
    [5] Result: [{'type': 'text', 'text': '{"status":"success","message":"Note \'Python Decorators\' updated successfully","note":{"id":"beb35560","title":"Python Decorators","content":"Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way. A decorator is essentially a function that takes another function as an argument and extends its behavior without explicitly modifying it.\\n\\n### How Decorators Work:\\n1. **Function as First-Class Objects**: In Python, functions are first-class objects, meaning they can be passed around as arguments, returned from other functions, and assigned to variables.\\n\\n2. **Defining a Decorator**: A decorator is defined as a function that returns another function. The inner function usually contains the code that extends the behavior of the original function.\\n\\n3. **Using a Decorator**: You can apply a decorator to a function using the `@decorator_name` syntax just above the function definition. This is syntactic sugar for calling the decorator function and passing the original function as an argument.\\n\\n### Example:\\n```python\\ndef my_decorator(func):\\n    def wrapper():\\n        print(\\"Something is happening before the function is called.\\")\\n        func()\\n        print(\\"Something is happening after the function is called.\\")\\n    return wrapper\\n\\n@my_decorator\\ndef say_hello():\\n    print(\\"Hello!\\")\\n\\nsay_hello()\\n```\\n\\n### Output:\\n```\\nSomething is happening before the function is called.\\nHello!\\nSomething is happening after the function is called.\\n```\\n\\n### Use Cases:\\n- Logging\\n- Access control\\n- Caching\\n- Validation\\n\\nDecorators are a key feature in Python that can help you write cleaner and more maintainable code by separating concerns and enhancing functionality without modifying the original code directly.","tags":["python","tutorial","programming"],"created_at":"2025-12-10T12:40:04.469786","updated_at":"2025-12-10T12:45:32.211860"}}', 'id': 'lc_f7d7601b-bcc2-47da-a145-15a1640380a1'}]
    ==================================================
    
    Final response:
    I found two notes tagged with 'python' and successfully added 'programming' to their tags. Here are the updated details:
    
    1. **Note Title**: Python Decorators
       - **Tags**: python, tutorial, programming
       - **Content**: Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way...
    
    2. **Note Title**: Python Decorators
       - **Tags**: python, tutorial, programming
       - **Content**: Decorators in Python are a powerful tool that allows you to modify the behavior of a function or class. They are often used to add functionality to existing code in a clean and readable way...
    
    If you need any further assistance, feel free to ask!


## Cleanup


```python
import json

storage_file = Path(".").resolve() / "data" / "notes.json"
if storage_file.exists():
    storage_file.write_text(json.dumps({"notes": {}}, indent=2))
    print("Knowledge base cleared.")
```

    Knowledge base cleared.


## Summary

- Create `MultiServerMCPClient(config)` with `stdio` transport for local MCP servers
- Call `await client.get_tools()` to get LangChain-compatible tools
- Pass tools to `create_agent()` just like any other LangChain tools
- The same MCP server works with Claude Desktop, Cursor, and custom agents
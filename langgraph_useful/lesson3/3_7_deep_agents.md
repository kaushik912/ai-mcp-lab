# Building Deep Agents with LangChain's DeepAgents Package

## Introduction

This tutorial teaches you how to build and run **deep agents** using LangChain's DeepAgents package. You'll learn what makes deep agents powerful and how to implement sophisticated agentic systems capable of handling complex, long-running tasks.

### What You'll Learn
- Understanding deep planning in AI agents
- The four core characteristics of deep agents
- How to install and configure the deepagents package
- Building a deep agent with ChatOpenAI and Tavily search
- Running and testing deep agents
- Practical examples demonstrating deep agent capabilities

### Prerequisites
- Basic Python knowledge
- Understanding of LLMs and AI agents
- OpenAI API key
- Tavily API key (for search functionality)

### Learning Objectives
By the end of this tutorial, you'll be able to:
1. Identify the four core characteristics that make an agent "deep"
2. Build a functional deep agent using the deepagents package
3. Implement planning tools, file system access, and subagents
4. Deploy deep agents for complex, multi-step tasks

## Part 1: Understanding Deep Agents

### What Are Deep Agents?

**Deep agents** are advanced AI systems that:
- Go beyond basic tool-calling loops
- Handle complex, long-running tasks
- Maintain sophisticated coordination and memory management
- Use persistent storage and planning tools
- Examples include: Claude Code, Deep Research systems, Manus

### The Four Core Characteristics of Deep Agents

| Characteristic | Description |
|----------------|-------------|
| **1. System Prompts** | Detailed instructions with examples, edge cases, and workflows |
| **2. Planning Tools** | Explicit planning tools (todo lists, progress tracking) |
| **3. Architecture** | Multiple specialized sub-agents for task decomposition |
| **4. Memory/Storage** | File system access + persistent storage |

Let's explore each characteristic in detail:

### 1. Detailed System Prompts

**Example Deep Agent Prompt:**
```
You are an expert researcher with access to web search and file tools.

TOOL USAGE PATTERNS:
- When searching: start broad, then narrow based on results
- When analyzing code: try function definitions first, then usage examples
- For debugging: read full error → check related files → test incrementally

ERROR HANDLING:
- If a search returns no results, try alternative keywords
- If a file is not found, check the directory structure first

MULTI-STEP WORKFLOWS:
1. Break complex tasks into subtasks using the todo tool
2. Save intermediate findings to files for later reference
3. Use subagents for specialized analysis
```

Deep agent prompts provide comprehensive guidance on tool usage, error handling, and workflows.

### 2. Planning Tools

Planning tools help agents maintain focus on long-term objectives:
- **Purpose**: Context engineering to keep agents organized
- **Implementation**: Todo lists, step trackers, progress monitors
- **Benefit**: Agent stays on track across complex, multi-step tasks
- **Key Insight**: Even if functionally simple, planning tools serve as prompts that structure the agent's thinking

### 3. Sub-agents

Sub-agents enable task decomposition:
- **Task decomposition**: Break complex work into specialized subtasks
- **Focused expertise**: Each sub-agent handles a specific domain
- **Context management**: Reduce prompt complexity per agent
- **Example**: Main agent delegates to `code_analyzer`, `test_generator`, `docs_writer`
- **Benefit**: Enables deep focus while maintaining coordination

### 4. File System Access & Persistent Memory

File system access provides:
- **Persistent memory** beyond conversation context
- **Shared workspace** for multi-agent collaboration
- **Long-term state** across sessions or restarts
- **Artifact storage** for intermediate results

This allows agents to:
- Save analysis findings for later reference
- Store plans that multiple sub-agents can access
- Build up a knowledge base over extended workflows
- Enable asynchronous agent coordination

## Part 2: Setup and Installation

Let's set up our environment and install the necessary packages.


```python
# Install required packages
# Run this cell first if you don't have these packages installed
# !pip install -q deepagents langchain-openai tavily-python python-dotenv
```

### Environment Setup

We'll use `load_dotenv()` to load API keys from a `.env` file. Create a `.env` file in your project directory with:

```
OPENAI_API_KEY=your_openai_api_key_here
TAVILY_API_KEY=your_tavily_api_key_here
```

Get your API keys from:
- OpenAI: https://platform.openai.com/api-keys
- Tavily: https://tavily.com/


```python
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Verify API keys are loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment")
if not os.getenv("TAVILY_API_KEY"):
    raise ValueError("TAVILY_API_KEY not found in environment")

print("Environment variables loaded successfully!")
```

    Environment variables loaded successfully!


## Part 3: Building Your First Deep Agent

Now let's build a deep agent step by step. We'll start with the basic components and gradually add more sophisticated features.

### Step 1: Import Required Libraries


```python
from deepagents import create_deep_agent
from langchain_openai import ChatOpenAI
from tavily import TavilyClient
```

### Step 2: Configure the LLM Model

We'll use ChatOpenAI as our language model. The deepagents package defaults to Claude Sonnet 4.5, but we'll explicitly use GPT-4o for this tutorial.


```python
# Initialize the ChatOpenAI model
model = ChatOpenAI(
    model="gpt-4o",
    temperature=0.1,
    api_key=os.getenv("OPENAI_API_KEY")
)

print(f"Model initialized: {model.model_name}")
```

    Model initialized: gpt-4o


### Step 3: Set Up the Tavily Search Tool

Tavily provides a powerful search API that deep agents can use to access current information from the web.


```python
# Initialize Tavily client
tavily_client = TavilyClient(api_key=os.getenv("TAVILY_API_KEY"))

# Create a search tool function
def internet_search(query: str, max_results: int = 5) -> dict:
    """
    Search the web for information using Tavily.
    
    Args:
        query: The search query string
        max_results: Maximum number of results to return (default: 5)
    
    Returns:
        Dictionary containing search results with titles, URLs, and content
    """
    try:
        results = tavily_client.search(query, max_results=max_results)
        return results
    except Exception as e:
        return {"error": f"Search failed: {str(e)}"}

print("Tavily search tool configured successfully!")
```

    Tavily search tool configured successfully!


### Step 4: Create a Basic Deep Agent

Let's create our first deep agent with a detailed system prompt and the search tool.


```python
from pathlib import Path
from deepagents.backends import FilesystemBackend

# Create a dedicated workspace directory for agent files
agent_workspace = Path("./agent_workspace")
agent_workspace.mkdir(exist_ok=True)

print(f"Agent workspace created at: {agent_workspace.absolute()}")
```

    Agent workspace created at: /Users/sajal/code/active/projects/oreilly_ai_agent_skill/level_3/agent_workspace


### Optional: Configure File System Directory

By default, deep agents use ephemeral storage (files are lost when the thread ends). Let's configure a persistent directory where the agent will save files:


```python
# Define a detailed system prompt (characteristic #1 of deep agents)
system_prompt = """You are an expert research assistant with access to web search and file management tools.

YOUR ROLE:
- Conduct thorough research on user queries
- Break down complex questions into manageable subtasks
- Synthesize information from multiple sources
- Save important findings to files for reference.

TOOL USAGE GUIDELINES:
- Use internet_search for current information and facts
- Start with broad searches, then narrow based on results
- Always cite sources when presenting information
- Save detailed research findings to files for later reference

WORKFLOW FOR COMPLEX TASKS:
1. Break the task into subtasks using the write_todos tool
2. Execute each subtask systematically
3. Save intermediate findings to files
4. Synthesize results into a comprehensive answer

ERROR HANDLING:
- If a search returns no results, try alternative keywords
- If information is unclear, search for clarification
- Always verify facts from multiple sources when possible
"""

# Create the deep agent with persistent filesystem
agent = create_deep_agent(
    model=model,
    tools=[internet_search],
    system_prompt=system_prompt,
    backend=FilesystemBackend(root_dir=str(agent_workspace), virtual_mode=True)
)

print("Deep agent created successfully!")
print("\\nAgent capabilities:")
print("- Planning tool (write_todos)")
print("- File system tools (read, write, edit files)")
print("- Internet search via Tavily")
print("- Subagent spawning (task tool)")
print(f"- Files will be saved to: {agent_workspace.absolute()}")
```

    Deep agent created successfully!
    \nAgent capabilities:
    - Planning tool (write_todos)
    - File system tools (read, write, edit files)
    - Internet search via Tavily
    - Subagent spawning (task tool)
    - Files will be saved to: /Users/sajal/code/active/projects/oreilly_ai_agent_skill/level_3/agent_workspace


### Understanding What Just Happened

The `create_deep_agent()` function automatically equipped our agent with:

1. **TodoListMiddleware**: Provides the `write_todos` tool for planning
2. **FilesystemMiddleware**: Adds file system tools (`ls`, `read_file`, `write_file`, `edit_file`, `glob`, `grep`)
3. **SubAgentMiddleware**: Enables the `task` tool for spawning specialized subagents
4. **Custom Tools**: Our `internet_search` function

This demonstrates the power of deep agents: they have built-in capabilities for planning, memory, and task decomposition that enable sophisticated workflows.

## Part 4: Running the Deep Agent

Let's test our deep agent with progressively more complex tasks to see how it leverages its deep planning capabilities.

### Helper Function: Stream Agent Responses

Before running examples, let's create a reusable helper function to stream and display agent responses with clean formatting. This function:

- **Tracks message IDs** to avoid duplicate output (since `stream_mode="values"` returns full state)
- **Formats different message types** clearly (`[USER]`, `[AGENT]`, `[TOOL]`)
- **Supports detailed mode** to show planning, search queries, and file operations
- **Handles content truncation** for long responses

This makes it easy to observe agent behavior across different examples.


```python
def stream_agent_response(agent, query, show_details=False, max_content_length=1000):
    """
    Stream and display agent responses with clean formatting.
    
    Args:
        agent: The deep agent instance
        query: The query string to send to the agent
        show_details: If True, show detailed tool arguments and planning
        max_content_length: Maximum characters to show for content (None for full content)
    """
    print(f"Query: {query if len(query) <= 100 else query[:100] + '...'}")
    print("\n" + "="*80)
    print("Streaming Agent Actions:")
    print("="*80 + "\n")
    
    # Track message IDs to avoid duplicates
    seen_message_ids = set()
    
    # Stream using stream_mode="values"
    for chunk in agent.stream(
        {"messages": [{"role": "user", "content": query}]},
        stream_mode="values"
    ):
        if "messages" in chunk:
            for message in chunk["messages"]:
                msg_id = message.id
                
                # Skip if we've already printed this message
                if msg_id in seen_message_ids:
                    continue
                    
                seen_message_ids.add(msg_id)
                msg_type = getattr(message, 'type', 'unknown')
                
                # Format based on message type
                if msg_type == 'human':
                    content = message.content
                    if max_content_length and len(content) > max_content_length:
                        content = content[:max_content_length] + "..."
                    print(f"[USER]: {content}")
                    print("-" * 80)
                    
                elif msg_type == 'ai':
                    if hasattr(message, 'tool_calls') and message.tool_calls:
                        print(f"[AGENT] Calling {len(message.tool_calls)} tool(s):")
                        for tc in message.tool_calls:
                            tool_name = tc.get('name', 'unknown')
                            tool_args = tc.get('args', {})
                            
                            # Show detailed formatting for specific tools
                            if tool_name == 'write_todos' and show_details:
                                todos = tool_args.get('todos', [])
                                print(f"  - {tool_name}: Planning {len(todos)} tasks")
                                for i, todo in enumerate(todos, 1):
                                    status = todo.get('status', 'pending')
                                    content = todo.get('content', '')[:50]
                                    print(f"      {i}. [{status}] {content}...")
                            elif tool_name == 'internet_search' and show_details:
                                query_str = tool_args.get('query', '')
                                print(f"  - {tool_name}: '{query_str}'")
                            elif tool_name == 'write_file' and show_details:
                                path = tool_args.get('path', '')
                                print(f"  - {tool_name}: {path}")
                            else:
                                # Simple format: show function call with key args
                                args_str = ', '.join(f'{k}={v}' for k, v in list(tool_args.items())[:2])
                                print(f"  - {tool_name}({args_str})")
                                
                    elif message.content:
                        content = message.content
                        if max_content_length and len(content) > max_content_length:
                            content = content[:max_content_length] + "..."
                        print(f"[AGENT]: {content}")
                    print("-" * 80)
                    
                elif msg_type == 'tool':
                    tool_name = getattr(message, 'name', 'unknown')
                    print(f"[TOOL: {tool_name}] Completed")
                    print("-" * 80)
    
    print("\n" + "="*80)
    print("Execution completed!")
    print("="*80)

print("Helper function 'stream_agent_response' created!")
```

    Helper function 'stream_agent_response' created!


### Example 1: Simple Query

First, let's see how the deep agent handles a simple query.


```python
# Simple query with streaming
simple_query = "What is LangGraph?"

# Use the helper function to stream and display the response
stream_agent_response(agent, simple_query, show_details=False, max_content_length=None)
```

    Query: What is LangGraph?
    
    ================================================================================
    Streaming Agent Actions:
    ================================================================================
    
    [USER]: What is LangGraph?
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - internet_search(query=LangGraph, max_results=5)
    --------------------------------------------------------------------------------
    [TOOL: internet_search] Completed
    --------------------------------------------------------------------------------
    [AGENT]: LangGraph is an open-source AI agent framework designed to build, deploy, and manage complex generative AI agent workflows. It utilizes graph-based architectures to model and manage the intricate relationships between various components of an AI agent workflow. LangGraph is built on several key technologies, including LangChain, a Python framework for building AI applications. It provides a versatile platform for developing AI solutions and workflows, such as chatbots, state graphs, and other agent-based systems. LangGraph is particularly useful for creating scalable and production-ready AI workloads, offering features like long-term memory and customizable architectures for complex task handling.
    
    For more detailed information, you can visit [IBM's LangGraph overview](https://www.ibm.com/think/topics/langgraph) or explore the [LangGraph GitHub repository](https://github.com/langchain-ai/langgraph).
    --------------------------------------------------------------------------------
    
    ================================================================================
    Execution completed!
    ================================================================================


### Example 2: Multi-Step Research Task

Now let's give the agent a complex task that requires planning and multiple steps. We'll use `show_details=True` to see the planning, search queries, and file operations in detail.


```python
# Complex multi-step task with streaming
complex_query = """Research the current state of AI agents in 2025. I need:
1. The main frameworks and tools being used
2. Key differences between agent architectures
3. Real-world applications and use cases
4. Save your findings to a file called 'ai_agents_research_2025.txt' in the current directory.
"""

# Use the helper function with detailed output for complex tasks
stream_agent_response(agent, complex_query, show_details=True, max_content_length=None)
```

    Query: Research the current state of AI agents in 2025. I need:
    1. The main frameworks and tools being used...
    
    ================================================================================
    Streaming Agent Actions:
    ================================================================================
    
    [USER]: Research the current state of AI agents in 2025. I need:
    1. The main frameworks and tools being used
    2. Key differences between agent architectures
    3. Real-world applications and use cases
    4. Save your findings to a file called 'ai_agents_research_2025.txt' in the current directory.
    
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - write_todos: Planning 5 tasks
          1. [in_progress] Research the main frameworks and tools being used ...
          2. [pending] Research key differences between AI agent architec...
          3. [pending] Research real-world applications and use cases of ...
          4. [pending] Compile research findings into a comprehensive rep...
          5. [pending] Save the report to a file named 'ai_agents_researc...
    --------------------------------------------------------------------------------
    [TOOL: write_todos] Completed
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - internet_search: 'AI agent frameworks and tools 2025'
    --------------------------------------------------------------------------------
    [TOOL: internet_search] Completed
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - task(description=Research key differences between AI agent architectures in 2025. Focus on the architectural design, scalability, and efficiency of different AI agent systems. Provide a summary of the key differences and any notable trends or innovations in AI agent architectures., subagent_type=general-purpose)
    --------------------------------------------------------------------------------
    [TOOL: task] Completed
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - task(description=Research real-world applications and use cases of AI agents in 2025. Focus on industries such as healthcare, finance, retail, and logistics. Provide examples of how AI agents are being used to solve specific problems or improve processes in these industries., subagent_type=general-purpose)
    --------------------------------------------------------------------------------
    [TOOL: task] Completed
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - write_todos: Planning 5 tasks
          1. [completed] Research the main frameworks and tools being used ...
          2. [completed] Research key differences between AI agent architec...
          3. [completed] Research real-world applications and use cases of ...
          4. [in_progress] Compile research findings into a comprehensive rep...
          5. [pending] Save the report to a file named 'ai_agents_researc...
    --------------------------------------------------------------------------------
    [TOOL: write_todos] Completed
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - write_file: 
    --------------------------------------------------------------------------------
    [TOOL: write_file] Completed
    --------------------------------------------------------------------------------
    [AGENT] Calling 1 tool(s):
      - write_todos: Planning 5 tasks
          1. [completed] Research the main frameworks and tools being used ...
          2. [completed] Research key differences between AI agent architec...
          3. [completed] Research real-world applications and use cases of ...
          4. [completed] Compile research findings into a comprehensive rep...
          5. [completed] Save the report to a file named 'ai_agents_researc...
    --------------------------------------------------------------------------------
    [TOOL: write_todos] Completed
    --------------------------------------------------------------------------------
    [AGENT]: I have completed the research on the current state of AI agents in 2025. The findings have been compiled into a comprehensive report and saved to a file named [ai_agents_research_2025.txt](sandbox:/ai_agents_research_2025.txt). This report includes information on the main frameworks and tools, key differences between agent architectures, and real-world applications and use cases across various industries.
    --------------------------------------------------------------------------------
    
    ================================================================================
    Execution completed!
    ================================================================================

# Semantic Memory with LangMem

## Introduction

Semantic memory enables agents to remember and retrieve information by meaning across multiple conversation sessions. Unlike simple chat history which stores all messages sequentially, semantic memory uses vector embeddings to find and retrieve the most relevant memories based on the current context.

**Why is semantic memory useful?**
- **Personalization**: Remember user preferences, interests, and past interactions
- **Context persistence**: Recall relevant information from previous sessions
- **Efficient retrieval**: Use similarity search to find the most relevant memories instead of loading entire conversation history
- **Scalability**: Handle long-term interactions without overwhelming the context window

**What you'll learn:**
- How to set up a semantic memory store with embeddings
- How to create memory management tools that the agent can use
- How to build an agent that actively saves and retrieves memories
- How to inspect stored memories

**Prerequisites**: An OpenAI API key stored in a `.env` file

## Step 1: Environment Setup

Load environment variables to access your API key.


```python
from dotenv import load_dotenv

load_dotenv()
```




    True



## Step 2: Import Required Libraries

We'll need:
- **LangGraph**: For agent creation with `create_react_agent`
- **LangChain**: For model interaction
- **LangMem**: For memory management tools
- **InMemoryStore**: A simple in-memory vector store for this demo


```python
from langgraph.prebuilt import create_react_agent
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_core.tools import tool
from langmem import create_manage_memory_tool, create_search_memory_tool
from langgraph.store.memory import InMemoryStore
```

## Step 3: Set Up the Memory Store with Embeddings

The `InMemoryStore` will store our memories with vector embeddings enabled. This allows semantic search - finding memories based on meaning similarity rather than exact keyword matches.

**Key configuration:**
- `index`: Enables semantic search with the specified embedding model
- We use OpenAI's `text-embedding-3-small` model for efficient, high-quality embeddings


```python
# Create the memory store with semantic search capabilities
memory_store = InMemoryStore(
    index={
        "embed": OpenAIEmbeddings(model="text-embedding-3-small")
    }
)

print("Memory store initialized with semantic search enabled!")
```

    Memory store initialized with semantic search enabled!


## Step 4: Create Memory Management Tools

LangMem provides two essential tools that our agent can use:

1. **manage_memory_tool**: Allows the agent to save new memories
2. **search_memory_tool**: Allows the agent to search for relevant memories

These tools give the agent control over its own memory system - this is called the "hot path" approach where the agent actively decides what to remember.

**Key configuration:**
- We use a namespace `("memories",)` to organize where memories are stored
- The namespace helps isolate different types of memories


```python
# Create the memory management tool - allows agent to save memories
manage_memory_tool = create_manage_memory_tool(namespace=("memories",))

# Create the memory search tool - allows agent to search for memories
search_memory_tool = create_search_memory_tool(namespace=("memories",))

print("Memory tools created:")
print(f"- {manage_memory_tool.name}: {manage_memory_tool.description}")
print(f"- {search_memory_tool.name}: {search_memory_tool.description}")
```

    Memory tools created:
    - manage_memory: Create, update, or delete a memory to persist across conversations.
    Include the MEMORY ID when updating or deleting a MEMORY. Omit when creating a new MEMORY - it will be created for you.
    Proactively call this tool when you:
    
    1. Identify a new USER preference.
    2. Receive an explicit USER request to remember something or otherwise alter your behavior.
    3. Are working and want to record important context.
    4. Identify that an existing MEMORY is incorrect or outdated.
    - search_memory: Search your long-term memories for information relevant to your current context.


## Step 5: Create a Simple Weather Tool

Let's add a basic tool so our agent can demonstrate memory usage in context. The agent might remember user location preferences.


```python
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
        "new york": "Sunny, 72°F with light breeze",
        "london": "Cloudy, 59°F with occasional drizzle",
        "tokyo": "Clear, 68°F and pleasant",
        "san francisco": "Foggy, 63°F typical for this time of year",
        "paris": "Partly cloudy, 65°F and comfortable",
    }
    
    location_key = location.lower().strip()
    
    if location_key in weather_data:
        return f"Weather in {location}: {weather_data[location_key]}"
    else:
        return f"Weather data not available for {location}"
```

## Step 6: Configure the Language Model



```python
model = ChatOpenAI(
    model="gpt-5",
    temperature=0.1
)
```

## Step 7: Create the Agent with Memory Tools

Now we create the agent with all our tools: the weather tool and the two memory tools. The agent will have access to:
- `get_weather`: Check weather for locations
- `manage_memory`: Save important information to memory
- `search_memory`: Retrieve relevant memories

The agent can now actively decide when to save memories and when to search for them!


```python
# Combine all tools
tools = [get_weather, manage_memory_tool, search_memory_tool]

# Create the agent with LangGraph's create_react_agent
# Pass the store so the memory tools can access it
agent = create_react_agent(
    model=model,
    tools=tools,
    store=memory_store
)

print("Agent created successfully with memory capabilities!")
print(f"Available tools: {[tool.name for tool in tools]}")
```

    Agent created successfully with memory capabilities!
    Available tools: ['get_weather', 'manage_memory', 'search_memory']


    /var/folders/_1/tfp0k5355jxb501q_ckbc6m80000gn/T/ipykernel_87136/7600960.py:6: LangGraphDeprecatedSinceV10: create_react_agent has been moved to `langchain.agents`. Please update your import to `from langchain.agents import create_agent`. Deprecated in LangGraph V1.0 to be removed in V2.0.
      agent = create_react_agent(


## Step 8: Create a Helper Function for Interactions

This helper function will make it easy to interact with the agent. The agent will automatically handle memory storage and retrieval.


```python
def chat_with_agent(message: str, user_id: str = "user_123"):
    """Chat with the agent and see its response.
    
    Args:
        message: The user's message
        user_id: Identifier to namespace this user's memories (optional)
    """
    print(f"\n{'='*60}")
    print(f"USER: {message}")
    print(f"{'='*60}")
    
    # Invoke the agent with the message
    result = agent.invoke({
        "messages": [{"role": "user", "content": message}]
    })
    
    # Extract the final response
    final_message = result["messages"][-1]
    print(f"\nAGENT: {final_message.content}\n")
    
    return result
```

## Step 9: Example Interactions - Building Memory

Let's have a conversation where the agent learns about our preferences. The agent should use the `manage_memory` tool to save important information.


```python
# First interaction - tell the agent our preferences
chat_with_agent(
    "Hi! My name is Sarah and I live in San Francisco. I love checking the weather in the morning for SF so if I ask for the weather without mentioning a city, return the weather for SF."
)
```

    
    ============================================================
    USER: Hi! My name is Sarah and I live in San Francisco. I love checking the weather in the morning for SF so if I ask for the weather without mentioning a city, return the weather for SF.
    ============================================================
    
    AGENT: Got it, Sarah! I’ll use San Francisco as your default location for weather requests. If you ever want to change it, just let me know.
    





    {'messages': [HumanMessage(content='Hi! My name is Sarah and I live in San Francisco. I love checking the weather in the morning for SF so if I ask for the weather without mentioning a city, return the weather for SF.', additional_kwargs={}, response_metadata={}, id='3575ed48-3b98-46df-b409-7ceb0769b076'),
      AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 1039, 'prompt_tokens': 407, 'total_tokens': 1446, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 960, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CadtxMtJsxdT0C1VN5D4iYT35t4HD', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--fb4bbc8d-6500-4a3b-b252-5ef87fe534d1-0', tool_calls=[{'name': 'manage_memory', 'args': {'action': 'create', 'content': 'User prefers default weather location to be San Francisco when not specified.'}, 'id': 'call_HcUBVkSbveD7KizCQ8y3yusj', 'type': 'tool_call'}, {'name': 'manage_memory', 'args': {'action': 'create', 'content': "User's preferred name is Sarah."}, 'id': 'call_qaCRMXXWrZh73h0d4dwE5SWA', 'type': 'tool_call'}], usage_metadata={'input_tokens': 407, 'output_tokens': 1039, 'total_tokens': 1446, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 960}}),
      ToolMessage(content='created memory 6ea3f543-cd95-41f7-aae1-c1f154e9f220', name='manage_memory', id='ecba121d-5792-49b4-a957-dd4417bdf965', tool_call_id='call_HcUBVkSbveD7KizCQ8y3yusj'),
      ToolMessage(content='created memory 185f3485-8c15-4551-9b79-b6f9ed0972a7', name='manage_memory', id='27c796a7-3eaf-4e40-a146-f93786763117', tool_call_id='call_qaCRMXXWrZh73h0d4dwE5SWA'),
      AIMessage(content='Got it, Sarah! I’ll use San Francisco as your default location for weather requests. If you ever want to change it, just let me know.', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 34, 'prompt_tokens': 550, 'total_tokens': 584, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CaduKYLKls7frOg5QIcH49r0qQGRO', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None}, id='lc_run--bf9052da-35e9-4d46-afb5-7aaa77bc93b4-0', usage_metadata={'input_tokens': 550, 'output_tokens': 34, 'total_tokens': 584, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}




```python
# Tell the agent about preferences
chat_with_agent(
    "By the way, I prefer temperature in Celsis, not Fahrenheit."
)
```

    
    ============================================================
    USER: By the way, I prefer temperature in Celsis, not Fahrenheit.
    ============================================================
    
    AGENT: Got it—I'll use Celsius for temperatures going forward.
    





    {'messages': [HumanMessage(content='By the way, I prefer temperature in Celsis, not Fahrenheit.', additional_kwargs={}, response_metadata={}, id='5e66c371-e829-495e-970a-35c0ccb8d998'),
      AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 224, 'prompt_tokens': 381, 'total_tokens': 605, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 192, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CaduMPqKAwEPr5NHDkQTcAKLOuizg', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--990d7c30-4028-45d6-a370-4a0791f2204a-0', tool_calls=[{'name': 'manage_memory', 'args': {'action': 'create', 'content': 'User prefers temperature in Celsius.'}, 'id': 'call_Z3rWobjaTiAWxT00SSqYFnJs', 'type': 'tool_call'}], usage_metadata={'input_tokens': 381, 'output_tokens': 224, 'total_tokens': 605, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 192}}),
      ToolMessage(content='created memory 8383c6e6-0873-4aff-8711-06b6d10191e9', name='manage_memory', id='1df679dd-bdeb-47d7-9e1d-f0e6c9269a5f', tool_call_id='call_Z3rWobjaTiAWxT00SSqYFnJs'),
      AIMessage(content="Got it—I'll use Celsius for temperatures going forward.", additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 14, 'prompt_tokens': 445, 'total_tokens': 459, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 0, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CaduRyHpl5MxNEnGUZOccCNhFwSdb', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None}, id='lc_run--ef451611-ad91-4296-8920-8c63a9144446-0', usage_metadata={'input_tokens': 445, 'output_tokens': 14, 'total_tokens': 459, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 0}})]}



## Step 10: Testing Memory Retrieval

Now let's start a "new session" where the agent doesn't have the conversation history. It should use the `search_memory` tool to recall what it learned about us.


```python
# Ask about something specific that should be remembered
chat_with_agent(
    "What's my name and where do I live?"
)
```

    
    ============================================================
    USER: What's my name and where do I live?
    ============================================================
    
    AGENT: Your name is Sarah. I don’t know where you live. I do have San Francisco as your default weather location—do you live there, or would you like me to remember a different city?
    





    {'messages': [HumanMessage(content="What's my name and where do I live?", additional_kwargs={}, response_metadata={}, id='51a6e65b-3161-4163-9b38-56f55f7527ff'),
      AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 103, 'prompt_tokens': 375, 'total_tokens': 478, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 64, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CaduSm60Hx8f36QWV7oziK6pz0iPt', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--07b00b3f-8ca5-4e85-a9d6-6973991f31a0-0', tool_calls=[{'name': 'search_memory', 'args': {'query': 'user name OR my name OR called name OR I am', 'limit': 10}, 'id': 'call_ws6Yu4rPP0PTHWUS8P4qahYL', 'type': 'tool_call'}], usage_metadata={'input_tokens': 375, 'output_tokens': 103, 'total_tokens': 478, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 64}}),
      ToolMessage(content='[{"namespace":["memories"],"key":"185f3485-8c15-4551-9b79-b6f9ed0972a7","value":{"content":"User\'s preferred name is Sarah."},"created_at":"2025-11-11T08:20:35.308980+00:00","updated_at":"2025-11-11T08:20:35.308982+00:00","score":0.40283246798597305},{"namespace":["memories"],"key":"6ea3f543-cd95-41f7-aae1-c1f154e9f220","value":{"content":"User prefers default weather location to be San Francisco when not specified."},"created_at":"2025-11-11T08:20:35.308771+00:00","updated_at":"2025-11-11T08:20:35.308775+00:00","score":0.20766277515472673},{"namespace":["memories"],"key":"8383c6e6-0873-4aff-8711-06b6d10191e9","value":{"content":"User prefers temperature in Celsius."},"created_at":"2025-11-11T08:20:41.597545+00:00","updated_at":"2025-11-11T08:20:41.597558+00:00","score":0.20220983866321657}]', name='search_memory', id='dd7004d1-22c5-451f-885b-3122f791e504', tool_call_id='call_ws6Yu4rPP0PTHWUS8P4qahYL'),
      AIMessage(content='Your name is Sarah. I don’t know where you live. I do have San Francisco as your default weather location—do you live there, or would you like me to remember a different city?', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 369, 'prompt_tokens': 729, 'total_tokens': 1098, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 320, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CadubnvMyDYxrIPsrssexobOJuYET', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None}, id='lc_run--e2008b9d-255a-4bc9-9937-7c161f79ffa1-0', usage_metadata={'input_tokens': 729, 'output_tokens': 369, 'total_tokens': 1098, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 320}})]}




```python
chat_with_agent(
    "Hey, can you check the weather for me in the city where I live?"
)
```

    
    ============================================================
    USER: Hey, can you check the weather for me in the city where I live?
    ============================================================
    
    AGENT: Here’s the weather in your default city, San Francisco: Foggy, around 17°C (63°F), typical for this time of year.
    
    If you meant a different city, tell me which one and I’ll check it.
    





    {'messages': [HumanMessage(content='Hey, can you check the weather for me in the city where I live?', additional_kwargs={}, response_metadata={}, id='a0313f1f-30fe-43d3-93a4-59b5d0ce8d23'),
      AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 100, 'prompt_tokens': 382, 'total_tokens': 482, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 64, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-Cadujh2W8vcVDj8wZMzLWoQF4W3q3', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--c5feac09-9198-4582-955e-0b0263fa9b75-0', tool_calls=[{'name': 'search_memory', 'args': {'query': 'user home city OR location OR lives in', 'limit': 5}, 'id': 'call_SEO5gS1RtxFpXq6FTISjuS7i', 'type': 'tool_call'}], usage_metadata={'input_tokens': 382, 'output_tokens': 100, 'total_tokens': 482, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 64}}),
      ToolMessage(content='[{"namespace":["memories"],"key":"6ea3f543-cd95-41f7-aae1-c1f154e9f220","value":{"content":"User prefers default weather location to be San Francisco when not specified."},"created_at":"2025-11-11T08:20:35.308771+00:00","updated_at":"2025-11-11T08:20:35.308775+00:00","score":0.44269580673763054},{"namespace":["memories"],"key":"185f3485-8c15-4551-9b79-b6f9ed0972a7","value":{"content":"User\'s preferred name is Sarah."},"created_at":"2025-11-11T08:20:35.308980+00:00","updated_at":"2025-11-11T08:20:35.308982+00:00","score":0.27294081225113337},{"namespace":["memories"],"key":"8383c6e6-0873-4aff-8711-06b6d10191e9","value":{"content":"User prefers temperature in Celsius."},"created_at":"2025-11-11T08:20:41.597545+00:00","updated_at":"2025-11-11T08:20:41.597558+00:00","score":0.23902420730407603}]', name='search_memory', id='43035020-eebd-494c-ac46-47e4e28460b1', tool_call_id='call_SEO5gS1RtxFpXq6FTISjuS7i'),
      AIMessage(content='', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 280, 'prompt_tokens': 733, 'total_tokens': 1013, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 256, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-CaduwkwcvJ1346FOCLqcUEi9noEzO', 'service_tier': 'default', 'finish_reason': 'tool_calls', 'logprobs': None}, id='lc_run--a0f3f256-e3bf-43cf-9a56-3792154adaba-0', tool_calls=[{'name': 'get_weather', 'args': {'location': 'San Francisco'}, 'id': 'call_YlX1axwHxc6Z0Xc7Ejdog1yA', 'type': 'tool_call'}], usage_metadata={'input_tokens': 733, 'output_tokens': 280, 'total_tokens': 1013, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 256}}),
      ToolMessage(content='Weather in San Francisco: Foggy, 63°F typical for this time of year', name='get_weather', id='363941c0-bf96-415d-9870-bf04b0bd32ec', tool_call_id='call_YlX1axwHxc6Z0Xc7Ejdog1yA'),
      AIMessage(content='Here’s the weather in your default city, San Francisco: Foggy, around 17°C (63°F), typical for this time of year.\n\nIf you meant a different city, tell me which one and I’ll check it.', additional_kwargs={'refusal': None}, response_metadata={'token_usage': {'completion_tokens': 504, 'prompt_tokens': 779, 'total_tokens': 1283, 'completion_tokens_details': {'accepted_prediction_tokens': 0, 'audio_tokens': 0, 'reasoning_tokens': 448, 'rejected_prediction_tokens': 0}, 'prompt_tokens_details': {'audio_tokens': 0, 'cached_tokens': 0}}, 'model_provider': 'openai', 'model_name': 'gpt-5-2025-08-07', 'system_fingerprint': None, 'id': 'chatcmpl-Cadv2NDAEUcuc9jjoBK8qz4a9XdFQ', 'service_tier': 'default', 'finish_reason': 'stop', 'logprobs': None}, id='lc_run--c91d9b24-4b25-4a6f-9659-c0de99a7e20a-0', usage_metadata={'input_tokens': 779, 'output_tokens': 504, 'total_tokens': 1283, 'input_token_details': {'audio': 0, 'cache_read': 0}, 'output_token_details': {'audio': 0, 'reasoning': 448}})]}



## Step 11: Testing Semantic Search

Let's directly test the semantic search capability by searching for memories using similarity.


```python
def search_memories_by_query(query: str, limit: int = 3):
    """Search for memories using semantic similarity."""
    namespace = ("memories",)
    
    print(f"\nSearching for: '{query}'")
    print(f"{'='*60}\n")
    
    try:
        # Perform semantic search
        results = memory_store.search(namespace, query=query, limit=limit)
        
        if not results:
            print("No relevant memories found.")
            return
        
        for i, result in enumerate(results, 1):
            print(f"Result {i}:")
            print(f"  Content: {result.value.get('content', 'N/A')}")
            if hasattr(result, 'score'):
                print(f"  Similarity Score: {result.score:.4f}")
            print()
    except Exception as e:
        print(f"Search error: {e}")

# Test semantic search
search_memories_by_query("Where does the user live?")
search_memories_by_query("temperature preferences")
```

    
    Searching for: 'Where does the user live?'
    ============================================================
    
    Result 1:
      Content: User prefers default weather location to be San Francisco when not specified.
      Similarity Score: 0.4492
    
    Result 2:
      Content: User's preferred name is Sarah.
      Similarity Score: 0.3864
    
    Result 3:
      Content: User prefers temperature in Celsius.
      Similarity Score: 0.3443
    
    
    Searching for: 'temperature preferences'
    ============================================================
    
    Result 1:
      Content: User prefers temperature in Celsius.
      Similarity Score: 0.5130
    
    Result 2:
      Content: User prefers default weather location to be San Francisco when not specified.
      Similarity Score: 0.3685
    
    Result 3:
      Content: User's preferred name is Sarah.
      Similarity Score: 0.2751
    

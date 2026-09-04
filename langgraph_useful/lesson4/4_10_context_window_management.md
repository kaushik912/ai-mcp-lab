# Managing Context Window Limits in AI Agents

## Introduction

When building AI agents that engage in extended conversations, one of the most critical challenges is managing **context window limits**. Every language model has a maximum number of tokens it can process in a single request, which includes both the input (conversation history, system prompts, tool definitions) and the output.

### What are Context Windows?

A context window is the maximum amount of text (measured in tokens) that a language model can process at once. For example:

- GPT-3.5-turbo: 16,385 tokens
- GPT-4o-mini: 128,000 tokens
- GPT-4: 8,192 tokens (standard) or 128,000 tokens (turbo)

### Why Context Window Management Matters

1. **Performance**: Larger contexts take longer to process and increase latency
2. **Cost**: You pay per token, so unnecessary context directly increases costs
3. **Overflow**: Exceeding the limit causes errors and conversation failures
4. **Quality**: Too much irrelevant context can confuse the model and degrade response quality

### Two Core Strategies

In this notebook, we'll explore two fundamental approaches to managing context:

1. **Trimming**: Remove older messages to keep only recent conversation history
   - Fast and simple
   - No additional API calls
   - Loses information from removed messages

2. **Summarization**: Compress older messages into summaries while preserving key information
   - Retains important context
   - Requires additional API calls
   - More sophisticated but higher cost

### Learning Objectives

By the end of this notebook, you'll be able to:
- Understand when context window limits become a problem
- Implement message trimming using the `@before_model` decorator
- Implement conversation summarization using `SummarizationMiddleware`
- Choose the right strategy for your specific use case
- Combine both approaches for optimal results

## Setup

First, let's import the necessary libraries and configure our environment. We'll be using LangChain's `create_agent` function with middleware to manage context.

Key imports:
- `create_agent`: The main function for creating agents with middleware support
- `@before_model`: Decorator for running functions before model invocation
- `SummarizationMiddleware`: Pre-built middleware for automatic summarization
- `InMemorySaver`: Checkpoint storage for conversation persistence


```python
import os
from dotenv import load_dotenv
from typing import Any
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, RemoveMessage
from langgraph.graph.message import REMOVE_ALL_MESSAGES
from langgraph.checkpoint.memory import InMemorySaver
from langchain.agents import create_agent, AgentState
from langchain.agents.middleware import before_model
from langgraph.runtime import Runtime
from langchain_core.runnables import RunnableConfig

# Load environment variables
load_dotenv()

# Verify API key is loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment variables")

print("Setup complete! All required libraries imported successfully.")
```

    Setup complete! All required libraries imported successfully.


## Understanding Middleware in LangChain Agents

Before diving into context management strategies, let's understand **middleware** - a powerful pattern for modifying agent behavior.

### What is Middleware?

Middleware are functions that intercept and potentially modify the agent's execution at specific points. They allow you to:
- Transform state before it reaches the model
- Process outputs after the model responds
- Implement cross-cutting concerns like logging, monitoring, or context management

### Key Middleware Decorators

LangChain provides two main decorators:

1. **`@before_model`**: Runs before the model is invoked
   - Receives current `AgentState` and `Runtime`
   - Can modify messages, add/remove context, etc.
   - Return `dict` with changes or `None` to keep state unchanged

2. **`@after_model`**: Runs after the model responds
   - Receives the model's response
   - Can modify or process the output
   - Useful for logging, validation, post-processing

### The Return Pattern

Middleware functions follow a simple pattern:

```python
@before_model
def my_middleware(state: AgentState, runtime: Runtime) -> dict[str, Any] | None:
    # Check if modifications are needed
    if no_changes_needed:
        return None  # Keep state as-is
    
    # Make modifications
    modified_state = {...}
    return modified_state  # Apply changes
```

### Passing Middleware to Agents

Middleware is passed as a list to the `middleware` parameter:

```python
agent = create_agent(
    model=model,
    tools=[],
    middleware=[middleware_func1, middleware_func2],  # Applied in order
    checkpointer=InMemorySaver(),
)
```

## Strategy 1: Message Trimming

### When to Use Trimming

Message trimming is ideal when:
- You need a simple, fast solution with no additional API calls
- Recent conversation context is most important
- Older messages become irrelevant over time
- You want minimal computational overhead and cost
- Your use case is transactional (e.g., single-issue support)

### How Trimming Works

The trimming strategy:
1. Keeps the first message (often contains important context or instructions)
2. Removes middle messages when the count exceeds a threshold
3. Always keeps the most recent messages
4. Uses `RemoveMessage` with `REMOVE_ALL_MESSAGES` to efficiently clear old messages

### The Trade-off

**Advantage**: Fast, free (no extra API calls), simple to implement

**Disadvantage**: You permanently lose information from trimmed messages - the agent cannot recall anything from removed context

### Implementing Trimming with `@before_model`

Let's create middleware that automatically trims messages before each model call.


```python
@before_model
def trim_messages(state: AgentState, runtime: Runtime) -> dict[str, Any] | None:
    """
    Keep only the last few messages to fit within context window.
    
    Strategy:
    - Keep last 4 messages (most recent conversation)
    - Remove everything older
    
    Args:
        state: Current agent state containing messages
        runtime: Runtime object (not used in this example)
    
    Returns:
        Dict with modified messages, or None if no changes needed
    """
    messages = state["messages"]
    
    # If we have 4 or fewer messages, no trimming needed
    if len(messages) <= 4:
        return None  # Keep state unchanged
    
    # Keep only the last 4 messages (most recent context)
    recent_messages = messages[-4:]
    
    # Return the trimmed message list
    # Use RemoveMessage with REMOVE_ALL_MESSAGES to clear, then add back what we want
    return {
        "messages": [
            RemoveMessage(id=REMOVE_ALL_MESSAGES),
            *recent_messages
        ]
    }

# Create model instance
model = ChatOpenAI(model="gpt-4o")

# Create agent with trimming middleware
agent_trimming = create_agent(
    model=model,
    tools=[],  # No tools for this demonstration
    middleware=[trim_messages],  # Apply our trimming middleware
    checkpointer=InMemorySaver(),  # Enable conversation persistence
)

print("Agent with trimming middleware created successfully!")
print(f"This agent will keep at most 4 messages (last 4 turns) in memory.")
```

    Agent with trimming middleware created successfully!
    This agent will keep at most 4 messages (last 4 turns) in memory.


### Testing the Trimming Strategy

Let's simulate a long conversation about travel planning and observe how trimming affects the agent's memory.


```python
# Configuration with thread ID for conversation persistence
config: RunnableConfig = {"configurable": {"thread_id": "trimming_demo"}}

# Simulate a multi-turn conversation
conversation = [
    "Hi! My name is Sajal and I'm planning a trip to Japan.",
    "I'm interested in visiting Tokyo first.",
    "What are the must-see places in Tokyo?",
    "How about food recommendations?",
    "What's the best way to get around Tokyo?",
    "Should I get a JR Pass?",
]

print("Starting conversation with trimming agent...")
print("=" * 80)

for i, msg in enumerate(conversation, 1):
    result = agent_trimming.invoke(
        {"messages": [HumanMessage(content=msg)]}, 
        config
    )
    
    # Get the last AI message
    ai_response = result['messages'][-1].content
    
    print(f"\nTurn {i}:")
    print(f"User: {msg}")
    print(f"Agent: {ai_response[:100]}...")
    print("-" * 80)

print("\nConversation complete!")
```

    Starting conversation with trimming agent...
    ================================================================================
    
    Turn 1:
    User: Hi! My name is Sajal and I'm planning a trip to Japan.
    Agent: Hi Sajal! That sounds like an exciting trip. Japan is a fantastic destination with a rich culture, i...
    --------------------------------------------------------------------------------
    
    Turn 2:
    User: I'm interested in visiting Tokyo first.
    Agent: Great choice! Tokyo is a vibrant city with endless things to see and do. Here are some suggestions t...
    --------------------------------------------------------------------------------
    
    Turn 3:
    User: What are the must-see places in Tokyo?
    Agent: Tokyo is full of fascinating places to explore, and while each visitor may have different interests,...
    --------------------------------------------------------------------------------
    
    Turn 4:
    User: How about food recommendations?
    Agent: Tokyo is a culinary paradise with a diverse range of food options, from traditional Japanese cuisine...
    --------------------------------------------------------------------------------
    
    Turn 5:
    User: What's the best way to get around Tokyo?
    Agent: Tokyo boasts one of the world's most efficient and comprehensive public transportation systems, maki...
    --------------------------------------------------------------------------------
    
    Turn 6:
    User: Should I get a JR Pass?
    Agent: Whether or not you should get a Japan Rail (JR) Pass depends on your travel plans within Japan. The ...
    --------------------------------------------------------------------------------
    
    Conversation complete!


### Verifying Trimming Behavior

Now let's examine what messages are actually stored in memory after trimming has been applied.


```python
# Get the current state to inspect memory
state = agent_trimming.get_state(config)
messages = state.values["messages"]

print(f"Total messages in memory: {len(messages)}")
print(f"\nMessage breakdown:")

for i, msg in enumerate(messages, 1):
    msg_type = msg.__class__.__name__
    content_preview = msg.content[:70] if len(msg.content) > 70 else msg.content
    print(f"  {i}. {msg_type}: {content_preview}...")

print(f"\nAs expected, we should see 5 messages in the state, the 4 messages after trimming BEFORE we invoked the agent one more time, and the latest agent response.")
```

    Total messages in memory: 5
    
    Message breakdown:
      1. AIMessage: Tokyo is a culinary paradise with a diverse range of food options, fro...
      2. HumanMessage: What's the best way to get around Tokyo?...
      3. AIMessage: Tokyo boasts one of the world's most efficient and comprehensive publi...
      4. HumanMessage: Should I get a JR Pass?...
      5. AIMessage: Whether or not you should get a Japan Rail (JR) Pass depends on your t...
    
    As expected, we should see 5 messages in the state, the 4 messages after trimming BEFORE we invoked the agent one more time, and the latest agent response.


### Testing Information Loss

The key limitation of trimming: information from removed messages is permanently lost. Let's test this by asking about something mentioned early in the conversation.


```python
# Ask about the first message (likely trimmed if conversation was long enough)
result = agent_trimming.invoke(
    {"messages": [HumanMessage(content="What's my name?")]},
    config
)

print("Testing recall of early conversation...")
print(f"\nUser: What's my name?")
print(f"\nAgent: {result['messages'][-1].content}")

print("\n" + "=" * 80)
print("OBSERVATION:")
print("If the early messages were trimmed, the agent may not the name was mentioned.")
print("This demonstrates the information loss inherent in the trimming strategy.")
print("=" * 80)
```

    Testing recall of early conversation...
    
    User: What's my name?
    
    Agent: I'm sorry, but I don't have access to personal data about users unless it's shared with me in the course of our conversation. That includes your name or any other personal details. If there's anything else you'd like to know or discuss, feel free to ask!
    
    ================================================================================
    OBSERVATION:
    If the early messages were trimmed, the agent may not the name was mentioned.
    This demonstrates the information loss inherent in the trimming strategy.
    ================================================================================


## Strategy 2: Conversation Summarization

### When to Use Summarization

Summarization is better when:
- You need to preserve information from earlier in the conversation
- The conversation contains important context that spans many turns
- You're willing to trade additional API costs for better memory
- The agent needs to reference details from throughout the entire conversation
- Your use case involves complex, long-running interactions (planning, tutoring, analysis)

### How Summarization Works

The summarization strategy:
1. Monitors the total token count of conversation history
2. When tokens exceed a threshold, triggers summarization
3. Uses the LLM to create a concise summary of older messages
4. Replaces old messages with the summary
5. Keeps recent messages in full detail for immediate context

### The Trade-off

**Advantage**: Preserves key information from the entire conversation history

**Disadvantage**: Each summarization requires an additional LLM API call, increasing cost and latency

### Implementing Summarization with `SummarizationMiddleware`

LangChain provides a pre-built `SummarizationMiddleware` class that handles all the complexity for us. Let's use it to create an agent with automatic summarization.


```python
from langchain.agents.middleware import SummarizationMiddleware

# Create agent with built-in summarization middleware
agent_summary = create_agent(
    model="gpt-4o-mini",  # Can pass model name directly
    tools=[],
    middleware=[
        SummarizationMiddleware(
            model="gpt-4o-mini",  # Model used for generating summaries
            max_tokens_before_summary=500,  # Trigger summary after 500 tokens
            messages_to_keep=4,  # Keep last 4 messages in full
        )
    ],
    checkpointer=InMemorySaver(),
)

print("Agent with summarization middleware created successfully!")
print(f"\nConfiguration:")
print(f"  - Summarization triggers after: 500 tokens")
print(f"  - Recent messages kept in full: 4")
print(f"  - Older messages will be: Summarized (not deleted)")
```

    Agent with summarization middleware created successfully!
    
    Configuration:
      - Summarization triggers after: 500 tokens
      - Recent messages kept in full: 4
      - Older messages will be: Summarized (not deleted)


### Testing the Summarization Strategy

Let's run the same conversation with the summarization agent and compare the results.


```python
# Use a different thread ID for this test
config_summary: RunnableConfig = {"configurable": {"thread_id": "summary_demo"}}

print("Starting conversation with summarization agent...")
print("=" * 80)

# Run the same conversation
for i, msg in enumerate(conversation, 1):
    result = agent_summary.invoke(
        {"messages": [HumanMessage(content=msg)]},
        config_summary
    )
    
    ai_response = result['messages'][-1].content
    
    print(f"\nTurn {i}:")
    print(f"User: {msg}")
    print(f"Agent: {ai_response[:100]}...")
    print("-" * 80)

print("\nConversation complete!")
```

    Starting conversation with summarization agent...
    ================================================================================
    
    Turn 1:
    User: Hi! My name is Sajal and I'm planning a trip to Japan.
    Agent: Hi Sajal! That sounds exciting! Japan is a beautiful country with a rich culture, stunning landscape...
    --------------------------------------------------------------------------------
    
    Turn 2:
    User: I'm interested in visiting Tokyo first.
    Agent: Great choice! Tokyo is a vibrant city with a mix of traditional and modern attractions. Here are som...
    --------------------------------------------------------------------------------
    
    Turn 3:
    User: What are the must-see places in Tokyo?
    Agent: Tokyo is full of incredible sights and experiences! Here’s a list of must-see places you should cons...
    --------------------------------------------------------------------------------
    
    Turn 4:
    User: How about food recommendations?
    Agent: Tokyo is a food lover's paradise, offering a wide variety of delicious cuisines and dining experienc...
    --------------------------------------------------------------------------------
    
    Turn 5:
    User: What's the best way to get around Tokyo?
    Agent: Getting around Tokyo is convenient and efficient, thanks to its extensive public transportation syst...
    --------------------------------------------------------------------------------
    
    Turn 6:
    User: Should I get a JR Pass?
    Agent: Deciding whether to get a Japan Rail (JR) Pass depends on your travel plans in Japan, including how ...
    --------------------------------------------------------------------------------
    
    Conversation complete!


### Verifying Summarization Behavior

Let's examine what's stored in memory with the summarization strategy. We should see a summary message plus recent messages.


```python
# Get the current state
state = agent_summary.get_state(config_summary)
messages = state.values["messages"]
print(f"Total messages in memory: {len(messages)}")
print(f"\nMessage structure:")
for i, msg in enumerate(messages, 1):
    msg_type = msg.__class__.__name__
    
    # Check if this is a summary message
    is_summary = "summary" in msg.content.lower()[:100] or msg_type == "SystemMessage"
    
    if is_summary:
        # Print summary in full
        print(f"\n  {i}. {msg_type} [SUMMARY]:")
        print(f"     {msg.content}\n\n")
    else:
        # Print preview for other messages
        content_preview = msg.content[:80] if len(msg.content) > 200 else msg.content
        print(f"  {i}. {msg_type}: {content_preview}{'...' if len(msg.content) > 200 else ''}")

print(f"\nNote: Look for a summary message that condenses older conversation turns.")
```

    Total messages in memory: 6
    
    Message structure:
    
      1. HumanMessage [SUMMARY]:
         Here is a summary of the conversation to date:
    
    Sajal is planning a trip to Japan, starting with a visit to Tokyo. Key attractions in Tokyo include Senso-ji Temple, Tokyo Skytree, Shibuya Crossing, Meiji Shrine, Harajuku, Akihabara, Ginza, Shinjuku Gyoen National Garden, Tokyo National Museum (Ueno Park), Odaiba, Tsukiji Outer Market, Imperial Palace, Roppongi Hills, and Yanaka District. Sajal is also interested in food recommendations.
    
    
      2. AIMessage: Tokyo is a food lover's paradise, offering a wide variety of delicious cuisines ...
      3. HumanMessage: What's the best way to get around Tokyo?
      4. AIMessage: Getting around Tokyo is convenient and efficient, thanks to its extensive public...
      5. HumanMessage: Should I get a JR Pass?
      6. AIMessage: Deciding whether to get a Japan Rail (JR) Pass depends on your travel plans in J...
    
    Note: Look for a summary message that condenses older conversation turns.


### Testing Information Retention

Now let's test whether the summarization approach successfully preserves information from early in the conversation.


```python
# Ask the same recall question
result = agent_summary.invoke(
    {"messages": [HumanMessage(content="What's my name?")]},
    config_summary
)

print("Testing recall of early conversation...")
print(f"\nUser: What's my name?")
print(f"\nAgent: {result['messages'][-1].content}")

print("\n" + "=" * 80)
print("OBSERVATION:")
print("With summarization, the agent should be able to recall our name.")
print("This demonstrates how summarization retains important context over time.")
print("=" * 80)
```

    Testing recall of early conversation...
    
    User: What's my name?
    
    Agent: Your name is Sajal. If you have any other questions or need further assistance with your trip to Japan, feel free to ask!
    
    ================================================================================
    OBSERVATION:
    With summarization, the agent should be able to recall our name.
    This demonstrates how summarization retains important context over time.
    ================================================================================



```python

```
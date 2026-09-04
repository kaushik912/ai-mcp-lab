# Fine-Tuning Agent Behavior with Feedback Using Meta-Prompting

## Introduction

In this tutorial, you'll learn how to use **meta-prompting** to automatically improve your agent's behavior based on user feedback. Meta-prompting is a powerful technique where one LLM analyzes the performance of another LLM and generates improved instructions for it.

### What is Meta-Prompting?

Meta-prompting is an instruction-tuning approach that creates a feedback-driven improvement loop:

```
┌─────────────────────────────────────────────────────────┐
│                   Improvement Cycle                      │
└─────────────────────────────────────────────────────────┘

    1. Run Agent                2. Collect Feedback
    ┌──────────┐                ┌──────────────┐
    │  Agent   │                │ User rates   │
    │ v1.0     │───────────────▶│ response &   │
    │ (prompt) │                │ provides     │
    └──────────┘                │ comments     │
         ▲                      └──────────────┘
         │                             │
         │                             ▼
    5. Deploy                  3. Analyze & Generate
    Improved Agent             ┌──────────────────┐
    ┌──────────┐               │  Meta-Prompt LLM │
    │  Agent   │               │  analyzes issues │
    │ v2.0     │◀──────────────│  & writes better │
    │ (better) │               │  system prompt   │
    └──────────┘               └──────────────────┘
                                      │
                                      ▼
                              4. Test & Validate
                              ┌──────────────────┐
                              │ Compare old vs   │
                              │ new performance  │
                              └──────────────────┘
```

### Key Concepts Covered

- **Automated Improvement**: Using an LLM to generate better prompts
- **Version Tracking**: Maintaining a history of prompt iterations
- **Feedback-Driven Refinement**: Systematic improvement based on user input
- **Iterative Development**: Multiple cycles leading to progressively better agents

### Prerequisites

- OpenAI API key stored in a `.env` file
- Basic understanding of LangChain agents
- Familiarity with the weather agent from Level 2

### Learning Objectives

By the end of this tutorial, you will:
1. Understand how meta-prompting enables automated prompt improvement
2. Implement a complete feedback collection and analysis system
3. Create a version management system for tracking prompt evolution
4. Build a feedback loop that continuously improves agent behavior
5. Compare and validate improvements across prompt versions

## 1. Setup and Configuration

First, let's import all necessary libraries and load our environment variables.


```python
import os
import json
from datetime import datetime
from typing import Dict, List, Any

from dotenv import load_dotenv
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

# Load environment variables
load_dotenv()

print("✓ Environment configured successfully")
```

    ✓ Environment configured successfully


## 2. Create Base Weather Tool

We'll use the same weather tool from Level 2 as our example agent. This tool provides mock weather data for demonstration purposes.


```python
@tool
def get_weather(location: str) -> str:
    """Get the current weather for a location.
    
    Args:
        location: The city name to get weather for
        
    Returns:
        A string describing the current weather conditions
    """
    # Mock weather data
    weather_data = {
        "new york": "Sunny, 72°F (22°C), humidity 45%",
        "london": "Cloudy, 59°F (15°C), light rain expected",
        "tokyo": "Clear, 68°F (20°C), humidity 60%",
        "paris": "Partly cloudy, 65°F (18°C), gentle breeze",
        "sydney": "Sunny, 75°F (24°C), perfect beach weather"
    }
    
    # Normalize location
    normalized_location = location.lower().strip()
    
    # Return weather or helpful message
    if normalized_location in weather_data:
        return weather_data[normalized_location]
    else:
        return f"Weather data not available for {location}. Try: New York, London, Tokyo, Paris, or Sydney."

print("✓ Weather tool created")
```

    ✓ Weather tool created


## 3. Initialize Prompt Version Management System

Before creating our agent, let's set up a system to track different versions of our system prompts. This is crucial for:
- Understanding how prompts evolve over time
- Rolling back to previous versions if needed
- Analyzing which changes led to improvements

💡 **Key Concept**: Version tracking isn't just about storing text - it's about maintaining context around *why* each change was made and *how* it performed.


```python
# File to store prompt versions
VERSIONS_FILE = "prompt_versions.json"

def initialize_version_system(initial_prompt: str) -> Dict[str, Any]:
    """Initialize the prompt version tracking system.
    
    Args:
        initial_prompt: The starting system prompt
        
    Returns:
        Dictionary containing version 1.0 of the prompt
    """
    versions = {
        "v1.0": {
            "prompt": initial_prompt,
            "timestamp": datetime.now().isoformat(),
            "feedback_summary": "Initial version - baseline prompt",
            "avg_rating": None,
            "num_interactions": 0,
            "feedback_items": []
        }
    }
    return versions

def save_versions(versions: Dict[str, Any]):
    """Save prompt versions to JSON file."""
    with open(VERSIONS_FILE, 'w') as f:
        json.dump(versions, f, indent=2)
    print(f"✓ Versions saved to {VERSIONS_FILE}")

def load_versions() -> Dict[str, Any]:
    """Load prompt versions from JSON file."""
    if os.path.exists(VERSIONS_FILE):
        with open(VERSIONS_FILE, 'r') as f:
            return json.load(f)
    return {}

def get_latest_version(versions: Dict[str, Any]) -> tuple[str, Dict[str, Any]]:
    """Get the most recent prompt version.
    
    Returns:
        Tuple of (version_key, version_data)
    """
    latest_key = sorted(versions.keys())[-1]
    return latest_key, versions[latest_key]

print("✓ Version management system ready")
```

    ✓ Version management system ready


## 4. Create Base Weather Agent (Version 1.0)

Let's start with a simple, basic system prompt. This will be our baseline - intentionally minimal so we can see clear improvements through the meta-prompting process.

⚠️ **Important**: Starting with a simple prompt helps demonstrate the improvement process. In production, you'd start with a more thoughtful initial prompt.


```python
# Initial system prompt (intentionally basic)
INITIAL_SYSTEM_PROMPT = "You are a helpful weather assistant."

# Initialize version tracking
prompt_versions = initialize_version_system(INITIAL_SYSTEM_PROMPT)
save_versions(prompt_versions)

print("Initial System Prompt (v1.0):")
print("="*60)
print(INITIAL_SYSTEM_PROMPT)
print("="*60)
```

    ✓ Versions saved to prompt_versions.json
    Initial System Prompt (v1.0):
    ============================================================
    You are a helpful weather assistant.
    ============================================================



```python
def create_weather_agent(system_prompt: str):
    """Create a weather agent with a specific system prompt.
    
    Args:
        system_prompt: The system instructions for the agent
        
    Returns:
        Configured LangChain agent
    """
    # Create LLM with system prompt
    model = ChatOpenAI(
        model="gpt-4o",
        temperature=0.1
    )
    
    # Note: We'll add the system prompt via messages in invoke
    # Create agent with tools
    agent = create_agent(
        model=model,
        tools=[get_weather]
    )
    
    return agent, system_prompt

# Create initial agent
agent_v1, system_prompt_v1 = create_weather_agent(INITIAL_SYSTEM_PROMPT)
print("✓ Weather agent v1.0 created")
```

    ✓ Weather agent v1.0 created



```python
def ask_agent(agent, system_prompt: str, question: str) -> str:
    """Ask the agent a question and return its response.
    
    Args:
        agent: The LangChain agent
        system_prompt: System instructions for the agent
        question: User's question
        
    Returns:
        Agent's response text
    """
    result = agent.invoke({
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": question}
        ]
    })
    
    # Extract final message
    final_message = result['messages'][-1]
    return final_message.content

print("✓ Helper function ready")
```

    ✓ Helper function ready


### Test the Initial Agent

Let's see how our basic agent performs with a simple weather query.


```python
test_query = "What's the weather in London?"

print(f"User: {test_query}\n")
response_v1 = ask_agent(agent_v1, system_prompt_v1, test_query)
print(f"Agent v1.0: {response_v1}")
```

    User: What's the weather in London?
    
    Agent v1.0: The current weather in London is cloudy with a temperature of 59°F (15°C). Light rain is expected.


## 5. Collect User Feedback

Feedback is the foundation of improvement. We'll collect both quantitative (rating) and qualitative (comments) feedback.

💡 **Key Concept**: Structured feedback (rating + specific comments) is more actionable than vague feedback like "it's okay". The meta-prompt LLM needs concrete information about what went wrong and why.


```python
def collect_feedback(query: str, response: str) -> Dict[str, Any]:
    """Collect user feedback on an agent response.
    
    Args:
        query: The user's original question
        response: The agent's response
        
    Returns:
        Dictionary containing feedback data
    """
    print("\n" + "="*60)
    print("FEEDBACK COLLECTION")
    print("="*60)
    print(f"\nQuery: {query}")
    print(f"Response: {response}\n")
    
    # For tutorial purposes, we'll use hardcoded feedback
    # In a real application, you would use input() or a UI form
    
    # Simulated user feedback for the basic agent
    rating = 2  # Out of 5
    comments = (
        "The response is too brief and doesn't provide context. "
        "I'd like to know what to wear or if I need an umbrella. "
        "Also, it would be nice if the agent was more conversational and friendly."
    )
    
    print(f"Rating: {rating}/5")
    print(f"Comments: {comments}")
    
    feedback = {
        "query": query,
        "response": response,
        "rating": rating,
        "comments": comments,
        "timestamp": datetime.now().isoformat()
    }
    
    return feedback

# Collect feedback on our test interaction
feedback_v1 = collect_feedback(test_query, response_v1)

print("\n✓ Feedback collected")
```

    
    ============================================================
    FEEDBACK COLLECTION
    ============================================================
    
    Query: What's the weather in London?
    Response: The current weather in London is cloudy with a temperature of 59°F (15°C). Light rain is expected.
    
    Rating: 2/5
    Comments: The response is too brief and doesn't provide context. I'd like to know what to wear or if I need an umbrella. Also, it would be nice if the agent was more conversational and friendly.
    
    ✓ Feedback collected


## 6. Meta-Prompting: Generate Improved System Prompt

Now for the magic! We'll use a separate LLM instance (the "meta-prompt LLM") to analyze the feedback and generate an improved system prompt.

### How Meta-Prompting Works

The meta-prompt LLM receives:
1. **Current system prompt** - What instructions the agent currently follows
2. **User query** - What the user asked
3. **Agent response** - What the agent said
4. **User feedback** - Rating and specific complaints/suggestions

Based on this analysis, it generates a **new system prompt** that addresses the issues.

💡 **Key Concept**: The meta-prompt LLM is a prompt engineer. It understands what makes good prompts and can write better instructions based on observed failures.


```python
def create_meta_prompt(current_prompt: str, feedback: Dict[str, Any]) -> str:
    """Create a meta-prompt for analyzing and improving the system prompt.
    
    Args:
        current_prompt: The current system prompt being used
        feedback: Dictionary containing query, response, rating, and comments
        
    Returns:
        Meta-prompt string for the optimization LLM
    """
    meta_prompt = f"""You are an expert prompt engineer specializing in optimizing AI agent behavior.

Your task is to analyze a user interaction with an AI agent and generate an improved system prompt that addresses the user's concerns.

CURRENT SYSTEM PROMPT:
{current_prompt}

INTERACTION DETAILS:
User Query: {feedback['query']}
Agent Response: {feedback['response']}
User Rating: {feedback['rating']}/5
User Feedback: {feedback['comments']}

ANALYSIS INSTRUCTIONS:
1. Identify specific issues mentioned in the user feedback
2. Determine what the current prompt is missing or doing wrong
3. Consider what instructions would lead to better responses
4. Generate a new system prompt that:
   - Addresses all user concerns
   - Maintains the agent's core purpose
   - Provides clear, actionable instructions
   - Specifies desired tone, style, and behavior

IMPORTANT: Output ONLY the new system prompt, nothing else. Do not include explanations, markdown formatting, or meta-commentary. Just the raw prompt text that will be used as the new system instruction.
"""
    return meta_prompt

print("✓ Meta-prompt template ready")
```

    ✓ Meta-prompt template ready



```python
def generate_improved_prompt(current_prompt: str, feedback: Dict[str, Any]) -> str:
    """Use meta-prompting to generate an improved system prompt.
    
    Args:
        current_prompt: The current system prompt
        feedback: User feedback dictionary
        
    Returns:
        New improved system prompt
    """
    # Create meta-prompt
    meta_prompt = create_meta_prompt(current_prompt, feedback)
    
    # Use a separate LLM instance for meta-prompting
    # We use higher temperature for creative prompt engineering
    meta_llm = ChatOpenAI(
        model="gpt-4o",
        temperature=0.7
    )
    
    # Generate improved prompt
    response = meta_llm.invoke(meta_prompt)
    improved_prompt = response.content.strip()
    
    return improved_prompt

print("✓ Prompt improvement function ready")
```

    ✓ Prompt improvement function ready


### Generate Version 2.0 of the System Prompt

Let's run the meta-prompting process to create an improved prompt based on our user feedback.


```python
print("Analyzing feedback and generating improved prompt...\n")

improved_prompt = generate_improved_prompt(system_prompt_v1, feedback_v1)

print("\n" + "="*60)
print("PROMPT COMPARISON")
print("="*60)

print("\n📋 VERSION 1.0 (Original):")
print("-" * 60)
print(system_prompt_v1)

print("\n\n📋 VERSION 2.0 (Improved):")
print("-" * 60)
print(improved_prompt)
print("="*60)

print("\n✓ Improved prompt generated successfully")
```

    Analyzing feedback and generating improved prompt...
    
    
    ============================================================
    PROMPT COMPARISON
    ============================================================
    
    📋 VERSION 1.0 (Original):
    ------------------------------------------------------------
    You are a helpful weather assistant.
    
    
    📋 VERSION 2.0 (Improved):
    ------------------------------------------------------------
    You are a friendly and conversational weather assistant. Provide detailed weather updates that include current conditions, temperature, and precipitation expectations. Offer practical advice on what to wear and whether an umbrella is needed. Ensure your responses are engaging and relatable, adding context to help users plan their day.
    ============================================================
    
    ✓ Improved prompt generated successfully


## 7. Save New Version and Update Tracking

Now that we have an improved prompt, let's save it to our version management system.


```python
def add_new_version(versions: Dict[str, Any], 
                   new_prompt: str, 
                   feedback: Dict[str, Any]) -> str:
    """Add a new prompt version to the tracking system.
    
    Args:
        versions: Current versions dictionary
        new_prompt: The improved system prompt
        feedback: Feedback that led to this improvement
        
    Returns:
        Version key for the new version
    """
    # Generate new version number
    version_numbers = [float(v.replace('v', '')) for v in versions.keys()]
    new_version_num = max(version_numbers) + 1.0
    new_version_key = f"v{new_version_num}"
    
    # Create summary of why this version was created
    feedback_summary = (
        f"Improved based on rating {feedback['rating']}/5. "
        f"User requested: {feedback['comments'][:100]}..."
    )
    
    # Add new version
    versions[new_version_key] = {
        "prompt": new_prompt,
        "timestamp": datetime.now().isoformat(),
        "feedback_summary": feedback_summary,
        "avg_rating": None,
        "num_interactions": 0,
        "feedback_items": [],
        "previous_version": sorted(versions.keys())[-1]  # Link to previous
    }
    
    return new_version_key

# Add v2.0 to our version tracking
new_version = add_new_version(prompt_versions, improved_prompt, feedback_v1)
save_versions(prompt_versions)

print(f"✓ Version {new_version} saved to tracking system")
print(f"✓ Total versions: {len(prompt_versions)}")
```

    ✓ Versions saved to prompt_versions.json
    ✓ Version v2.0 saved to tracking system
    ✓ Total versions: 2


## 8. Test Improved Agent (Version 2.0)

Let's create a new agent with the improved prompt and test it with the same query to see the difference.


```python
# Create agent with improved prompt
agent_v2, system_prompt_v2 = create_weather_agent(improved_prompt)

print("✓ Weather agent v2.0 created with improved prompt\n")

# Test with same query
print(f"User: {test_query}\n")
response_v2 = ask_agent(agent_v2, system_prompt_v2, test_query)
print(f"Agent v2.0: {response_v2}")
```

    ✓ Weather agent v2.0 created with improved prompt
    
    User: What's the weather in London?
    
    Agent v2.0: In London, it's currently cloudy with a temperature of 59°F (15°C). There's light rain expected, so it might be a good idea to have an umbrella handy if you're heading out. With the cool and damp conditions, wearing a light jacket or a sweater would be comfortable. Enjoy your day, and don't forget to stay dry!

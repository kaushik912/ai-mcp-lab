# Tutorial: Conversation Threading and Memory Persistence in LangGraph

## Learning Objectives

By the end of this tutorial, you will be able to:
- Understand what conversation threading means and why it's important
- Use checkpointers to persist conversation state across invocations
- Implement thread_id to maintain separate conversation histories
- Build a chatbot with memory using SQLite persistence
- Manage multiple user conversations with proper isolation

## Prerequisites

- Basic understanding of LangGraph workflows
- Python programming fundamentals
- Familiarity with LangChain message types
- OpenAI API key

## What is Conversation Threading?

**Conversation threading** enables a chatbot to remember previous interactions within a conversation. Without threading, each message to the bot starts fresh with no memory of what was said before.

### Why Threading Matters:

**Without Threading:**
```
User: My name is Sarah
Bot: Nice to meet you, Sarah!

User: What's my name?
Bot: I don't know your name.
```

**With Threading:**
```
User: My name is Sarah
Bot: Nice to meet you, Sarah!

User: What's my name?
Bot: Your name is Sarah!
```

### Key Concepts:

| Concept | Description |
|---------|-------------|
| **Checkpointer** | A component that saves and loads conversation state |
| **thread_id** | A unique identifier for a conversation thread |
| **State Persistence** | Saving conversation history between invocations |
| **Thread Isolation** | Keeping different conversations separate |

### Use Cases:
- **Customer support bots** that remember context throughout a support session
- **Personal assistants** that maintain conversation history
- **Multi-user applications** where each user has their own conversation thread
- **Long-running conversations** that span multiple sessions

## Setup

Let's start by importing the necessary libraries and loading environment variables.


```python
# Install required packages if needed
# !pip install langgraph langchain-openai python-dotenv
```


```python
import os
import sqlite3
from dotenv import load_dotenv
from typing import TypedDict, Annotated
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, BaseMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.sqlite import SqliteSaver

# Load environment variables
load_dotenv()

# Verify OpenAI API key is loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment variables")

print("Environment loaded successfully!")
print("All required libraries imported!")
```

    Environment loaded successfully!
    All required libraries imported!


## Understanding Checkpointers and Persistence

A **checkpointer** is responsible for:
1. Saving the state after each step in the graph
2. Loading the state when you invoke the graph again
3. Managing multiple conversation threads

### SQLite for Learning and Production

In this tutorial, we'll use **SqliteSaver** which stores conversation state in a SQLite database file.

**Why SQLite?**
- **Persistent storage**: Conversations survive program restarts
- **No setup required**: File-based database with zero configuration
- **Perfect for learning**: Simple to understand and use
- **Production-ready**: Suitable for many real-world applications

### Production Database Options

While we use SQLite in this tutorial for simplicity, LangGraph supports several database backends for production scenarios:

| Database | Use Case | LangGraph Class |
|----------|----------|-----------------|
| **SQLite** | Single-server apps, development, learning | `SqliteSaver` |
| **PostgreSQL** | High-traffic production apps, distributed systems | `PostgresSaver` |
| **MongoDB** | Document-based storage, flexible schemas | `MongoDBSaver` |
| **Redis** | Ultra-fast access, caching, real-time apps | `RedisSaver` |

**For this tutorial, we'll focus on SQLite**, but the concepts and patterns you learn here apply to all checkpoint implementations.

## Building a Basic Chatbot (Without Memory)

Let's first build a simple chatbot without any persistence to see the problem we're solving.

### Step 1: Define the State

Our state will contain a list of messages. We use the special `add_messages` reducer to append new messages to the list rather than replacing them.


```python
class ChatState(TypedDict):
    """
    State structure for our chatbot.
    
    The Annotated type with add_messages tells LangGraph to append new messages
    to the messages list rather than replacing the entire list.
    """
    messages: Annotated[list[BaseMessage], add_messages]

print("State defined successfully!")
```

    State defined successfully!


### Step 2: Create the Chatbot Node

This node will call the LLM with the current messages and return the response.


```python
def chatbot_node(state: ChatState) -> ChatState:
    """
    The main chatbot node that processes messages using OpenAI.
    """
    # Initialize the LLM
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
    
    # Get the response from the LLM
    response = llm.invoke(state["messages"])
    
    # Return the updated state with the new message
    # The add_messages reducer will append this to the existing messages
    return {"messages": [response]}

print("Chatbot node created successfully!")
```

    Chatbot node created successfully!


### Step 3: Build the Graph (Without Checkpointer)

Notice we compile the graph WITHOUT a checkpointer. This means it has no memory.


```python
# Create the workflow
workflow_no_memory = StateGraph(ChatState)

# Add the chatbot node
workflow_no_memory.add_node("chatbot", chatbot_node)

# Add edges
workflow_no_memory.add_edge(START, "chatbot")
workflow_no_memory.add_edge("chatbot", END)

# Compile WITHOUT a checkpointer - no memory!
app_no_memory = workflow_no_memory.compile()

print("Chatbot without memory compiled successfully!")
```

    Chatbot without memory compiled successfully!


### Step 4: Test Without Memory

Let's see what happens when we try to have a conversation without memory.


```python
print("=" * 60)
print("DEMONSTRATION: Chatbot WITHOUT Memory")
print("=" * 60)

# First message
print("\nUser: My name is Alice")
result1 = app_no_memory.invoke({
    "messages": [HumanMessage(content="My name is Alice")]
})
print(f"Bot: {result1['messages'][-1].content}")

# Second message - trying to reference the first
print("\nUser: What's my name?")
result2 = app_no_memory.invoke({
    "messages": [HumanMessage(content="What's my name?")]
})
print(f"Bot: {result2['messages'][-1].content}")

print("\n" + "=" * 60)
print("Notice: The bot doesn't remember the previous conversation!")
print("=" * 60)
```

    ============================================================
    DEMONSTRATION: Chatbot WITHOUT Memory
    ============================================================
    
    User: My name is Alice
    Bot: Nice to meet you, Alice! How can I assist you today?
    
    User: What's my name?
    Bot: I'm sorry, but I don't have access to personal information about you unless you've shared it with me in this conversation. How can I assist you today?
    
    ============================================================
    Notice: The bot doesn't remember the previous conversation!
    ============================================================


## Adding Memory with SqliteSaver

Now let's add persistent memory to our chatbot using the `SqliteSaver` checkpointer. This will allow the bot to remember conversations even after restarting the program.

### Understanding thread_id

The `thread_id` is a unique identifier for a conversation thread. Think of it as a "conversation ID":

- **Same thread_id** = Same conversation (bot remembers previous messages)
- **Different thread_id** = Different conversation (fresh start)

The thread_id is passed in the config parameter:
```python
config = {"configurable": {"thread_id": "user_123"}}
```

### Setting Up SQLite Persistence

To add persistence, we need to:
1. Create a SqliteSaver checkpointer with a database file path
2. Compile the graph with the checkpointer
3. Pass the thread_id in the config when invoking the graph


```python
# Create the same workflow structure
workflow_with_memory = StateGraph(ChatState)
workflow_with_memory.add_node("chatbot", chatbot_node)
workflow_with_memory.add_edge(START, "chatbot")
workflow_with_memory.add_edge("chatbot", END)

# The KEY difference: compile WITH a checkpointer
# SqliteSaver stores conversation state in a database file
# For Jupyter notebooks, we create the connection directly

# Create database connection
conn = sqlite3.connect("chatbot_memory.db", check_same_thread=False)
sqlite_checkpointer = SqliteSaver(conn)

# Compile with the checkpointer
app_with_memory = workflow_with_memory.compile(checkpointer=sqlite_checkpointer)

print("Chatbot with SQLite persistence compiled successfully!")
print("Database file: chatbot_memory.db")
```

    Chatbot with SQLite persistence compiled successfully!
    Database file: chatbot_memory.db


### Testing with Memory

Now let's test the same conversation with memory enabled. Notice how we:
1. Use the same `thread_id` for related messages
2. Only send the NEW message each time (not the entire history)
3. The checkpointer automatically loads and saves the full conversation history


```python
print("=" * 60)
print("DEMONSTRATION: Chatbot WITH SQLite Persistence")
print("=" * 60)

# Define our thread configuration
config = {"configurable": {"thread_id": "conversation_1"}}

# First message
print("\nUser: My name is Alice")
result1 = app_with_memory.invoke(
    {"messages": [HumanMessage(content="My name is Alice")]},
    config=config  # Pass the thread_id config
)
print(f"Bot: {result1['messages'][-1].content}")

# Second message - using the SAME thread_id
print("\nUser: What's my name?")
result2 = app_with_memory.invoke(
    {"messages": [HumanMessage(content="What's my name?")]},
    config=config  # Same thread_id = same conversation
)
print(f"Bot: {result2['messages'][-1].content}")

# Third message - continuing the conversation
print("\nUser: What was the first thing I told you?")
result3 = app_with_memory.invoke(
    {"messages": [HumanMessage(content="What was the first thing I told you?")]},
    config=config
)
print(f"Bot: {result3['messages'][-1].content}")

print("\n" + "=" * 60)
print("Success! The bot remembers the entire conversation!")
print("This is now stored persistently in chatbot_memory.db")
print("=" * 60)
```

    ============================================================
    DEMONSTRATION: Chatbot WITH SQLite Persistence
    ============================================================
    
    User: My name is Alice
    Bot: Hello again, Alice! How can I assist you today?
    
    User: What's my name?
    Bot: Your name is Alice.
    
    User: What was the first thing I told you?
    Bot: The first thing you told me was, "My name is Alice."
    
    ============================================================
    Success! The bot remembers the entire conversation!
    This is now stored persistently in chatbot_memory.db
    ============================================================


### Viewing the Conversation History

We can retrieve the full conversation history from the checkpointer using the `get_state()` method.


```python
print("=" * 60)
print("Full Conversation History for thread_id='conversation_1'")
print("=" * 60)

# Get the current state for our thread
state = app_with_memory.get_state(config)

# Display all messages
for i, message in enumerate(state.values["messages"], 1):
    role = "User" if isinstance(message, HumanMessage) else "Bot"
    print(f"\n{i}. {role}: {message.content}")
```

    ============================================================
    Full Conversation History for thread_id='conversation_1'
    ============================================================
    
    1. User: My name is Alice
    
    2. Bot: Nice to meet you, Alice! How can I assist you today?
    
    3. User: What's my name?
    
    4. Bot: Your name is Alice. How can I help you today?
    
    5. User: What was the first thing I told you?
    
    6. Bot: The first thing you told me was, "My name is Alice."
    
    7. User: My name is Alice
    
    8. Bot: Hello again, Alice! How can I assist you today?
    
    9. User: What's my name?
    
    10. Bot: Your name is Alice.
    
    11. User: What was the first thing I told you?
    
    12. Bot: The first thing you told me was, "My name is Alice."


## Example : Single User Persistent Conversation

Let's demonstrate a realistic scenario where a user has a conversation, then comes back later (simulated by using the same thread_id in a new invocation).


```python
print("=" * 70)
print("EXAMPLE 1: Single User - Conversation Continuity")
print("=" * 70)

# User's thread
user_config = {"configurable": {"thread_id": "user_alice_thread"}}

print("\n--- Session 1: Initial Conversation ---")
print("\nUser: I'm planning a trip to Paris next month.")
result = app_with_memory.invoke(
    {"messages": [HumanMessage(content="I'm planning a trip to Paris next month.")]},
    config=user_config
)
print(f"Bot: {result['messages'][-1].content}")

print("\nUser: What are the must-see attractions?")
result = app_with_memory.invoke(
    {"messages": [HumanMessage(content="What are the must-see attractions?")]},
    config=user_config
)
print(f"Bot: {result['messages'][-1].content}")

print("\n--- User logs off, comes back later ---")
print("--- Session 2: Continuing the Conversation ---")
print("\nUser: Thanks for those suggestions! How about restaurants?")
result = app_with_memory.invoke(
    {"messages": [HumanMessage(content="Thanks for those suggestions! How about restaurants?")]},
    config=user_config  # Same thread_id = continues the conversation
)
print(f"Bot: {result['messages'][-1].content}")

print("\nUser: Which one is closest to the Eiffel Tower?")
result = app_with_memory.invoke(
    {"messages": [HumanMessage(content="Which one is closest to the Eiffel Tower?")]},
    config=user_config
)
print(f"Bot: {result['messages'][-1].content}")

print("\n" + "=" * 70)
print("Key Takeaway: Same thread_id maintains conversation context")
print("across multiple sessions!")
print("=" * 70)
```

    ======================================================================
    EXAMPLE 1: Single User - Conversation Continuity
    ======================================================================
    
    --- Session 1: Initial Conversation ---
    
    User: I'm planning a trip to Paris next month.
    Bot: That sounds wonderful! Paris is a fantastic destination with so much to explore. If you have any specific questions or need help with planning your itinerary, feel free to ask! Whether you need recommendations on attractions, restaurants, accommodations, or tips for getting around, I'm here to help. What are you most excited to do in Paris?
    
    User: What are the must-see attractions?
    Bot: When visiting Paris, there are several iconic attractions that you won't want to miss. Here’s a list of must-see spots:
    
    1. **Eiffel Tower**: The symbol of Paris, you can take an elevator ride to the top for spectacular views of the city. Visiting at night when it’s illuminated is particularly magical.
    
    2. **Louvre Museum**: One of the world’s largest and most famous art museums, home to masterpieces like the Mona Lisa and the Venus de Milo. Allocate a few hours to explore its vast collection.
    
    3. **Notre-Dame Cathedral**: Although it is currently under restoration due to the 2019 fire, the exterior remains stunning. The area around Notre-Dame is also worth exploring.
    
    4. **Sacré-Cœur Basilica**: Located in Montmartre, this basilica offers breathtaking views of the city from its dome. The surrounding neighborhood is charming, with artists and cafés.
    
    5. **Champs-Élysées and Arc de Triomphe**: Stroll down this famous avenue, lined with shops and cafés, and visit the Arc de Triomphe for a panoramic view of Paris.
    
    6. **Palace of Versailles**: A short trip from Paris, this opulent palace and its gardens are a must-see for anyone interested in French history and royalty.
    
    7. **Musée d'Orsay**: Housed in a former train station, this museum is known for its collection of Impressionist and Post-Impressionist masterpieces, including works by Monet, Van Gogh, and Degas.
    
    8. **Seine River Cruise**: A cruise on the Seine, especially at sunset or in the evening, offers beautiful views of many iconic landmarks along the river.
    
    9. **Latin Quarter**: This historic area is known for its narrow streets, lively atmosphere, and rich literary history. It's a great place to wander, shop, and enjoy a meal.
    
    10. **Sainte-Chapelle**: Famous for its stunning stained glass windows, this Gothic chapel is located near Notre-Dame and is a hidden gem worth visiting.
    
    11. **Montmartre**: Explore this bohemian neighborhood where artists like Picasso and Van Gogh lived. It’s filled with quaint streets, cafés, and the beautiful Place du Tertre.
    
    12. **The Pompidou Center**: Known for its modern architecture and contemporary art collections, it's an interesting contrast to the historical sites in the city.
    
    Be sure to check the opening hours and book tickets in advance for popular attractions to save time. Enjoy your trip to Paris!
    
    --- User logs off, comes back later ---
    --- Session 2: Continuing the Conversation ---
    
    User: Thanks for those suggestions! How about restaurants?
    Bot: Paris is a culinary delight with a wide range of dining options, from traditional French bistros to modern eateries. Here are some recommendations across different styles and budgets:
    
    ### Classic French Bistros:
    1. **Le Comptoir de la Gastronomie**: A classic bistro known for its traditional French dishes, including duck confit and a variety of charcuterie.
    2. **Chez Janou**: Located in the Marais, this charming spot serves Provençal cuisine and offers a lovely courtyard. Their chocolate mousse is a must-try!
    
    ### Michelin-Starred Restaurants:
    3. **Le Meurice**: A luxurious dining experience with a menu inspired by French cuisine. The elegant setting and exceptional service make it a special occasion spot.
    4. **L'Arpège**: Famous for its vegetable-focused dishes, this three-Michelin-star restaurant emphasizes fresh, seasonal ingredients.
    
    ### Casual Dining:
    5. **L’As du Fallafel**: Renowned for its delicious falafel sandwiches, this casual eatery in the Marais is a favorite for a quick and tasty meal.
    6. **Le Relais de l’Entrecôte**: Known for its steak-frites and secret sauce, this restaurant serves a simple yet satisfying menu.
    
    ### Bakeries and Cafés:
    7. **Du Pain et des Idées**: An artisanal bakery famous for its pain au chocolat and other pastries. Perfect for breakfast or a snack.
    8. **Café de Flore**: One of Paris's oldest cafés, it’s a great spot for people-watching while enjoying a coffee or a light meal.
    
    ### Modern and Trendy Spots:
    9. **Frenchie**: Located in the 2nd arrondissement, this trendy restaurant offers a modern twist on French cuisine. Reservations are highly recommended!
    10. **Le Mary Celeste**: A hip bar and restaurant in the Marais known for its creative cocktails and small plates, perfect for sharing.
    
    ### Dessert:
    11. **Pierre Hermé**: Famous for his macarons, a visit to one of his shops is a must for anyone with a sweet tooth.
    12. **Angelina**: Known for its rich hot chocolate and pastries, this historic tearoom offers a delightful break during your sightseeing.
    
    ### Tips:
    - **Reservations**: For popular restaurants, especially those with Michelin stars, making a reservation is highly recommended.
    - **Explore Local Neighborhoods**: Some of the best meals can be found in smaller, less touristy spots, so don’t hesitate to wander and discover hidden gems.
    - **Enjoy the Experience**: Dining in Paris is often a leisurely affair, so take your time and savor the experience.
    
    Enjoy your culinary adventures in Paris!
    
    User: Which one is closest to the Eiffel Tower?
    Bot: If you're looking for restaurants close to the Eiffel Tower, here are a few excellent options:
    
    1. **Le Café de l'Homme**: Located in the Trocadéro area, this restaurant offers stunning views of the Eiffel Tower. It's a great place to enjoy French cuisine in a stylish setting.
    
    2. **Les Ombres**: Situated on the rooftop of the Musée du quai Branly, this restaurant provides beautiful views of the Eiffel Tower, especially at sunset. The menu features modern French cuisine.
    
    3. **58 Tour Eiffel**: Located on the first floor of the Eiffel Tower, this restaurant offers a unique dining experience with incredible views. Reservations are highly recommended, as it can be quite popular.
    
    4. **Le Relais de l'Entrecôte**: While not directly next to the Eiffel Tower, it’s located in the 6th arrondissement and is a short distance away. This casual eatery is famous for its steak-frites.
    
    5. **Bistro Parisien**: Located right by the Seine River, this bistro offers a lovely view of the Eiffel Tower and serves a variety of French dishes in a relaxed atmosphere.
    
    These options allow you to enjoy a meal while taking in the beauty of the Eiffel Tower! Make sure to check for reservations, especially for those with great views!
    
    ======================================================================
    Key Takeaway: Same thread_id maintains conversation context
    across multiple sessions!
    ======================================================================

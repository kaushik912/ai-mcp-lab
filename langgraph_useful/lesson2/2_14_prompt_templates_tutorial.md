# Creating Reusable Prompt Templates with LangChain

## Introduction

In this tutorial, you'll learn how to create **reusable prompt templates** using LangChain, enabling you to build flexible, maintainable AI applications.

### What Are Prompt Templates?

Prompt templates are parameterized strings or message structures that allow you to:
- Define prompts once and reuse them with different inputs
- Maintain consistency across your application
- Easily modify prompts without changing application logic
- Separate prompt design from code implementation

### Problems They Solve

Without templates, you face:
- **Inflexible hardcoded prompts** that are difficult to change
- **Scaling challenges** when modifying prompts across multiple locations
- **Limited customization** for different users or contexts
- **Maintenance headaches** when testing prompt variations

### Common Use Cases

- **Multi-user applications**: Customize prompts per user role
- **A/B testing**: Test different prompt variations
- **Localization**: Adapt prompts for different languages
- **Domain-specific variations**: Use the same logic with different domains

### Template Types

LangChain provides two main template types:

1. **PromptTemplate**: For simple string-based prompts (completion models)
2. **ChatPromptTemplate**: For structured conversations with roles (chat models)

### Prerequisites

- Python 3.12+
- OpenAI API key (set in a `.env` file)
- Basic understanding of Python strings and dictionaries

### Learning Objectives

By the end of this notebook, you will:
- Understand when to use PromptTemplate vs ChatPromptTemplate
- Create templates with single and multiple variables
- Build multi-role conversation templates
- Integrate templates with ChatOpenAI for complete workflows
- Apply best practices for template design and reusability

## Setup: Install Dependencies and Load API Key

First, we'll install the required packages and configure our OpenAI API key.

**Note**: Make sure you have a `.env` file in your project directory with:
```
OPENAI_API_KEY=your_api_key_here
```


```python
# Install required packages (uncomment if needed)
# !pip install langchain-core langchain-openai python-dotenv

from langchain_core.prompts import PromptTemplate, ChatPromptTemplate, MessagesPlaceholder
from langchain_openai import ChatOpenAI
from dotenv import load_dotenv
import os

# Load environment variables from .env file
load_dotenv()

# Verify API key is loaded
if os.getenv("OPENAI_API_KEY"):
    print("Setup complete! API key loaded successfully.")
else:
    print("WARNING: OPENAI_API_KEY not found. Please check your .env file.")
```

    Setup complete! API key loaded successfully.


## Part 1: PromptTemplate for Simple String-Based Prompts

### What is PromptTemplate?

`PromptTemplate` is designed for simple, string-based prompts. It uses **curly braces `{}`** to define variables that will be substituted at runtime.

### Use When:
- You need a single completion prompt
- Working with simple text generation tasks
- The prompt doesn't require multiple roles (system, user, assistant)

### Basic Syntax

```python
template = PromptTemplate.from_template("Your prompt with {variable}")
prompt = template.invoke({"variable": "value"})
```

### Example 1: Single Variable Template

Let's start with the simplest case: a template with one variable.


```python
# Create a template with a single variable
simple_template = PromptTemplate.from_template(
    "Write a short poem about {topic}"
)

# Invoke the template with a specific value
prompt = simple_template.invoke({"topic": "mountains"})

# Display the formatted prompt
print("Formatted prompt:")
print(prompt.to_string())
print("\nType:", type(prompt))
```

    Formatted prompt:
    Write a short poem about mountains
    
    Type: <class 'langchain_core.prompt_values.StringPromptValue'>


### Example 2: Multiple Variables Template

Templates become more powerful with multiple variables. This allows you to create complex, reusable prompts.


```python
# Create a template with multiple variables
multi_var_template = PromptTemplate.from_template(
    "Tell me a {adjective} joke about {topic} suitable for {audience}"
)

# Try it with different combinations
print("Example 1:")
prompt1 = multi_var_template.invoke({
    "adjective": "funny",
    "topic": "programming",
    "audience": "software engineers"
})
print(prompt1.to_string())

print("\n" + "="*60 + "\n")

print("Example 2:")
prompt2 = multi_var_template.invoke({
    "adjective": "clever",
    "topic": "data science",
    "audience": "statisticians"
})
print(prompt2.to_string())
```

    Example 1:
    Tell me a funny joke about programming suitable for software engineers
    
    ============================================================
    
    Example 2:
    Tell me a clever joke about data science suitable for statisticians


### Example 3: Using PromptTemplate with ChatOpenAI

Now let's see a complete workflow: reuse our existing template, format it, and get a response from the model.



```python
# Initialize the chat model
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)

# Reuse the multi_var_template from Example 2!
# This demonstrates template reusability across different contexts

print("Using our existing multi_var_template with new values:")
prompt = multi_var_template.invoke({
    "adjective": "short",
    "topic": "Python dictionaries", 
    "audience": "beginners"
})

print("\nFormatted Prompt:")
print(prompt.to_string())
print("\n" + "="*60 + "\n")

# Get response from the model
response = llm.invoke(prompt.to_string())

print("Model Response:")
print(response.content)
```

    Using our existing multi_var_template with new values:
    
    Formatted Prompt:
    Tell me a short joke about Python dictionaries suitable for beginners
    
    ============================================================
    
    Model Response:
    Why did the Python dictionary break up with the list?
    
    Because it found someone who could really “key” into its values!


## Part 2: ChatPromptTemplate for Multi-Role Conversations

### What is ChatPromptTemplate?

`ChatPromptTemplate` is designed for chat models that use **roles** (system, user, assistant). It allows you to structure conversations with multiple messages.

### Key Differences from PromptTemplate

| Feature | PromptTemplate | ChatPromptTemplate |
|---------|---------------|--------------------|
| Output | Single string | List of messages |
| Roles | No role concept | System, user, assistant |
| Use case | Simple completion | Conversational AI |
| Method | `from_template()` | `from_messages()` |

### Use When:
- Building conversational applications
- You need to set system behavior/instructions
- Working with chat models (like GPT-4, Claude)
- Managing multi-turn conversations

### Basic Syntax

```python
template = ChatPromptTemplate.from_messages([
    ("system", "System message with {variable}"),
    ("user", "User message with {variable}")
])
```

### Example 4: Basic Chat Template with System and User Roles

Let's create a template that sets the system behavior and accepts user input.


```python
# Create a chat template with system and user roles
chat_template = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful {role} who explains concepts in simple terms."),
    ("user", "{input}")
])

# Invoke the template
messages = chat_template.invoke({
    "role": "coding instructor",
    "input": "What is a Python decorator?"
})

# Display the formatted messages
print("Formatted Messages:")
for msg in messages.to_messages():
    print(f"{msg.type.upper()}: {msg.content}")
```

    Formatted Messages:
    SYSTEM: You are a helpful coding instructor who explains concepts in simple terms.
    HUMAN: What is a Python decorator?


### Example 5: Using ChatPromptTemplate with ChatOpenAI

Now let's see the complete workflow with a chat model. 


```python
# Initialize the chat model (if not already done)
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)

# Reuse the chat_template from Example 4!
# This demonstrates how one template can serve multiple use cases

print("Reusing chat_template from Example 4 with different values:")
messages = chat_template.invoke({
    "role": "Python programming expert",
    "input": "How do I reverse a string in Python?"
})

print("\nFormatted Messages:")
for msg in messages.to_messages():
    print(f"{msg.type.upper()}: {msg.content}")
print("\n" + "="*60 + "\n")

# Get response from the model
response = llm.invoke(messages)

print("Model Response:")
print(response.content)
```

    Reusing chat_template from Example 4 with different values:
    
    Formatted Messages:
    SYSTEM: You are a helpful Python programming expert who explains concepts in simple terms.
    HUMAN: How do I reverse a string in Python?
    
    ============================================================
    
    Model Response:
    Reversing a string in Python can be done in several simple ways. Here are a few common methods:
    
    ### 1. Using Slicing
    Python allows you to use slicing to reverse a string easily. Here's how you can do it:
    
    ```python
    original_string = "Hello, World!"
    reversed_string = original_string[::-1]
    print(reversed_string)  # Output: !dlroW ,olleH
    ```
    
    In this example, `[::-1]` means "take the string from start to end but step backwards by 1."
    
    ### 2. Using the `reversed()` Function
    You can also use the `reversed()` function, which returns an iterator that accesses the given string in reverse order. You’ll need to join the characters back into a string:
    
    ```python
    original_string = "Hello, World!"
    reversed_string = ''.join(reversed(original_string))
    print(reversed_string)  # Output: !dlroW ,olleH
    ```
    
    ### 3. Using a Loop
    If you prefer to use a loop, you can build the reversed string character by character:
    
    ```python
    original_string = "Hello, World!"
    reversed_string = ''
    for char in original_string:
        reversed_string = char + reversed_string
    print(reversed_string)  # Output: !dlroW ,olleH
    ```
    
    ### 4. Using a Stack
    Another way to reverse a string is by using a stack (a list in this case):
    
    ```python
    original_string = "Hello, World!"
    stack = list(original_string)
    reversed_string = ''
    while stack:
        reversed_string += stack.pop()
    print(reversed_string)  # Output: !dlroW ,olleH
    ```
    
    ### Summary
    All of these methods will give you the reversed version of the original string. The slicing method is the most concise and commonly used way in Python. Choose the method that you find the most intuitive!

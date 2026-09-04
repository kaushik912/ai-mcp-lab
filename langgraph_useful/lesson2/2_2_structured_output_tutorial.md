# Structured Output for AI Agents: A Comprehensive Tutorial

## Learning Objectives

By the end of this notebook, you will be able to:
- Understand why structured output is essential for AI agent systems
- Implement structured output using OpenAI's JSON Schema with strict mode
- Use Pydantic models with LangChain for type-safe structured output
- Compare different approaches and choose the right one for your use case
- Handle validation and error cases effectively

## Prerequisites

- Basic Python knowledge
- Familiarity with LLMs and API calls
- OpenAI API key (set as environment variable `OPENAI_API_KEY`)

## Required Libraries

```bash
pip install openai langchain-openai pydantic python-dotenv
```

## 1. Introduction: Why Structured Output Matters

### The Problem

Large Language Models (LLMs) are trained to generate natural language text. While this is powerful for human interaction, it creates challenges when building AI agent systems:

- **Unpredictable Format**: LLMs might return data in different formats each time
- **Parsing Complexity**: Extracting specific information from free-form text is error-prone
- **Type Safety**: No guarantees about data types or required fields
- **Integration Issues**: Other components in your system need reliable, predictable data

### The Solution: Structured Output

Structured output ensures that LLM responses conform to a predefined schema, providing:

1. **Predictability**: Always receive data in the expected format
2. **Type Safety**: Guaranteed data types (strings, integers, booleans, etc.)
3. **Validation**: Automatic checking of required fields and constraints
4. **Seamless Integration**: Easy to pass data between agent components
5. **Tool Calling**: Enables reliable function/tool invocation in agent workflows

### Two Main Approaches

We'll explore two powerful methods:

1. **OpenAI API with JSON Schema**: Direct API control with strict schema enforcement
2. **LangChain with Pydantic Models**: Higher-level abstraction with Python type hints

## 2. Setup and Imports

Let's start by importing the necessary libraries and setting up our environment.


```python
import os
import json
from openai import OpenAI
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field, ValidationError
from typing import Optional, List
from dotenv import load_dotenv

load_dotenv()

# Verify OpenAI API key is set
if not os.getenv("OPENAI_API_KEY"):
    print("WARNING: OPENAI_API_KEY environment variable not set!")
    print("Please set it before running the examples.")
else:
    print("OpenAI API key found. Ready to proceed!")
```

    OpenAI API key found. Ready to proceed!


## 3. Approach 1: OpenAI API with JSON Schema

### Overview

OpenAI's API supports structured outputs through the `response_format` parameter with:
- `type: "json_schema"` to specify the desired structure
- `strict: True` to guarantee schema compliance

### Use Case: User Information Extraction

We'll extract structured user information (name, age, email) from natural language text.

### 3.1 Important: Optional Fields with Strict Mode

When using `strict: true` mode in OpenAI's structured outputs, there's a key constraint:

**All properties MUST be included in the `required` array.**

This means you cannot have truly "optional" fields in the traditional JSON Schema sense. However, you can emulate optional fields using **union types with null**:

```json
{
  "properties": {
    "age": {
      "type": ["integer", "null"],
      "description": "Age or null if not provided"
    }
  },
  "required": ["name", "age", "email"]
}
```

This approach:
- Satisfies the strict mode requirement (all fields in `required`)
- Allows the model to return `null` when data is not available
- Maintains schema compliance and type safety

Let's see this in action:

### 3.2 Define the JSON Schema

First, we define our desired output structure using JSON Schema format with the union type approach for optional fields:


```python
# Define JSON Schema for user information
# Note: age uses union type ["integer", "null"] to allow null values
user_info_schema = {
    "type": "json_schema",
    "json_schema": {
        "name": "user_info",
        "schema": {
            "type": "object",
            "properties": {
                "name": {
                    "type": "string",
                    "description": "The user's full name"
                },
                "age": {
                    "type": ["integer", "null"],
                    "description": "The user's age in years, or null if not provided"
                },
                "email": {
                    "type": "string",
                    "description": "The user's email address"
                }
            },
            "required": ["name", "age", "email"],
            "additionalProperties": False
        },
        "strict": True
    }
}

print("JSON Schema defined successfully!")
print("\nSchema structure:")
print(json.dumps(user_info_schema["json_schema"]["schema"], indent=2))
```

    JSON Schema defined successfully!
    
    Schema structure:
    {
      "type": "object",
      "properties": {
        "name": {
          "type": "string",
          "description": "The user's full name"
        },
        "age": {
          "type": [
            "integer",
            "null"
          ],
          "description": "The user's age in years, or null if not provided"
        },
        "email": {
          "type": "string",
          "description": "The user's email address"
        }
      },
      "required": [
        "name",
        "age",
        "email"
      ],
      "additionalProperties": false
    }


### 3.3 Create a Helper Function for Structured Output

Let's create a reusable helper function to make API calls with structured output cleaner and more maintainable:


```python
def extract_with_structured_output(
    text: str,
    schema: dict,
    model: str = "gpt-5",
    system_message: str = "You are a helpful assistant that extracts structured information from text."
) -> dict:
    """
    Helper function to extract structured output using OpenAI API.
    
    Args:
        text: The input text to extract information from
        schema: The JSON schema defining the structure
        model: The OpenAI model to use
        system_message: System message to guide the model
        
    Returns:
        Dictionary containing the structured output
        
    Raises:
        Exception: If API call fails or JSON parsing fails
    """
    client = OpenAI()
    try:
        response = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": system_message},
                {"role": "user", "content": text}
            ],
            response_format=schema
        )
        return json.loads(response.choices[0].message.content)
    except json.JSONDecodeError as e:
        raise Exception(f"Failed to parse JSON response: {e}")
    except Exception as e:
        raise Exception(f"API call failed: {e}")

print("Helper function created successfully!")
print("This function makes structured output extraction reusable and cleaner.")
```

    Helper function created successfully!
    This function makes structured output extraction reusable and cleaner.


### 3.4 Use the Helper Function

Now let's use our helper function to extract user information:


```python
# Example user input text
user_text = "Extract user info: John Doe, 30 years old, john.doe@example.com"

# System message to guide extraction
system_msg = "You are a helpful assistant that extracts user information from text."

# Use helper function
structured_output = extract_with_structured_output(
    text=user_text,
    schema=user_info_schema,
    system_message=system_msg
)

print("Input text:")
print(f"  {user_text}")
print("\nStructured output:")
print(json.dumps(structured_output, indent=2))

```

    Input text:
      Extract user info: John Doe, 30 years old, john.doe@example.com
    
    Structured output:
    {
      "name": "John Doe",
      "age": 30,
      "email": "john.doe@example.com"
    }


### 3.5 Testing with Multiple Inputs

Let's test the robustness of our structured output with various input formats:


```python
# Test cases with different input formats
test_cases = [
    "My name is Alice Smith, I'm 25, and you can reach me at alice.smith@email.com",
    "Bob Johnson, bob.j@company.org, age 42",
    "Contact: sarah.williams@domain.com, Name: Sarah Williams (no age provided)"
]

system_msg = "You are a helpful assistant that extracts user information from text."

for i, test_input in enumerate(test_cases, 1):
    print(f"\n{'='*60}")
    print(f"Test Case {i}")
    print(f"{'='*60}")
    print(f"Input: {test_input}")
    
    result = extract_with_structured_output(
        text=test_input,
        schema=user_info_schema,
        system_message=system_msg
    )
    
    print(f"\nOutput:")
    print(json.dumps(result, indent=2))
```

    
    ============================================================
    Test Case 1
    ============================================================
    Input: My name is Alice Smith, I'm 25, and you can reach me at alice.smith@email.com
    
    Output:
    {
      "name": "Alice Smith",
      "age": 25,
      "email": "alice.smith@email.com"
    }
    
    ============================================================
    Test Case 2
    ============================================================
    Input: Bob Johnson, bob.j@company.org, age 42
    
    Output:
    {
      "name": "Bob Johnson",
      "age": 42,
      "email": "bob.j@company.org"
    }
    
    ============================================================
    Test Case 3
    ============================================================
    Input: Contact: sarah.williams@domain.com, Name: Sarah Williams (no age provided)
    
    Output:
    {
      "name": "Sarah Williams",
      "age": null,
      "email": "sarah.williams@domain.com"
    }


## 4. Approach 2: LangChain with Pydantic Models

### Overview

LangChain provides a higher-level abstraction using Pydantic models:
- Define schemas using Python classes with type hints
- Use `with_structured_output()` method on chat models
- Automatic validation and type conversion
- More Pythonic and developer-friendly

### Use Case: Same User Information Extraction

We'll implement the same use case to compare approaches directly.

### 4.1 Define the Pydantic Model

Instead of JSON Schema, we define a Python class with type annotations:


```python
# Define the Pydantic model for user information
class UserInfo(BaseModel):
    """Model for extracting user information from text."""
    
    name: str = Field(
        description="The user's full name"
    )
    age: Optional[int] = Field(
        default=None,
        description="The user's age in years"
    )
    email: str = Field(
        description="The user's email address"
    )
    

print("Pydantic model defined successfully!")
```

    Pydantic model defined successfully!


### 4.2 Create Structured Output Model

Now let's create a LangChain model configured for structured output:


```python
# Initialize the base chat model
base_model = ChatOpenAI(
    model="gpt-5",
    temperature=0
)

# Create structured output model
structured_model = base_model.with_structured_output(UserInfo)

print("Structured output model created successfully!")
print("The model will now return UserInfo objects instead of text.")
```

    Structured output model created successfully!
    The model will now return UserInfo objects instead of text.


### 4.3 Invoke with Natural Language

Let's extract structured information using our Pydantic-based model:


```python
# Example user input
user_text = "Extract user info: John Doe, 30 years old, john.doe@example.com"

# Invoke the structured model
result = structured_model.invoke(user_text)

print("Input text:")
print(f"  {user_text}")
print("\nStructured output:")
print(f"  Type: {type(result)}")
print(f"  Result: {result}")
print("\nAccessing fields (with type safety):")
print(f"  Name: {result.name} (type: {type(result.name).__name__})")
print(f"  Age: {result.age} (type: {type(result.age).__name__})")
print(f"  Email: {result.email} (type: {type(result.email).__name__})")
print("\nConvert to dictionary:")
print(json.dumps(result.model_dump(), indent=2))
```

    Input text:
      Extract user info: John Doe, 30 years old, john.doe@example.com
    
    Structured output:
      Type: <class '__main__.UserInfo'>
      Result: name='John Doe' age=30 email='john.doe@example.com'
    
    Accessing fields (with type safety):
      Name: John Doe (type: str)
      Age: 30 (type: int)
      Email: john.doe@example.com (type: str)
    
    Convert to dictionary:
    {
      "name": "John Doe",
      "age": 30,
      "email": "john.doe@example.com"
    }


### 4.4 Testing with Various Inputs

Let's test with the same diverse inputs we used earlier:


```python
# Same test cases as before
test_cases = [
    "My name is Alice Smith, I'm 25, and you can reach me at alice.smith@email.com",
    "Bob Johnson, bob.j@company.org, age 42",
    "Contact: sarah.williams@domain.com, Name: Sarah Williams (no age provided)"
]

for i, test_input in enumerate(test_cases, 1):
    print(f"\n{'='*60}")
    print(f"Test Case {i}")
    print(f"{'='*60}")
    print(f"Input: {test_input}")
    
    result = structured_model.invoke(test_input)

    print(json.dumps(result.model_dump(), indent=2))
```

    
    ============================================================
    Test Case 1
    ============================================================
    Input: My name is Alice Smith, I'm 25, and you can reach me at alice.smith@email.com
    {
      "name": "Alice Smith",
      "age": 25,
      "email": "alice.smith@email.com"
    }
    
    ============================================================
    Test Case 2
    ============================================================
    Input: Bob Johnson, bob.j@company.org, age 42
    {
      "name": "Bob Johnson",
      "age": 42,
      "email": "bob.j@company.org"
    }
    
    ============================================================
    Test Case 3
    ============================================================
    Input: Contact: sarah.williams@domain.com, Name: Sarah Williams (no age provided)
    {
      "name": "Sarah Williams",
      "age": null,
      "email": "sarah.williams@domain.com"
    }

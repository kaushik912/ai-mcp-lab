# Create Test Cases with Expected Outputs for AI Agents

## Overview

In this tutorial, you'll learn how to create robust test cases for AI agents and their tools. Testing AI agents is fundamentally different from testing traditional software because agent behavior involves stochastic (probabilistic) elements from the language models.

## What You'll Learn

- **What is a test case**: Understanding input → expected output patterns
- **Why AI agent testing is different**: Deterministic vs. stochastic behavior
- **Anatomy of a test case**: The Arrange-Act-Assert (AAA) pattern
- **Testing individual tools**: Verify tool functions work correctly
- **Testing agent actions**: Check tool usage, parameters, and outputs
- **Writing good test cases**: Clear, focused, independent, and repeatable

## Prerequisites

- **API Keys Required**: You'll need `OPENAI_API_KEY`
  - OpenAI: https://platform.openai.com/api-keys
- **Required Packages**: `langchain`, `langchain-openai`, `pytest`, `python-dotenv`

## Setup Instructions

1. Create a `.env` file in your project directory
2. Add your API key:
   ```
   OPENAI_API_KEY=your-openai-key-here
   ```
3. Install required packages:
   ```bash
   pip install langchain langchain-openai pytest python-dotenv
   ```

## Understanding Test Cases

### What is a Test Case?

A **test case** is a specification that describes:
1. **Input**: What you provide to the system
2. **Expected Output**: What you expect the system to produce
3. **Verification**: How you check that the output matches expectations

**Example**: For a function `add(a, b)`:
- Input: `add(2, 3)`
- Expected Output: `5`
- Verification: `assert add(2, 3) == 5`

### Why AI Agent Testing is Different

Traditional software is **deterministic** - the same input always produces the same output.

AI agents are **stochastic** (probabilistic) - the language model may produce different outputs for the same input because:
- Temperature settings introduce randomness
- Model responses vary naturally
- Wording differs while meaning stays consistent

**Therefore**, when testing AI agents we focus on:
1. **Tool usage**: Was the correct tool called?
2. **Parameters**: Were the right arguments passed?
3. **Output structure**: Does the response contain expected elements?
4. **Semantic correctness**: Is the answer reasonable, not exact word matching?

### The Arrange-Act-Assert Pattern

All good tests follow the **AAA pattern**:

1. **Arrange**: Set up test data and preconditions
2. **Act**: Execute the code being tested
3. **Assert**: Verify the results match expectations

This pattern makes tests clear, readable, and maintainable.

## Step 1: Import Libraries and Load Environment Variables

First, we'll import all necessary libraries and load our API keys from the `.env` file.


```python
import os
from dotenv import load_dotenv
from typing import Dict, Any, List
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

# Load environment variables from .env file
load_dotenv()

# Verify API key is loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment variables")

print("Environment and libraries loaded successfully!")
```

    Environment and libraries loaded successfully!


## Step 2: Create Simple Tools for Testing

We'll create two simple tools that our agent can use:
1. **Calculator**: Performs basic arithmetic operations
2. **Temperature Converter**: Converts between Celsius and Fahrenheit

These tools are deliberately simple so we can focus on testing concepts rather than complex logic.


```python
@tool
def calculator(operation: str, a: float, b: float) -> float:
    """
    Performs basic arithmetic operations.
    
    Args:
        operation: The operation to perform (add, subtract, multiply, divide)
        a: First number
        b: Second number
    
    Returns:
        The result of the operation
    """
    operations = {
        "add": lambda x, y: x + y,
        "subtract": lambda x, y: x - y,
        "multiply": lambda x, y: x * y,
        "divide": lambda x, y: x / y if y != 0 else "Error: Division by zero"
    }
    
    if operation not in operations:
        return f"Error: Unknown operation '{operation}'"
    
    return operations[operation](a, b)


@tool
def temperature_converter(temperature: float, from_unit: str, to_unit: str) -> Dict[str, Any]:
    """
    Converts temperature between Celsius and Fahrenheit.
    
    Args:
        temperature: The temperature value to convert
        from_unit: Source unit (C or F)
        to_unit: Target unit (C or F)
    
    Returns:
        A dictionary with the conversion result
    """
    from_unit = from_unit.upper()
    to_unit = to_unit.upper()
    
    if from_unit == to_unit:
        return {
            "original": temperature,
            "converted": temperature,
            "from_unit": from_unit,
            "to_unit": to_unit
        }
    
    if from_unit == "C" and to_unit == "F":
        converted = (temperature * 9/5) + 32
    elif from_unit == "F" and to_unit == "C":
        converted = (temperature - 32) * 5/9
    else:
        return {"error": f"Invalid units: {from_unit} to {to_unit}"}
    
    return {
        "original": temperature,
        "converted": round(converted, 2),
        "from_unit": from_unit,
        "to_unit": to_unit
    }

print("Tools created successfully!")
print(f"Tool 1: {calculator.name} - {calculator.description}")
print(f"Tool 2: {temperature_converter.name} - {temperature_converter.description}")
```

    Tools created successfully!
    Tool 1: calculator - Performs basic arithmetic operations.
    
    Args:
        operation: The operation to perform (add, subtract, multiply, divide)
        a: First number
        b: Second number
    
    Returns:
        The result of the operation
    Tool 2: temperature_converter - Converts temperature between Celsius and Fahrenheit.
    
    Args:
        temperature: The temperature value to convert
        from_unit: Source unit (C or F)
        to_unit: Target unit (C or F)
    
    Returns:
        A dictionary with the conversion result


## Part 1: Testing Individual Tools

### Why Test Tools Separately?

Before testing an agent's use of tools, we should verify that the tools themselves work correctly. This is called **unit testing** - testing individual components in isolation.

Benefits:
- **Faster**: No LLM calls required
- **Deterministic**: Same input always produces same output
- **Cheaper**: No API costs
- **Easier debugging**: Failures point directly to tool logic

### Test Case 1: Calculator Tool - Addition


```python
def test_calculator_addition():
    """
    Test that the calculator can correctly add two numbers.
    
    This test follows the Arrange-Act-Assert pattern.
    """
    # ARRANGE: Set up test inputs
    operation = "add"
    a = 15
    b = 27
    expected_result = 42
    
    # ACT: Execute the tool
    result = calculator.invoke({
        "operation": operation,
        "a": a,
        "b": b
    })
    
    # ASSERT: Verify the result
    assert result == expected_result, f"Expected {expected_result}, but got {result}"
    print(f"✓ Test passed: {a} + {b} = {result}")

# Run the test
test_calculator_addition()
```

    ✓ Test passed: 15 + 27 = 42.0


### Test Case 2: Calculator Tool - Division

Let's test another operation to ensure our calculator handles different operations correctly.


```python
def test_calculator_division():
    """
    Test that the calculator can correctly divide two numbers.
    """
    # ARRANGE
    operation = "divide"
    a = 100
    b = 4
    expected_result = 25.0
    
    # ACT
    result = calculator.invoke({
        "operation": operation,
        "a": a,
        "b": b
    })
    
    # ASSERT
    assert result == expected_result, f"Expected {expected_result}, but got {result}"
    print(f"✓ Test passed: {a} / {b} = {result}")

# Run the test
test_calculator_division()
```

    ✓ Test passed: 100 / 4 = 25.0


### Test Case 3: Example of a Failing Test

Let's see what happens when a test fails. This helps you understand how to read error messages and debug issues.

**Note**: This test is intentionally designed to fail to demonstrate the testing process.


```python
def test_calculator_division_failing_example():
    """
    INTENTIONALLY FAILING TEST - Demonstrates what a test failure looks like.
    
    This test expects an incorrect result to show how assertion errors appear.
    """
    # ARRANGE
    operation = "divide"
    a = 100
    b = 4
    expected_result = 20.0  # WRONG! We know 100 / 4 = 25, not 20
    
    # ACT
    result = calculator.invoke({
        "operation": operation,
        "a": a,
        "b": b
    })
    
    # ASSERT
    try:
        assert result == expected_result, f"Expected {expected_result}, but got {result}"
        print(f"✓ Test passed: {a} / {b} = {result}")
    except AssertionError as e:
        print("✗ Test FAILED (as expected for this demo)!")
        print(f"  AssertionError: {e}")
        print(f"\n  What went wrong:")
        print(f"    - We expected: {expected_result}")
        print(f"    - We actually got: {result}")
        print(f"    - The test correctly caught this mismatch!")
        print(f"\n  How to fix:")
        print(f"    - Either fix the expected value (expected_result = 25.0)")
        print(f"    - Or fix the implementation if the tool is wrong")

# Run the failing test
test_calculator_division_failing_example()
```

    ✗ Test FAILED (as expected for this demo)!
      AssertionError: Expected 20.0, but got 25.0
    
      What went wrong:
        - We expected: 20.0
        - We actually got: 25.0
        - The test correctly caught this mismatch!
    
      How to fix:
        - Either fix the expected value (expected_result = 25.0)
        - Or fix the implementation if the tool is wrong


### Key Takeaways: Tool Testing

1. **Test tools in isolation** before integrating them into agents
2. **Use specific, known values** with predictable outcomes
3. **Verify both output values and structure** (e.g., dictionary keys)
4. **Learn from failures** - when tests fail, they show you exactly what's wrong
5. **Read assertion errors carefully** - they tell you expected vs. actual values
6. **Test edge cases** (like division by zero, invalid units)
7. **Keep tests independent** - each test should run without depending on others

### Test Case 4: Temperature Converter Tool

Now let's test the temperature converter to ensure it correctly converts between units.


```python
def test_temperature_converter_c_to_f():
    """
    Test that the temperature converter correctly converts Celsius to Fahrenheit.
    
    We know that 0°C = 32°F and 100°C = 212°F
    """
    # ARRANGE
    temperature = 0
    from_unit = "C"
    to_unit = "F"
    expected_result = 32
    
    # ACT
    result = temperature_converter.invoke({
        "temperature": temperature,
        "from_unit": from_unit,
        "to_unit": to_unit
    })
    
    # ASSERT
    assert "converted" in result, "Result should contain 'converted' key"
    assert result["converted"] == expected_result, f"Expected {expected_result}, but got {result['converted']}"
    assert result["from_unit"] == "C", "Source unit should be 'C'"
    assert result["to_unit"] == "F", "Target unit should be 'F'"
    
    print(f"✓ Test passed: {temperature}°{from_unit} = {result['converted']}°{to_unit}")

# Run the test
test_temperature_converter_c_to_f()
```

    ✓ Test passed: 0°C = 32.0°F


## Part 2: Testing Agent Actions

### Why Agent Testing is Different

When testing agents, we care about:
1. **Tool selection**: Did the agent choose the right tool?
2. **Parameter extraction**: Did the agent pass the correct arguments?
3. **Output quality**: Is the final response reasonable?

We **cannot** expect:
- Exact word-for-word responses (stochastic behavior)
- Same tool calling order every time
- Identical phrasing across runs

### Step 3: Create an Agent

Let's create an agent that can use our tools to answer questions.


```python
# Configure the language model
model = ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0  # Use 0 for more deterministic behavior in tests
)

# Create the agent with tools
tools = [calculator, temperature_converter]
agent = create_agent(
    model,
    tools=tools,
    system_prompt="You are a helpful assistant with access to tools. Use them when needed to answer questions accurately."
)

print("Agent created successfully!")
print(f"Agent has access to {len(tools)} tools: {[tool.name for tool in tools]}")
```

    Agent created successfully!
    Agent has access to 2 tools: ['calculator', 'temperature_converter']


### Test Case 5: Agent Tool Usage - Was the Correct Tool Called?

Our first agent test verifies that the agent selects the appropriate tool for a given query.


```python
def test_agent_uses_calculator():
    """
    Test that the agent correctly identifies when to use the calculator tool.
    
    We verify:
    1. The calculator tool was called
    2. The tool was called at least once
    """
    # ARRANGE
    query = "What is 456 multiplied by 789?"
    
    # ACT
    result = agent.invoke({
        "messages": [{"role": "user", "content": query}]
    })
    
    # ASSERT
    # The result contains messages including tool calls
    messages = result.get("messages", [])
    assert len(messages) > 0, "Agent should return messages"
    
    # Extract tool calls from messages
    tool_calls = []
    for message in messages:
        if hasattr(message, "tool_calls") and message.tool_calls:
            for tool_call in message.tool_calls:
                tool_calls.append(tool_call.get("name"))
    
    # Verify calculator was used
    assert "calculator" in tool_calls, f"Expected calculator to be called, but got tools: {tool_calls}"
    
    print(f"✓ Test passed: Agent correctly used calculator tool")
    print(f"  Tools called: {tool_calls}")
    # Get the final message content
    final_message = messages[-1].content if messages else ""
    print(f"  Final answer: {final_message[:100]}...")

# Run the test
test_agent_uses_calculator()
```

    ✓ Test passed: Agent correctly used calculator tool
      Tools called: ['calculator']
      Final answer: 456 multiplied by 789 is 359,784....


### Test Case 6: Agent Output Validation - Reasonable Response?

We should also verify that the agent's final output is reasonable. Since we can't expect exact text matches, we check for key elements.


```python
def test_agent_output_contains_answer():
    """
    Test that the agent's output contains the expected answer.
    
    We verify:
    1. The output exists and is non-empty
    2. The output contains the correct numerical answer
    3. The output is reasonable (not an error message)
    """
    # ARRANGE
    query = "What is 100 divided by 4?"
    expected_answer = 25  # We know 100 / 4 = 25
    
    # ACT
    result = agent.invoke({
        "messages": [{"role": "user", "content": query}]
    })
    
    # ASSERT
    messages = result.get("messages", [])
    assert len(messages) > 0, "Agent should return messages"
    
    # Get the final message content
    output = messages[-1].content if messages else ""
    
    # Check output exists and is non-empty
    assert output, "Output should not be empty"
    assert len(output) > 0, "Output should contain text"
    
    # Check that the answer appears in the output
    # We convert to string because the agent might format it differently
    assert str(expected_answer) in output or str(float(expected_answer)) in output, \
        f"Expected answer '{expected_answer}' not found in output: {output}"
    
    # Check it's not an error message
    assert "error" not in output.lower(), f"Output contains error: {output}"
    
    print(f"✓ Test passed: Agent provided correct answer")
    print(f"  Query: {query}")
    print(f"  Expected answer: {expected_answer}")
    print(f"  Agent response: {output}")

# Run the test
test_agent_output_contains_answer()
```

    ✓ Test passed: Agent provided correct answer
      Query: What is 100 divided by 4?
      Expected answer: 25
      Agent response: 100 divided by 4 is 25.


### Test Case 7: Comprehensive Agent Test - All Three Aspects

Let's create a comprehensive test that checks tool usage, parameters, and output all together. This combines what we learned from Test Cases 5 and 6.


```python
def test_agent_temperature_conversion_comprehensive():
    """
    Comprehensive test of agent behavior with temperature conversion.
    
    This test verifies all three critical aspects:
    1. Tool Usage: Was temperature_converter called?
    2. Parameters: Were the correct arguments passed?
    3. Output: Does the response contain the expected answer?
    """
    # ARRANGE
    query = "Convert 25 degrees Celsius to Fahrenheit"
    expected_tool = "temperature_converter"
    expected_temp = 25
    expected_from_unit = "C"
    expected_to_unit = "F"
    expected_result = 77  # 25°C = 77°F
    
    # ACT
    result = agent.invoke({
        "messages": [{"role": "user", "content": query}]
    })
    
    # ASSERT - Part 1: Tool Usage
    messages = result.get("messages", [])
    assert len(messages) > 0, "Agent should return messages"
    
    tool_call_found = False
    tool_input = None
    
    for message in messages:
        if hasattr(message, "tool_calls") and message.tool_calls:
            for tool_call in message.tool_calls:
                if tool_call.get("name") == expected_tool:
                    tool_call_found = True
                    tool_input = tool_call.get("args", {})
                    break
    
    assert tool_call_found, f"Expected tool '{expected_tool}' was not called"
    
    print("✓ Part 1 passed: Correct tool was used")
    
    # ASSERT - Part 2: Parameters
    assert tool_input is not None, "Tool input should not be None"
    assert tool_input.get("temperature") == expected_temp, \
        f"Expected temperature={expected_temp} but got {tool_input.get('temperature')}"
    
    # Note: Agent might use lowercase, so we normalize
    from_unit = tool_input.get("from_unit", "")
    to_unit = tool_input.get("to_unit", "")
    assert from_unit.upper() == expected_from_unit, \
        f"Expected from_unit='{expected_from_unit}' but got '{from_unit}'"
    
    assert to_unit.upper() == expected_to_unit, \
        f"Expected to_unit='{expected_to_unit}' but got '{to_unit}'"
    
    print("✓ Part 2 passed: Correct parameters were passed")
    print(f"  Parameters: {tool_input}")
    
    # ASSERT - Part 3: Output Quality
    output = messages[-1].content if messages else ""
    assert output, "Output should not be empty"
    
    # Check that the expected answer appears in the output
    assert str(expected_result) in output or str(float(expected_result)) in output, \
        f"Expected result '{expected_result}' not found in output: {output}"
    
    assert "error" not in output.lower(), f"Output contains error: {output}"
    
    print("✓ Part 3 passed: Output contains correct answer")
    print(f"  Agent response: {output}")
    print("\n✓✓✓ All tests passed: Agent performed complete task correctly!")

# Run the comprehensive test
test_agent_temperature_conversion_comprehensive()
```

    ✓ Part 1 passed: Correct tool was used
    ✓ Part 2 passed: Correct parameters were passed
      Parameters: {'temperature': 25, 'from_unit': 'C', 'to_unit': 'F'}
    ✓ Part 3 passed: Output contains correct answer
      Agent response: 25 degrees Celsius is equal to 77 degrees Fahrenheit.
    
    ✓✓✓ All tests passed: Agent performed complete task correctly!

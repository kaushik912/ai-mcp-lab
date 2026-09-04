# API Error Handling with OpenAI: A Practical Guide

## Overview

When working with external APIs like OpenAI, robust error handling is essential for building reliable applications. This tutorial covers three critical aspects of API error handling:

1. **Timeout Handling** - Managing connection and read timeouts
2. **Fallback Strategies** - Switching to alternative models when services fail
3. **Rate Limiting** - Handling rate limits with exponential backoff

## Prerequisites

- Python 3.8 or higher
- OpenAI Python SDK (`openai` library)
- Valid OpenAI API key
- Basic understanding of try-except error handling in Python

## Learning Objectives

By the end of this notebook, you will:
- Understand different types of API errors and when they occur
- Implement timeout handling for API calls
- Build fallback strategies for service failures
- Apply exponential backoff for rate limit errors
- Create more resilient API integrations

## Common API Errors

When working with OpenAI's API, you'll encounter several types of errors:

- **Timeout Errors**: Occur when the API takes too long to respond (connection timeout) or takes too long to complete (read timeout)
- **Service Errors (5xx)**: HTTP 500, 502, 503 errors indicate temporary service unavailability
- **Rate Limit Errors (429)**: You've exceeded your requests per minute (RPM) or tokens per minute (TPM) quota
- **Authentication Errors (401)**: Invalid or missing API key
- **Bad Request Errors (400)**: Invalid parameters or malformed requests

This tutorial focuses on the first three, which are the most common in production systems.

## Setup: Import Required Libraries

First, let's import all necessary libraries for this tutorial.


```python
import os
import time
from openai import OpenAI
from openai import APIError, RateLimitError, APIConnectionError, APITimeoutError

from dotenv import load_dotenv

load_dotenv()

# Initialize the OpenAI client
# Note: Ensure your OPENAI_API_KEY environment variable is set
# or pass it explicitly: client = OpenAI(api_key="your-key-here")
client = OpenAI()

print("OpenAI client initialized successfully")
```

    OpenAI client initialized successfully


## Helper Function: Basic OpenAI API Call

Let's create a simple helper function that makes API calls to OpenAI. This function will serve as the foundation for our error handling examples.


```python
def call_openai_api(prompt, model="gpt-4o", max_tokens=100, timeout=30):
    """
    Make a simple call to the OpenAI API.
    
    Args:
        prompt (str): The user prompt to send
        model (str): The model to use (default: gpt-4o)
        max_tokens (int): Maximum tokens in response
        timeout (int): Timeout in seconds
    
    Returns:
        str: The API response content
    """
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "user", "content": prompt}
        ],
        max_tokens=max_tokens,
        timeout=timeout
    )
    return response.choices[0].message.content

# Test the helper function
try:
    result = call_openai_api("Say 'Hello, World!' in one another way")
    print(f"API Response: {result}")
except Exception as e:
    print(f"Error: {type(e).__name__}: {e}")
```

    API Response: Greetings, Earth!


---

# Section 1: Timeout Handling

## Understanding Timeouts

Timeouts prevent your application from hanging indefinitely when an API is slow or unresponsive. There are two types of timeouts:

1. **Connection Timeout**: Maximum time to establish a connection with the server
2. **Read Timeout**: Maximum time to wait for a response after the connection is established

The OpenAI SDK allows you to specify a timeout value that applies to the entire request.

## Why Timeouts Matter

- Prevents resource exhaustion in your application
- Improves user experience by failing fast
- Allows you to implement retry logic or fallback strategies
- Essential for production systems with SLAs

## Exercise 1.1: Basic Timeout Handling

Let's demonstrate how to catch and handle timeout errors.


```python
def call_with_timeout_handling(prompt, timeout=10):
    """
    Make an API call with timeout handling.
    
    Args:
        prompt (str): The prompt to send
        timeout (int): Timeout in seconds
    
    Returns:
        tuple: (success: bool, result: str)
    """
    try:
        print(f"Making API call with {timeout}s timeout...")
        start_time = time.time()
        
        result = call_openai_api(prompt, timeout=timeout)
        
        elapsed = time.time() - start_time
        print(f"Success! Completed in {elapsed:.2f}s")
        return True, result
        
    except APITimeoutError as e:
        elapsed = time.time() - start_time
        print(f"Timeout Error after {elapsed:.2f}s: {e}")
        return False, "Request timed out"
    
    except APIConnectionError as e:
        print(f"Connection Error: {e}")
        return False, "Could not connect to API"
    
    except Exception as e:
        print(f"Unexpected Error: {type(e).__name__}: {e}")
        return False, str(e)

# Test with a reasonable timeout
print("=== Test 1: Normal timeout (30s) ===")
success, result = call_with_timeout_handling(
    "What is the capital of France?", 
    timeout=30
)
if success:
    print(f"Result: {result}\n")

# Test with a very short timeout (likely to fail)
print("=== Test 2: Very short timeout (0.001s) ===")
success, result = call_with_timeout_handling(
    "What is the capital of France?", 
    timeout=0.001
)
print(f"Success: {success}, Message: {result}")
```

    === Test 1: Normal timeout (30s) ===
    Making API call with 30s timeout...
    Success! Completed in 0.76s
    Result: The capital of France is Paris.
    
    === Test 2: Very short timeout (0.001s) ===
    Making API call with 0.001s timeout...
    Timeout Error after 1.49s: Request timed out.
    Success: False, Message: Request timed out


## Exercise 1.2: Timeout with Retry Logic

A common pattern is to retry the request with a longer timeout if the first attempt times out.


```python
def call_with_timeout_retry(prompt, initial_timeout=5, max_retries=3):
    """
    Make an API call with progressive timeout increases on retry.
    
    Args:
        prompt (str): The prompt to send
        initial_timeout (int): Starting timeout in seconds
        max_retries (int): Maximum number of retry attempts
    
    Returns:
        tuple: (success: bool, result: str, attempts: int)
    """
    timeout = initial_timeout
    
    for attempt in range(1, max_retries + 1):
        try:
            print(f"Attempt {attempt}/{max_retries} with {timeout}s timeout...")
            result = call_openai_api(prompt, timeout=timeout)
            print(f"Success on attempt {attempt}!")
            return True, result, attempt
            
        except APITimeoutError:
            print(f"Timeout on attempt {attempt}")
            if attempt < max_retries:
                # Double the timeout for next attempt
                timeout *= 2
                print(f"Retrying with {timeout}s timeout...")
            else:
                print("Max retries reached")
                return False, "Request timed out after all retries", attempt
        
        except Exception as e:
            print(f"Non-timeout error: {type(e).__name__}")
            return False, str(e), attempt
    
    return False, "Unexpected exit", max_retries

# Test the retry logic
print("=== Testing timeout with retry logic ===")
success, result, attempts = call_with_timeout_retry(
    "Explain quantum computing in one sentence",
    initial_timeout=10,
    max_retries=3
)

print(f"\nFinal result after {attempts} attempts:")
print(f"Success: {success}")
if success:
    print(f"Response: {result}")
```

    === Testing timeout with retry logic ===
    Attempt 1/3 with 10s timeout...
    Success on attempt 1!
    
    Final result after 1 attempts:
    Success: True
    Response: Quantum computing is a type of computation that harnesses the principles of quantum mechanics, using quantum bits (qubits) to perform operations that can potentially solve complex problems much faster than classical computers.


## Key Takeaways: Timeout Handling

1. Always set reasonable timeouts to prevent hanging requests
2. Catch `Timeout` and `APIConnectionError` exceptions specifically
3. Consider implementing retry logic with progressive timeout increases
4. Balance between user experience (fast failures) and success rate (longer timeouts)
5. Log timeout events for monitoring and debugging

---

# Section 2: Fallback Strategy

## Understanding Service Errors

Service errors (HTTP 500, 502, 503) indicate that the API service is temporarily unavailable. These errors are typically transient and can be caused by:

- Server overload
- Deployment or maintenance
- Infrastructure issues
- Network problems

## Fallback Strategy Pattern

A fallback strategy involves:
1. Attempting to use the primary (preferred) model
2. Detecting service errors
3. Automatically switching to a backup model
4. Logging the fallback for monitoring

Common fallback: `gpt-4` → `gpt-3.5-turbo` (faster and more available)

## Exercise 2.1: Basic Fallback Implementation

Let's implement a function that falls back to an alternative model when the primary model fails.


```python
def call_with_fallback(prompt, primary_model="gpt-4", fallback_model="gpt-3.5-turbo"):
    """
    Call OpenAI API with automatic fallback to alternative model on service errors.
    
    Args:
        prompt (str): The prompt to send
        primary_model (str): Preferred model to try first
        fallback_model (str): Backup model to use if primary fails
    
    Returns:
        tuple: (model_used: str, result: str)
    """
    # Try primary model first
    try:
        print(f"Attempting with primary model: {primary_model}")
        result = call_openai_api(prompt, model=primary_model)
        print(f"Success with {primary_model}!")
        return primary_model, result
        
    except APIError as e:
        # Check if it's a service error (5xx) or model availability issue
        error_code = getattr(e, 'status_code', None)
        
        if error_code and (400 <= error_code < 600):
            print(f"Service error {error_code} with {primary_model}")
            print(f"Falling back to {fallback_model}...")
            
            # Try fallback model
            try:
                result = call_openai_api(prompt, model=fallback_model)
                print(f"Success with fallback model {fallback_model}!")
                return fallback_model, result
                
            except Exception as fallback_error:
                print(f"Fallback also failed: {type(fallback_error).__name__}")
                raise Exception(f"Both primary and fallback models failed") from fallback_error
        else:
            # Not a service error, re-raise
            print(f"Non-service API error (code: {error_code}): {e}")
            raise
    
    except Exception as e:
        print(f"Unexpected error with {primary_model}: {type(e).__name__}")
        raise

# Test 1: Normal operation with real models (should succeed with primary)
print("=== Test 1: Normal operation ===")
try:
    model_used, result = call_with_fallback(
        "What is machine learning in one sentence?",
        primary_model="gpt-4o-mini",
        fallback_model="gpt-3.5-turbo"
    )
    print(f"\nModel used: {model_used}")
    print(f"Response: {result}")
except Exception as e:
    print(f"Final error: {e}")

# Test 2: Invalid model (demonstrates non-fallback error)
print("\n\n=== Test 2: Invalid primary model (404 error - no fallback) ===")
try:
    model_used, result = call_with_fallback(
        "What is machine learning in one sentence?",
        primary_model="gpt-10",  # Invalid model
        fallback_model="gpt-3.5-turbo"
    )
    print(f"\nModel used: {model_used}")
    print(f"Response: {result}")
except Exception as e:
    print(f"Final error: Model not found (404) - fallback not triggered")
    print(f"Note: Fallback only triggers on 5xx service errors, not 404s")

print("\n\n=== Note ===")
print("Service errors (5xx) typically occur during:")
print("- API outages or maintenance")
print("- Server overload situations")
print("- Infrastructure problems")
print("In those cases, the fallback to gpt-3.5-turbo would automatically trigger.")
```

    === Test 1: Normal operation ===
    Attempting with primary model: gpt-4o-mini
    Success with gpt-4o-mini!
    
    Model used: gpt-4o-mini
    Response: Machine learning is a subset of artificial intelligence that enables systems to learn from data and improve their performance on tasks without being explicitly programmed.
    
    
    === Test 2: Invalid primary model (404 error - no fallback) ===
    Attempting with primary model: gpt-10
    Service error 404 with gpt-10
    Falling back to gpt-3.5-turbo...
    Success with fallback model gpt-3.5-turbo!
    
    Model used: gpt-3.5-turbo
    Response: Machine learning is a type of artificial intelligence that allows computers to learn and improve from experience without being explicitly programmed.
    
    
    === Note ===
    Service errors (5xx) typically occur during:
    - API outages or maintenance
    - Server overload situations
    - Infrastructure problems
    In those cases, the fallback to gpt-3.5-turbo would automatically trigger.


---

# Section 3: Rate Limiting and Exponential Backoff

## Understanding Rate Limits

OpenAI enforces rate limits to ensure fair usage across all users. Rate limits are measured in:

- **RPM (Requests Per Minute)**: Total number of API requests
- **TPM (Tokens Per Minute)**: Total tokens (input + output)
- **RPD (Requests Per Day)**: Daily request quota

When you exceed these limits, the API returns an HTTP 429 status code with a `RateLimitError`.

## Exponential Backoff Strategy

Exponential backoff is the recommended approach for handling rate limits:

1. First retry: Wait 1 second
2. Second retry: Wait 2 seconds
3. Third retry: Wait 4 seconds
4. Fourth retry: Wait 8 seconds
5. And so on...

This approach:
- Gives the rate limit time to reset
- Reduces server load
- Increases success rate
- Prevents aggressive retry storms


```python
def call_with_rate_limit_handling(prompt, max_retries=3):
    """
    Make an API call with basic rate limit handling.
    
    Args:
        prompt (str): The prompt to send
        max_retries (int): Maximum retry attempts for rate limits
    
    Returns:
        tuple: (success: bool, result: str, retries: int)
    """
    for attempt in range(max_retries + 1):
        try:
            if attempt > 0:
                print(f"Retry attempt {attempt}/{max_retries}")
            else:
                print("Initial attempt...")
            
            result = call_openai_api(prompt)
            print(f"Success!")
            return True, result, attempt
            
        except RateLimitError as e:
            print(f"Rate limit hit: {e}")
            
            if attempt < max_retries:
                # Simple wait before retry
                wait_time = 2  # Fixed wait time
                print(f"Waiting {wait_time} seconds before retry...")
                time.sleep(wait_time)
            else:
                print("Max retries reached")
                return False, "Rate limit exceeded, max retries reached", attempt
        
        except Exception as e:
            print(f"Non-rate-limit error: {type(e).__name__}: {e}")
            return False, str(e), attempt
    
    return False, "Unexpected exit", max_retries

# Test rate limit handling
print("=== Testing basic rate limit handling ===")
success, result, retries = call_with_rate_limit_handling(
    "Explain cloud computing briefly",
    max_retries=3
)

print(f"\nFinal result:")
print(f"Success: {success}")
print(f"Retries used: {retries}")
if success:
    print(f"Response: {result}")
```

    === Testing basic rate limit handling ===
    Initial attempt...
    Success!
    
    Final result:
    Success: True
    Retries used: 0
    Response: Cloud computing is a model for delivering information technology services where resources such as servers, storage, databases, networking, software, and analytics are provided over the internet ("the cloud"). This model allows users to access and use computing resources on-demand without owning and maintaining physical hardware or infrastructure.
    
    Key characteristics of cloud computing include:
    
    1. **On-demand self-service**: Users can access computing resources as needed automatically, without human intervention from the service provider.
    
    2. **Broad network access**: Cloud services are


## Key Takeaways: Fallback Strategy

1. Always have a fallback plan for critical applications
2. Detect service errors (5xx status codes) specifically for fallback triggers
3. Consider using cheaper/faster models as fallbacks (e.g., gpt-3.5-turbo)
4. Log when fallbacks occur for monitoring and cost analysis


```python
import random

def call_with_exponential_backoff(
    prompt, 
    max_retries=5, 
    base_delay=1, 
    max_delay=60,
    jitter=True
):
    """
    Make an API call with exponential backoff for rate limits.
    
    Args:
        prompt (str): The prompt to send
        max_retries (int): Maximum retry attempts
        base_delay (float): Initial delay in seconds (doubles each retry)
        max_delay (int): Maximum delay cap in seconds
        jitter (bool): Add random jitter to prevent thundering herd
    
    Returns:
        tuple: (success: bool, result: str, attempts: int, total_wait: float)
    """
    total_wait_time = 0
    
    for attempt in range(max_retries + 1):
        try:
            print(f"Attempt {attempt + 1}/{max_retries + 1}...")
            result = call_openai_api(prompt)
            print(f"Success on attempt {attempt + 1}!")
            return True, result, attempt + 1, total_wait_time
            
        except RateLimitError as e:
            print(f"Rate limit error: {e}")
            
            if attempt < max_retries:
                # Calculate exponential backoff
                delay = min(base_delay * (2 ** attempt), max_delay)
                
                # Add jitter to prevent synchronized retries
                if jitter:
                    delay = delay * (0.5 + random.random() * 0.5)
                
                print(f"Backing off for {delay:.2f} seconds...")
                time.sleep(delay)
                total_wait_time += delay
            else:
                print("Max retries exhausted")
                return False, "Rate limit exceeded after all retries", attempt + 1, total_wait_time
        
        except Exception as e:
            print(f"Non-rate-limit error: {type(e).__name__}")
            return False, str(e), attempt + 1, total_wait_time
    
    return False, "Unexpected exit", max_retries + 1, total_wait_time

# Test exponential backoff
print("=== Testing exponential backoff ===")
success, result, attempts, wait_time = call_with_exponential_backoff(
    "What is blockchain technology?",
    max_retries=5,
    base_delay=1,
    max_delay=32,
    jitter=True
)

print(f"\nFinal Statistics:")
print(f"Success: {success}")
print(f"Total attempts: {attempts}")
print(f"Total wait time: {wait_time:.2f}s")
if success:
    print(f"\nResponse: {result}")
```

    === Testing exponential backoff ===
    Attempt 1/6...
    Success on attempt 1!
    
    Final Statistics:
    Success: True
    Total attempts: 1
    Total wait time: 0.00s
    
    Response: Blockchain technology is a decentralized digital ledger system that allows multiple parties to record, verify, and share data securely and transparently without the need for a central authority. It consists of a chain of blocks, where each block contains a list of transactions. These blocks are linked together chronologically and secured using cryptographic principles.
    
    Here are the key features and components of blockchain technology:
    
    1. **Decentralization**: Unlike traditional databases that are controlled by a single entity, a blockchain is maintained across a network


## Exercise 3.4: Simulating Rate Limit Scenarios

Let's create a simulation to understand how exponential backoff behaves under different scenarios.


```python
def simulate_backoff_timing(max_retries=5, base_delay=1, max_delay=60):
    """
    Simulate and visualize exponential backoff timing.
    This doesn't make real API calls - just shows the timing pattern.
    
    Args:
        max_retries (int): Number of retries to simulate
        base_delay (float): Initial delay in seconds
        max_delay (int): Maximum delay cap
    """
    print("Exponential Backoff Simulation")
    print(f"{'='*60}")
    print(f"Base delay: {base_delay}s")
    print(f"Max delay: {max_delay}s")
    print(f"Max retries: {max_retries}")
    print(f"{'='*60}\n")
    
    cumulative_time = 0
    
    print(f"{'Attempt':<10} {'Delay (s)':<15} {'Cumulative (s)':<20}")
    print(f"{'-'*60}")
    
    for attempt in range(max_retries + 1):
        if attempt == 0:
            print(f"{attempt + 1:<10} {'0 (initial)':<15} {cumulative_time:<20.2f}")
        else:
            # Calculate delay
            delay = min(base_delay * (2 ** (attempt - 1)), max_delay)
            cumulative_time += delay
            print(f"{attempt + 1:<10} {delay:<15.2f} {cumulative_time:<20.2f}")
    
    print(f"\nTotal time if all retries needed: {cumulative_time:.2f} seconds")
    print(f"Total time in minutes: {cumulative_time/60:.2f} minutes")

# Run simulations with different parameters
print("\n=== Scenario 1: Standard Configuration ===")
simulate_backoff_timing(max_retries=5, base_delay=1, max_delay=60)

print("\n\n=== Scenario 2: Aggressive Retries (faster backoff) ===")
simulate_backoff_timing(max_retries=5, base_delay=0.5, max_delay=30)

print("\n\n=== Scenario 3: Conservative Retries (slower backoff) ===")
simulate_backoff_timing(max_retries=5, base_delay=2, max_delay=120)
```

    
    === Scenario 1: Standard Configuration ===
    Exponential Backoff Simulation
    ============================================================
    Base delay: 1s
    Max delay: 60s
    Max retries: 5
    ============================================================
    
    Attempt    Delay (s)       Cumulative (s)      
    ------------------------------------------------------------
    1          0 (initial)     0.00                
    2          1.00            1.00                
    3          2.00            3.00                
    4          4.00            7.00                
    5          8.00            15.00               
    6          16.00           31.00               
    
    Total time if all retries needed: 31.00 seconds
    Total time in minutes: 0.52 minutes
    
    
    === Scenario 2: Aggressive Retries (faster backoff) ===
    Exponential Backoff Simulation
    ============================================================
    Base delay: 0.5s
    Max delay: 30s
    Max retries: 5
    ============================================================
    
    Attempt    Delay (s)       Cumulative (s)      
    ------------------------------------------------------------
    1          0 (initial)     0.00                
    2          0.50            0.50                
    3          1.00            1.50                
    4          2.00            3.50                
    5          4.00            7.50                
    6          8.00            15.50               
    
    Total time if all retries needed: 15.50 seconds
    Total time in minutes: 0.26 minutes
    
    
    === Scenario 3: Conservative Retries (slower backoff) ===
    Exponential Backoff Simulation
    ============================================================
    Base delay: 2s
    Max delay: 120s
    Max retries: 5
    ============================================================
    
    Attempt    Delay (s)       Cumulative (s)      
    ------------------------------------------------------------
    1          0 (initial)     0.00                
    2          2.00            2.00                
    3          4.00            6.00                
    4          8.00            14.00               
    5          16.00           30.00               
    6          32.00           62.00               
    
    Total time if all retries needed: 62.00 seconds
    Total time in minutes: 1.03 minutes

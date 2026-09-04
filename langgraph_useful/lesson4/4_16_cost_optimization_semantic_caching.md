# Semantic Caching for LLM Cost Optimization

## Introduction

Welcome to this tutorial on **semantic caching** - one of the most effective strategies for optimizing AI agent costs while maintaining response quality!

### What You'll Learn

In this notebook, you will:
- Understand what semantic caching is and why it's crucial for cost optimization
- Learn the difference between traditional and semantic caching
- Build a semantic cache using embeddings and vector similarity
- Implement a cache-aware agent wrapper
- Measure and calculate real cost savings
- Learn best practices for production deployment

### Prerequisites

- Basic Python knowledge
- Understanding of LLMs and agents
- Familiarity with embeddings (helpful but not required)
- An OpenAI API key

### The Cost Problem

AI agents that use LLMs can be expensive to run at scale:
- GPT-4o mini call: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- High-volume applications: Thousands of requests per day
- Many queries are similar or repetitive

**Example scenario:**
```
User 1: "What's the weather in New York?"
User 2: "Can you tell me the weather in New York?"
User 3: "How's the weather in New York City?"
```

These three queries ask the same thing but have different wording. Traditional caching (exact string match) would miss these opportunities. **Semantic caching solves this!**

### Traditional vs Semantic Caching

| Aspect | Traditional Cache | Semantic Cache |
|--------|------------------|----------------|
| Match Type | Exact string match | Meaning-based match |
| Query: "weather in NYC" | ❌ Miss | ✅ Hit |
| Query: "NYC weather" | ❌ Miss | ✅ Hit |
| Technology | Hash tables | Vector embeddings |
| Hit Rate | 5-10% | 30-50% |

### How Semantic Caching Works

```
┌─────────────────────────────────────────────────────────┐
│  User Query: "What's the weather in NYC?"               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  1. Convert to Embedding Vector                         │
│     [0.23, -0.45, 0.12, ...]                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  2. Search Vector Store for Similar Queries            │
│     Cosine Similarity Threshold: 0.90                  │
└────────────────────┬────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
   ┌─────────────┐      ┌─────────────┐
   │ Similarity  │      │ Similarity  │
   │   ≥ 0.90    │      │   < 0.90    │
   │ (CACHE HIT) │      │ (CACHE MISS)│
   └──────┬──────┘      └──────┬──────┘
          │                     │
          ▼                     ▼
   ┌─────────────┐      ┌─────────────┐
   │ Return      │      │ Call LLM    │
   │ Cached      │      │ Get New     │
   │ Response    │      │ Response    │
   │             │      │ Cache It    │
   └─────────────┘      └─────────────┘
```

### Cost Savings Breakdown

**Per Query Costs:**
- Embedding generation: ~$0.0001 (very cheap!)
- LLM call: ~$0.01-0.03 (100-300x more expensive)
- Cache hit saves: 99%+ of the cost

**Example Calculation:**
- 1000 queries per day
- 40% cache hit rate (400 hits)
- Savings: 400 × $0.02 = $8 per day = $240 per month
- Break-even: After just 2-3 cache hits per unique query

Let's build it!

## 1. Setup and Installation

First, let's install the required packages and set up our environment.


```python
# Install required packages
# Uncomment the line below if you need to install packages
# !pip install langchain langchain-openai langchain-chroma langgraph python-dotenv
```

### Import Libraries and Load Environment

We'll need:
- **LangChain Core**: For tools and base abstractions
- **LangChain OpenAI**: For embeddings and LLM
- **LangChain Chroma**: For vector-based cache storage
- **LangChain Agents**: For creating our weather agent
- **Python standard library**: For time tracking and hashing


```python
import os
import time
from datetime import datetime
from typing import Optional
from dotenv import load_dotenv

# LangChain imports
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_chroma import Chroma
from langchain_core.tools import tool
from langchain_core.documents import Document
from langchain.agents import create_agent

# Load environment variables
load_dotenv()

# Verify API key is loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment variables. Please set it in your .env file.")

print("All libraries imported successfully!")
print("OpenAI API key loaded")
```

    All libraries imported successfully!
    OpenAI API key loaded


## 2. Build a Simple Weather Agent (Without Cache)

Before implementing caching, let's create a simple agent that uses a weather tool. This will be our baseline for comparison.

### Create the Weather Tool

This is a mock tool that returns simulated weather data. In production, you'd call a real weather API.


```python
@tool
def get_weather(location: str) -> str:
    """Get current weather information for a specific location.
    
    Args:
        location: The city or location to get weather for
    
    Returns:
        A string describing the current weather conditions
    """
    # Mock implementation - simulates API call delay
    time.sleep(0.5)  # Simulate API latency
    
    weather_data = {
        "new york": "Sunny, 72°F with light breeze",
        "new york city": "Sunny, 72°F with light breeze",
        "nyc": "Sunny, 72°F with light breeze",
        "london": "Cloudy, 59°F with occasional drizzle",
        "tokyo": "Clear, 68°F with low humidity",
        "san francisco": "Foggy, 62°F with coastal breeze",
        "paris": "Partly cloudy, 65°F",
    }
    
    location_key = location.lower().strip()
    
    if location_key in weather_data:
        return f"Weather in {location}: {weather_data[location_key]}"
    else:
        return f"Weather data not available for {location}"

print("Weather tool created")
print("Available locations: New York, London, Tokyo, San Francisco, Paris")
```

    Weather tool created
    Available locations: New York, London, Tokyo, San Francisco, Paris


### Create the Agent

We'll use LangChain's `create_agent` function to create a simple agent with the weather tool.


```python
# Initialize the LLM
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)

# Create the agent
agent = create_agent(
    model=llm,
    tools=[get_weather]
)

print("Agent created successfully!")
print("The agent can answer weather-related questions using the get_weather tool")
```

    Agent created successfully!
    The agent can answer weather-related questions using the get_weather tool


### Test the Agent (Without Cache)

Let's test our agent with a few queries and time how long they take.


```python
def ask_agent(question: str) -> str:
    """Helper function to ask the agent a question."""
    result = agent.invoke({
        "messages": [{"role": "user", "content": question}]
    })
    return result["messages"][-1].content

# Test with timing
print("Testing agent WITHOUT cache...\n")
print("="*70)

test_queries = [
    "What's the weather in New York?",
    "Can you tell me the weather in New York?",
    "How's the weather in NYC?"
]

for i, query in enumerate(test_queries, 1):
    start = time.time()
    response = ask_agent(query)
    duration = time.time() - start
    
    print(f"\nQuery {i}: {query}")
    print(f"Response: {response}")
    print(f"Time: {duration:.2f}s")
    print("="*70)

print("\nNotice: All three similar queries took similar time and cost!")
print("This is where semantic caching can help.")
```

    Testing agent WITHOUT cache...
    
    ======================================================================
    
    Query 1: What's the weather in New York?
    Response: The weather in New York is sunny, with a temperature of 72°F and a light breeze.
    Time: 3.90s
    ======================================================================
    
    Query 2: Can you tell me the weather in New York?
    Response: The weather in New York is currently sunny, with a temperature of 72°F and a light breeze.
    Time: 2.67s
    ======================================================================
    
    Query 3: How's the weather in NYC?
    Response: The weather in New York City is sunny, with a temperature of 72°F and a light breeze.
    Time: 2.83s
    ======================================================================
    
    Notice: All three similar queries took similar time and cost!
    This is where semantic caching can help.


## 3. Implement Semantic Cache

Now let's build the semantic cache using embeddings and ChromaDB. The cache will store user queries and their corresponding agent responses.

### Initialize Embeddings and Vector Store

We'll use:
- **OpenAI's text-embedding-3-small**: Cost-effective and fast
- **ChromaDB**: In-memory vector store for similarity search


```python
# Initialize embeddings model
embeddings = OpenAIEmbeddings(
    model="text-embedding-3-small"
)

print("Embeddings model initialized: text-embedding-3-small")
print("Cost per 1M tokens: ~$0.02 (very cheap!)")

# Test embeddings
sample_query = "What's the weather in New York?"
sample_embedding = embeddings.embed_query(sample_query)
print(f"\nSample embedding dimensions: {len(sample_embedding)}")
print(f"First 5 values: {sample_embedding[:5]}")
```

    Embeddings model initialized: text-embedding-3-small
    Cost per 1M tokens: ~$0.02 (very cheap!)
    
    Sample embedding dimensions: 1536
    First 5 values: [-0.03764260932803154, -0.020033851265907288, -0.05704335868358612, 0.0393165685236454, 0.0066153560765087605]



```python
# Create vector store for semantic cache
semantic_cache = Chroma(
    collection_name="semantic_cache",
    embedding_function=embeddings,
    # Using in-memory for this tutorial
    # In production, add: persist_directory="./semantic_cache_db"
)

print("Semantic cache initialized!")
print("Vector store: ChromaDB (in-memory)")
print("\nCache structure:")
print("  - Document content: User query text")
print("  - Metadata: {response: agent_response, timestamp: datetime}")
print("  - Automatic embedding: Handled by ChromaDB")
```

    Semantic cache initialized!
    Vector store: ChromaDB (in-memory)
    
    Cache structure:
      - Document content: User query text
      - Metadata: {response: agent_response, timestamp: datetime}
      - Automatic embedding: Handled by ChromaDB


### Understanding the Cache Structure

Our cache stores three pieces of information for each query:

1. **User Query** (as Document content): The actual question asked
2. **Agent Response** (in metadata): The answer the agent provided
3. **Timestamp** (in metadata): When this was cached

**Important:** We only cache the user's input query, NOT system prompts or tool descriptions. This ensures we're matching on what the user actually asked.

When a new query comes in:
1. We embed the query → convert to vector
2. Search for similar cached queries → cosine similarity
3. If similarity ≥ threshold (0.90) → return cached response
4. If similarity < threshold → call agent and cache the new response

## 4. Create Cache-Aware Agent Wrapper

Now let's create a wrapper function that checks the cache before calling the agent.

### Define the Similarity Threshold

The threshold determines how similar queries must be to count as a cache hit:
- **0.95+**: Very strict, fewer false positives, lower hit rate
- **0.90-0.95**: Balanced (recommended for most use cases)
- **0.80-0.90**: Looser, higher hit rate, some risk of false positives


```python
# Configuration
SIMILARITY_THRESHOLD = 0.90
CACHE_TTL_HOURS = 24  # Time-to-live for cache entries

print(f"Cache Configuration:")
print(f"  Similarity Threshold: {SIMILARITY_THRESHOLD}")
print(f"  Cache TTL: {CACHE_TTL_HOURS} hours")
print(f"\nThreshold guide:")
print(f"  0.95+ : Very precise, fewer cache hits")
print(f"  0.90-0.95: Balanced (recommended)")
print(f"  0.80-0.90: More cache hits, some false positives")
```

    Cache Configuration:
      Similarity Threshold: 0.9
      Cache TTL: 24 hours
    
    Threshold guide:
      0.95+ : Very precise, fewer cache hits
      0.90-0.95: Balanced (recommended)
      0.80-0.90: More cache hits, some false positives


### Implement the Cached Agent Function

This function:
1. Takes a user query
2. Searches the cache for similar queries
3. Returns cached response if similarity ≥ threshold
4. Otherwise calls the agent and caches the new response


```python
def ask_agent_with_cache(question: str, verbose: bool = True) -> tuple[str, bool, float, float]:
    """
    Ask the agent a question with semantic caching.
    
    Args:
        question: The user's question
        verbose: If True, print cache hit/miss information
    
    Returns:
        tuple: (response, cache_hit, similarity_score, duration)
    """
    start_time = time.time()
    
    if verbose:
        print(f"\n{'='*70}")
        print(f"Query: {question}")
        print(f"{'='*70}")
    
    # Step 1: Search cache for similar queries
    # similarity_search_with_score returns (Document, similarity_score) tuples
    results = semantic_cache.similarity_search_with_score(
        query=question,
        k=1  # Get the most similar cached query
    )
    
    cache_hit = False
    similarity_score = 0.0
    
    # Step 2: Check if we have a cache hit
    if results:
        cached_doc, distance = results[0]
        # ChromaDB returns L2 distance, convert to cosine similarity
        # For normalized vectors: cosine_similarity = 1 - (distance^2 / 2)
        similarity_score = 1 - (distance ** 2 / 2)
        
        if verbose:
            print(f"\nCache Search:")
            print(f"  Most similar cached query: '{cached_doc.page_content}'")
            print(f"  Similarity score: {similarity_score:.4f}")
            print(f"  Threshold: {SIMILARITY_THRESHOLD}")
        
        # Step 3: Cache hit - return cached response
        if similarity_score >= SIMILARITY_THRESHOLD:
            cache_hit = True
            response = cached_doc.metadata["response"]
            cached_time = cached_doc.metadata.get("timestamp", "unknown")
            
            duration = time.time() - start_time
            
            if verbose:
                print(f"\n  ✅ CACHE HIT!")
                print(f"  Cached at: {cached_time}")
                print(f"  Cost savings: ~99% (embedding vs LLM call)")
                print(f"  Response time: {duration:.2f}s")
                print(f"\nResponse: {response}")
            
            return response, cache_hit, similarity_score, duration
    
    # Step 4: Cache miss - call agent and cache the response
    if verbose:
        print(f"\n  ❌ CACHE MISS (similarity: {similarity_score:.4f} < {SIMILARITY_THRESHOLD})")
        print(f"  Calling agent...")
    
    # Call the agent
    response = ask_agent(question)
    
    # Cache the new query-response pair
    cache_doc = Document(
        page_content=question,  # Store the user query
        metadata={
            "response": response,
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
    )
    semantic_cache.add_documents([cache_doc])
    
    duration = time.time() - start_time
    
    if verbose:
        print(f"  Cached new response for future queries")
        print(f"  Response time: {duration:.2f}s")
        print(f"\nResponse: {response}")
    
    return response, cache_hit, similarity_score, duration

print("Cache-aware agent wrapper created!")
print("Ready to demonstrate semantic caching.")
```

    Cache-aware agent wrapper created!
    Ready to demonstrate semantic caching.


## 5. Demonstrate Semantic Caching

Now let's test our semantic cache with similar queries and see the cost savings!

### Test 1: Similar Queries About New York Weather

We'll ask three different ways of asking about New York weather. The first query will be a cache miss, but the subsequent similar queries should be cache hits.


```python
print("TEST 1: Similar queries about New York weather\n")

test_queries = [
    "What's the weather in New York?",
    "Can you tell me the weather in New York?",
    "How's the weather in NYC?",
    "What's the weather like in New York City?"
]

results = []

for query in test_queries:
    response, cache_hit, similarity, duration = ask_agent_with_cache(query)
    results.append({
        "query": query,
        "cache_hit": cache_hit,
        "similarity": similarity,
        "duration": duration
    })
    time.sleep(0.5)  # Small delay between queries

print("\n" + "="*70)
print("SUMMARY")
print("="*70)
```

    TEST 1: Similar queries about New York weather
    
    
    ======================================================================
    Query: What's the weather in New York?
    ======================================================================
    
      ❌ CACHE MISS (similarity: 0.0000 < 0.9)
      Calling agent...
      Cached new response for future queries
      Response time: 6.87s
    
    Response: The weather in New York is sunny, with a temperature of 72°F and a light breeze.
    
    ======================================================================
    Query: Can you tell me the weather in New York?
    ======================================================================
    
    Cache Search:
      Most similar cached query: 'What's the weather in New York?'
      Similarity score: 0.9821
      Threshold: 0.9
    
      ✅ CACHE HIT!
      Cached at: 2025-11-15 17:45:25
      Cost savings: ~99% (embedding vs LLM call)
      Response time: 0.51s
    
    Response: The weather in New York is sunny, with a temperature of 72°F and a light breeze.
    
    ======================================================================
    Query: How's the weather in NYC?
    ======================================================================
    
    Cache Search:
      Most similar cached query: 'What's the weather in New York?'
      Similarity score: 0.9675
      Threshold: 0.9
    
      ✅ CACHE HIT!
      Cached at: 2025-11-15 17:45:25
      Cost savings: ~99% (embedding vs LLM call)
      Response time: 0.42s
    
    Response: The weather in New York is sunny, with a temperature of 72°F and a light breeze.
    
    ======================================================================
    Query: What's the weather like in New York City?
    ======================================================================
    
    Cache Search:
      Most similar cached query: 'What's the weather in New York?'
      Similarity score: 0.9917
      Threshold: 0.9
    
      ✅ CACHE HIT!
      Cached at: 2025-11-15 17:45:25
      Cost savings: ~99% (embedding vs LLM call)
      Response time: 0.52s
    
    Response: The weather in New York is sunny, with a temperature of 72°F and a light breeze.
    
    ======================================================================
    SUMMARY
    ======================================================================



```python
# Display summary table
print(f"\n{'Query':<50} {'Hit?':<8} {'Similarity':<12} {'Time':<8}")
print("="*80)

for result in results:
    hit_symbol = "✅" if result["cache_hit"] else "❌"
    print(f"{result['query']:<50} {hit_symbol:<8} {result['similarity']:<12.4f} {result['duration']:<8.2f}s")

# Calculate statistics
total_queries = len(results)
cache_hits = sum(1 for r in results if r["cache_hit"])
cache_hit_rate = (cache_hits / total_queries) * 100
avg_hit_time = sum(r["duration"] for r in results if r["cache_hit"]) / max(cache_hits, 1)
avg_miss_time = sum(r["duration"] for r in results if not r["cache_hit"]) / (total_queries - cache_hits)

print(f"\nStatistics:")
print(f"  Total queries: {total_queries}")
print(f"  Cache hits: {cache_hits}")
print(f"  Cache hit rate: {cache_hit_rate:.1f}%")
print(f"  Avg time (cache hit): {avg_hit_time:.2f}s")
print(f"  Avg time (cache miss): {avg_miss_time:.2f}s")
print(f"  Speedup: {avg_miss_time/avg_hit_time:.1f}x faster with cache")
```

    
    Query                                              Hit?     Similarity   Time    
    ================================================================================
    What's the weather in New York?                    ❌        0.0000       6.87    s
    Can you tell me the weather in New York?           ✅        0.9821       0.51    s
    How's the weather in NYC?                          ✅        0.9675       0.42    s
    What's the weather like in New York City?          ✅        0.9917       0.52    s
    
    Statistics:
      Total queries: 4
      Cache hits: 3
      Cache hit rate: 75.0%
      Avg time (cache hit): 0.48s
      Avg time (cache miss): 6.87s
      Speedup: 14.2x faster with cache


### Test 2: Different Location (Cache Miss Expected)

Now let's ask about a different location. This should be a cache miss because it's semantically different from the New York queries.


```python
print("\n\nTEST 2: Different location (Tokyo)\n")

# This should be a cache miss
response, cache_hit, similarity, duration = ask_agent_with_cache(
    "What's the weather in Tokyo?"
)

print("\nThis query is semantically different from the New York queries,")
print("so it correctly results in a cache miss.")
```

    
    
    TEST 2: Different location (Tokyo)
    
    
    ======================================================================
    Query: What's the weather in Tokyo?
    ======================================================================
    
    Cache Search:
      Most similar cached query: 'What's the weather in New York?'
      Similarity score: 0.6172
      Threshold: 0.9
    
      ❌ CACHE MISS (similarity: 0.6172 < 0.9)
      Calling agent...
      Cached new response for future queries
      Response time: 4.57s
    
    Response: The weather in Tokyo is clear, with a temperature of 68°F and low humidity.
    
    This query is semantically different from the New York queries,
    so it correctly results in a cache miss.



```python
# Now ask about Tokyo again with different wording
print("\n\nAsking about Tokyo again with different wording...\n")

response, cache_hit, similarity, duration = ask_agent_with_cache(
    "Can you tell me the weather in Tokyo?"
)

print("\nThis time we got a cache hit because it's similar to the previous Tokyo query!")
```

    
    
    Asking about Tokyo again with different wording...
    
    
    ======================================================================
    Query: Can you tell me the weather in Tokyo?
    ======================================================================
    
    Cache Search:
      Most similar cached query: 'What's the weather in Tokyo?'
      Similarity score: 0.9854
      Threshold: 0.9
    
      ✅ CACHE HIT!
      Cached at: 2025-11-15 17:45:49
      Cost savings: ~99% (embedding vs LLM call)
      Response time: 1.23s
    
    Response: The weather in Tokyo is clear, with a temperature of 68°F and low humidity.
    
    This time we got a cache hit because it's similar to the previous Tokyo query!


## 6. Calculate Cost Savings

Let's calculate the actual cost savings from semantic caching with real numbers.

### Cost Model

Based on OpenAI pricing (as of January 2025):
- **GPT-4o mini**: $0.150 per 1M input tokens, $0.600 per 1M output tokens
- **text-embedding-3-small**: $0.020 per 1M tokens
- **Average LLM call**: ~500 input tokens + ~100 output tokens
- **Average embedding**: ~50 tokens

Let's calculate the savings:


```python
# Cost parameters (in dollars)
GPT4O_MINI_INPUT_COST_PER_1M = 0.150
GPT4O_MINI_OUTPUT_COST_PER_1M = 0.600
EMBEDDING_COST_PER_1M = 0.020

# Average token counts
AVG_INPUT_TOKENS_PER_QUERY = 500
AVG_OUTPUT_TOKENS_PER_QUERY = 100
AVG_EMBEDDING_TOKENS_PER_QUERY = 50

# Calculate cost per query
cost_per_llm_call = (
    (AVG_INPUT_TOKENS_PER_QUERY / 1_000_000) * GPT4O_MINI_INPUT_COST_PER_1M +
    (AVG_OUTPUT_TOKENS_PER_QUERY / 1_000_000) * GPT4O_MINI_OUTPUT_COST_PER_1M
)

cost_per_embedding = (
    (AVG_EMBEDDING_TOKENS_PER_QUERY / 1_000_000) * EMBEDDING_COST_PER_1M
)

savings_per_cache_hit = cost_per_llm_call - cost_per_embedding

print("COST ANALYSIS")
print("="*70)
print(f"\nPer Query Costs:")
print(f"  LLM call (GPT-4o mini): ${cost_per_llm_call:.6f}")
print(f"  Embedding lookup: ${cost_per_embedding:.6f}")
print(f"  Savings per cache hit: ${savings_per_cache_hit:.6f} ({(savings_per_cache_hit/cost_per_llm_call)*100:.1f}%)")
print(f"\nCost breakdown:")
print(f"  Cache hit is {cost_per_llm_call/cost_per_embedding:.0f}x cheaper than LLM call!")
```

    COST ANALYSIS
    ======================================================================
    
    Per Query Costs:
      LLM call (GPT-4o mini): $0.000135
      Embedding lookup: $0.000001
      Savings per cache hit: $0.000134 (99.3%)
    
    Cost breakdown:
      Cache hit is 135x cheaper than LLM call!


### Projected Savings for Different Scenarios

Let's calculate savings for different usage patterns:


```python
def calculate_monthly_savings(queries_per_day: int, cache_hit_rate: float) -> dict:
    """
    Calculate monthly cost savings from semantic caching.
    
    Args:
        queries_per_day: Number of queries per day
        cache_hit_rate: Cache hit rate (0.0 to 1.0)
    
    Returns:
        Dictionary with cost breakdown
    """
    queries_per_month = queries_per_day * 30
    cache_hits = queries_per_month * cache_hit_rate
    cache_misses = queries_per_month * (1 - cache_hit_rate)
    
    # Cost without caching
    cost_without_cache = queries_per_month * cost_per_llm_call
    
    # Cost with caching
    cost_with_cache = (
        cache_hits * cost_per_embedding +  # Cache hits: only embedding cost
        cache_misses * (cost_per_llm_call + cost_per_embedding)  # Misses: LLM + embedding
    )
    
    monthly_savings = cost_without_cache - cost_with_cache
    savings_percentage = (monthly_savings / cost_without_cache) * 100
    
    return {
        "queries_per_month": queries_per_month,
        "cache_hits": int(cache_hits),
        "cache_misses": int(cache_misses),
        "cost_without_cache": cost_without_cache,
        "cost_with_cache": cost_with_cache,
        "monthly_savings": monthly_savings,
        "savings_percentage": savings_percentage,
        "annual_savings": monthly_savings * 12
    }

# Calculate for different scenarios
scenarios = [
    {"name": "Small App", "queries": 1000, "hit_rate": 0.30},
    {"name": "Medium App", "queries": 10000, "hit_rate": 0.40},
    {"name": "Large App", "queries": 100000, "hit_rate": 0.50},
]

print("\nMONTHLY SAVINGS PROJECTIONS")
print("="*70)

for scenario in scenarios:
    result = calculate_monthly_savings(scenario["queries"], scenario["hit_rate"])
    
    print(f"\n{scenario['name']} ({scenario['queries']:,} queries/day, {scenario['hit_rate']*100:.0f}% hit rate):")
    print(f"  Monthly queries: {result['queries_per_month']:,}")
    print(f"  Cache hits: {result['cache_hits']:,}")
    print(f"  Cost without cache: ${result['cost_without_cache']:.2f}/month")
    print(f"  Cost with cache: ${result['cost_with_cache']:.2f}/month")
    print(f"  Monthly savings: ${result['monthly_savings']:.2f} ({result['savings_percentage']:.1f}%)")
    print(f"  Annual savings: ${result['annual_savings']:.2f}")

print("\n" + "="*70)
print("Note: These calculations use average token counts and may vary in practice.")
```

    
    MONTHLY SAVINGS PROJECTIONS
    ======================================================================
    
    Small App (1,000 queries/day, 30% hit rate):
      Monthly queries: 30,000
      Cache hits: 9,000
      Cost without cache: $4.05/month
      Cost with cache: $2.86/month
      Monthly savings: $1.19 (29.3%)
      Annual savings: $14.22
    
    Medium App (10,000 queries/day, 40% hit rate):
      Monthly queries: 300,000
      Cache hits: 120,000
      Cost without cache: $40.50/month
      Cost with cache: $24.60/month
      Monthly savings: $15.90 (39.3%)
      Annual savings: $190.80
    
    Large App (100,000 queries/day, 50% hit rate):
      Monthly queries: 3,000,000
      Cache hits: 1,500,000
      Cost without cache: $405.00/month
      Cost with cache: $205.50/month
      Monthly savings: $199.50 (49.3%)
      Annual savings: $2394.00
    
    ======================================================================
    Note: These calculations use average token counts and may vary in practice.

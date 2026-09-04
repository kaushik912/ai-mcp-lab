# Performance Metrics Tracking for AI Agents

This notebook demonstrates how to track key performance metrics when making calls to OpenAI using LangChain:
- **Latency**: Response time
- **Token Usage**: Input/output tokens
- **Cost**: API expenses
- **Success Rate**: Request success/failure tracking

We'll compare the latest GPT model family:
- **GPT-5**: Best for coding and agentic tasks
- **GPT-5 mini**: Faster, cheaper version for well-defined tasks
- **GPT-5 nano**: Fastest, cheapest for summarization and classification
- **GPT-4o**: Previous generation flagship model

## Setup


```python
import os
import time
from datetime import datetime
from typing import Dict, List

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
```


```python
from dotenv import load_dotenv

# Load environment variables
load_dotenv()
```




    True




```python
# Example: Initialize a single LLM
# We'll initialize all models in the comparison example below
llm = ChatOpenAI(
    model="gpt-5-nano",  # Start with the most cost-effective option
    temperature=0.7
)
```

## Metrics Tracking Class


```python
class PerformanceMetrics:
    """Track performance metrics for LLM calls"""
    
    # OpenAI pricing (as of 2025, in USD per 1K tokens)
    # Note: Cached input pricing is not tracked in this simple implementation
    PRICING = {
        "gpt-5": {"input": 0.00125, "output": 0.01},
        "gpt-5-mini": {"input": 0.00025, "output": 0.002},
        "gpt-5-nano": {"input": 0.00005, "output": 0.0004},
        "gpt-4o": {"input": 0.0025, "output": 0.01},
        "gpt-4": {"input": 0.03, "output": 0.06},
        "gpt-4-turbo": {"input": 0.01, "output": 0.03},
        "gpt-3.5-turbo": {"input": 0.0005, "output": 0.0015}
    }
    
    def __init__(self):
        self.metrics: List[Dict] = []
        
    def track_call(self, model: str, prompt: str, llm: ChatOpenAI) -> Dict:
        """Track a single LLM call and return metrics"""
        start_time = time.time()
        
        try:
            # Make the LLM call with callbacks to capture token usage
            response = llm.invoke([HumanMessage(content=prompt)])
            
            end_time = time.time()
            latency = end_time - start_time
            
            # Extract token usage from response metadata
            usage_metadata = response.response_metadata.get('token_usage', {})
            input_tokens = usage_metadata.get('prompt_tokens', 0)
            output_tokens = usage_metadata.get('completion_tokens', 0)
            total_tokens = usage_metadata.get('total_tokens', 0)
            
            # Calculate cost - use gpt-4o as fallback if model not found
            pricing = self.PRICING.get(model, self.PRICING["gpt-4o"])
            input_cost = (input_tokens / 1000) * pricing["input"]
            output_cost = (output_tokens / 1000) * pricing["output"]
            total_cost = input_cost + output_cost
            
            metric = {
                "timestamp": datetime.now().isoformat(),
                "model": model,
                "latency_ms": round(latency * 1000, 2),
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "total_tokens": total_tokens,
                "input_cost_usd": round(input_cost, 6),
                "output_cost_usd": round(output_cost, 6),
                "total_cost_usd": round(total_cost, 6),
                "success": True,
                "response": response.content
            }
            
        except Exception as e:
            end_time = time.time()
            latency = end_time - start_time
            
            metric = {
                "timestamp": datetime.now().isoformat(),
                "model": model,
                "latency_ms": round(latency * 1000, 2),
                "input_tokens": 0,
                "output_tokens": 0,
                "total_tokens": 0,
                "input_cost_usd": 0,
                "output_cost_usd": 0,
                "total_cost_usd": 0,
                "success": False,
                "error": str(e)
            }
        
        self.metrics.append(metric)
        return metric
    
    def get_summary(self) -> Dict:
        """Calculate summary statistics across all tracked calls"""
        if not self.metrics:
            return {}
        
        successful_calls = [m for m in self.metrics if m["success"]]
        total_calls = len(self.metrics)
        
        return {
            "total_calls": total_calls,
            "successful_calls": len(successful_calls),
            "failed_calls": total_calls - len(successful_calls),
            "success_rate": round(len(successful_calls) / total_calls * 100, 2) if total_calls > 0 else 0,
            "avg_latency_ms": round(sum(m["latency_ms"] for m in successful_calls) / len(successful_calls), 2) if successful_calls else 0,
            "total_tokens": sum(m["total_tokens"] for m in successful_calls),
            "total_cost_usd": round(sum(m["total_cost_usd"] for m in successful_calls), 6)
        }
    
    def print_metric(self, metric: Dict):
        """Pretty print a single metric"""
        print(f"\n{'='*60}")
        print(f"Timestamp: {metric['timestamp']}")
        print(f"Model: {metric['model']}")
        print(f"Success: {metric['success']}")
        print(f"Latency: {metric['latency_ms']} ms")
        
        if metric['success']:
            print(f"\nToken Usage:")
            print(f"  Input: {metric['input_tokens']}")
            print(f"  Output: {metric['output_tokens']}")
            print(f"  Total: {metric['total_tokens']}")
            print(f"\nCost:")
            print(f"  Input: ${metric['input_cost_usd']:.6f}")
            print(f"  Output: ${metric['output_cost_usd']:.6f}")
            print(f"  Total: ${metric['total_cost_usd']:.6f}")
            print(f"\nResponse: {metric['response'][:100]}..." if len(metric['response']) > 100 else f"\nResponse: {metric['response']}")
        else:
            print(f"Error: {metric.get('error', 'Unknown error')}")
        
        print(f"{'='*60}\n")
    
    def print_summary(self):
        """Pretty print summary statistics"""
        summary = self.get_summary()
        
        print(f"\n{'='*60}")
        print("PERFORMANCE METRICS SUMMARY")
        print(f"{'='*60}")
        print(f"Total Calls: {summary['total_calls']}")
        print(f"Successful: {summary['successful_calls']}")
        print(f"Failed: {summary['failed_calls']}")
        print(f"Success Rate: {summary['success_rate']}%")
        print(f"\nAverage Latency: {summary['avg_latency_ms']} ms")
        print(f"Total Tokens Used: {summary['total_tokens']}")
        print(f"Total Cost: ${summary['total_cost_usd']:.6f}")
        print(f"{'='*60}\n")
```

## Example: Comparing Model Family

We'll test the same prompt across all models to compare their performance and cost characteristics.


```python
# Initialize metrics tracker
metrics = PerformanceMetrics()

# Dynamically initialize LLMs for each model
model_names = ["gpt-5", "gpt-5-mini", "gpt-5-nano", "gpt-4o"]
models = {
    model_name: ChatOpenAI(model=model_name, temperature=0.7) 
    for model_name in model_names
}

# Test prompt - a coding task suitable for comparison
test_prompt = """Write a Python function that takes a list of numbers and returns 
a dictionary with the following statistics: mean, median, min, max, and standard deviation. 
Include proper error handling."""
```


```python
# Run the same prompt across all three models
results = {}

for model_name, llm in models.items():
    print(f"\n{'='*60}")
    print(f"Testing {model_name.upper()}")
    print(f"{'='*60}")
    
    metric = metrics.track_call(model_name, test_prompt, llm)
    results[model_name] = metric
    metrics.print_metric(metric)
```

    
    ============================================================
    Testing GPT-5
    ============================================================
    
    ============================================================
    Timestamp: 2025-11-07T15:59:36.752275
    Model: gpt-5
    Success: True
    Latency: 29432.39 ms
    
    Token Usage:
      Input: 44
      Output: 1966
      Total: 2010
    
    Cost:
      Input: $0.000055
      Output: $0.019660
      Total: $0.019715
    
    Response: Here’s a robust Python function with validation and clear errors. It computes mean, median, min, max...
    ============================================================
    
    
    ============================================================
    Testing GPT-5-MINI
    ============================================================
    
    ============================================================
    Timestamp: 2025-11-07T16:00:07.984484
    Model: gpt-5-mini
    Success: True
    Latency: 31231.56 ms
    
    Token Usage:
      Input: 44
      Output: 1872
      Total: 1916
    
    Cost:
      Input: $0.000011
      Output: $0.003744
      Total: $0.003755
    
    Response: Here's a concise, robust Python function that computes mean, median, min, max, and (population) stan...
    ============================================================
    
    
    ============================================================
    Testing GPT-5-NANO
    ============================================================
    
    ============================================================
    Timestamp: 2025-11-07T16:00:35.352503
    Model: gpt-5-nano
    Success: True
    Latency: 27366.72 ms
    
    Token Usage:
      Input: 44
      Output: 3447
      Total: 3491
    
    Cost:
      Input: $0.000002
      Output: $0.001379
      Total: $0.001381
    
    Response: Here's a robust Python function that computes mean, median, min, max, and standard deviation for a l...
    ============================================================
    
    
    ============================================================
    Testing GPT-4O
    ============================================================
    
    ============================================================
    Timestamp: 2025-11-07T16:00:44.671063
    Model: gpt-4o
    Success: True
    Latency: 9318.33 ms
    
    Token Usage:
      Input: 45
      Output: 524
      Total: 569
    
    Cost:
      Input: $0.000112
      Output: $0.005240
      Total: $0.005353
    
    Response: To achieve the task of calculating statistics from a list of numbers and returning them as a diction...
    ============================================================
    


## Model Comparison Table

Let's create a comprehensive comparison table to visualize the performance and cost differences across the model family.


```python
# Create comparison table
comparison_data = []
for model_name, metric in results.items():
    comparison_data.append({
        "Model": model_name,
        "Latency (ms)": metric["latency_ms"],
        "Input Tokens": metric["input_tokens"],
        "Output Tokens": metric["output_tokens"],
        "Total Tokens": metric["total_tokens"],
        "Total Cost ($)": f"${metric['total_cost_usd']:.6f}",
        "Cost per Token ($)": f"${metric['total_cost_usd']/metric['total_tokens']:.8f}" if metric['total_tokens'] > 0 else "$0"
    })

# Display comparison table
print("\n" + "="*100)
print("MODEL FAMILY COMPARISON")
print("="*100)

# Table header
header = f"{'Model':<15} {'Latency (ms)':<15} {'Input Tokens':<15} {'Output Tokens':<15} {'Total Tokens':<15} {'Total Cost ($)':<18} {'Cost per Token ($)':<20}"
print(header)
print("-" * 100)

# Table rows
for row in comparison_data:
    print(f"{row['Model']:<15} {str(row['Latency (ms)']):<15} {str(row['Input Tokens']):<15} {str(row['Output Tokens']):<15} {str(row['Total Tokens']):<15} {row['Total Cost ($)']:<18} {row['Cost per Token ($)']:<20}")

print("="*100)
```

    
    ====================================================================================================
    MODEL FAMILY COMPARISON
    ====================================================================================================
    Model           Latency (ms)    Input Tokens    Output Tokens   Total Tokens    Total Cost ($)     Cost per Token ($)  
    ----------------------------------------------------------------------------------------------------
    gpt-5           29432.39        44              1966            2010            $0.019715          $0.00000981         
    gpt-5-mini      31231.56        44              1872            1916            $0.003755          $0.00000196         
    gpt-5-nano      27366.72        44              3447            3491            $0.001381          $0.00000040         
    gpt-4o          9318.33         45              524             569             $0.005353          $0.00000941         
    ====================================================================================================

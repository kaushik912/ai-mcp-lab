# Define and Manage State in a LangGraph Workflow

## Introduction

State is the shared data structure that flows through your LangGraph workflow. Each node reads from and writes to this state, enabling communication between different parts of your graph.

In this notebook, you'll learn how to:

- Define state using Pydantic for validation
- Use **reducers** to control how state updates are applied
- Use `operator.add` to accumulate items in a list
- Use the default (overwrite) behavior for fields that should be replaced

**Prerequisites**: Familiarity with basic LangGraph concepts (nodes, edges, graphs)

## Step 1: Import Required Libraries


```python
from typing import Annotated
from operator import add
from pydantic import BaseModel, Field
from langgraph.graph import StateGraph, START, END
```

## Step 2: Define State with Pydantic

We'll build a document analysis pipeline that processes a paragraph through multiple analysis nodes. Our state needs:

- `document`: The input text (doesn't change during processing)
- `findings`: A list that **accumulates** results from each node
- `status`: The current processing stage (gets **overwritten** by each node)

### Understanding Reducers

A **reducer** determines how state updates are applied:

- **Default (overwrite)**: New values replace old values
- **`operator.add`**: New list items are appended to existing items

Use `Annotated[type, reducer]` to specify a reducer for a field.


```python
class AnalysisState(BaseModel):
    """State for document analysis pipeline."""
    
    # Input document - no reducer needed, stays constant
    document: str = ""
    
    # Findings accumulate from each node using the add reducer
    findings: Annotated[list[str], add] = Field(default_factory=list)
    
    # Status gets overwritten by each node (default behavior)
    status: str = "pending"
```

## Step 3: Create Analysis Nodes

Each node performs a specific analysis task and returns its findings. Notice that:

- Each node returns `findings` as a list - these will be **appended** to existing findings
- Each node returns `status` as a string - this will **overwrite** the previous status


```python
def extract_keywords(state: AnalysisState) -> dict:
    """Extract keywords from the document."""
    doc = state.document.lower()
    
    keywords_found = []
    keyword_list = ["ai", "machine learning", "data", "python", "automation"]
    
    for keyword in keyword_list:
        if keyword in doc:
            keywords_found.append(f"Keyword found: '{keyword}'")
    
    if not keywords_found:
        keywords_found.append("No target keywords found")
    
    return {
        "findings": keywords_found,  # Will be APPENDED
        "status": "keywords_extracted"  # Will OVERWRITE
    }
```


```python
def analyze_sentiment(state: AnalysisState) -> dict:
    """Analyze the sentiment of the document."""
    doc = state.document.lower()
    
    positive_words = ["great", "excellent", "amazing", "innovative", "powerful", "efficient"]
    negative_words = ["bad", "poor", "terrible", "difficult", "complex", "slow"]
    
    positive_count = sum(1 for word in positive_words if word in doc)
    negative_count = sum(1 for word in negative_words if word in doc)
    
    if positive_count > negative_count:
        sentiment = "Sentiment: Positive"
    elif negative_count > positive_count:
        sentiment = "Sentiment: Negative"
    else:
        sentiment = "Sentiment: Neutral"
    
    return {
        "findings": [sentiment],  # Will be APPENDED
        "status": "sentiment_analyzed"  # Will OVERWRITE
    }
```


```python
def generate_stats(state: AnalysisState) -> dict:
    """Generate document statistics."""
    doc = state.document
    
    word_count = len(doc.split())
    sentence_count = doc.count('.') + doc.count('!') + doc.count('?')
    
    summary = [
        f"Word count: {word_count}",
        f"Sentence count: {sentence_count}"
    ]
    
    return {
        "findings": summary,  # Will be APPENDED
        "status": "complete"  # Will OVERWRITE
    }
```

## Step 4: Build and Compile the Graph


```python
# Create the graph builder
builder = StateGraph(AnalysisState)

# Add nodes
builder.add_node("extract_keywords", extract_keywords)
builder.add_node("analyze_sentiment", analyze_sentiment)
builder.add_node("generate_stats", generate_stats)

# Define the flow
builder.add_edge(START, "extract_keywords")
builder.add_edge("extract_keywords", "analyze_sentiment")
builder.add_edge("analyze_sentiment", "generate_stats")
builder.add_edge("generate_stats", END)

# Compile
graph = builder.compile()

print("Graph compiled successfully!")
```

    Graph compiled successfully!


## Step 5: Run the Analysis Pipeline

Let's analyze a sample paragraph about AI and observe how:
- `findings` accumulates results from all three nodes
- `status` shows only the final status (overwritten by each node)


```python
sample_document = """
Artificial Intelligence and machine learning are transforming how businesses operate. 
Python has become the go-to language for data science and AI development due to its 
excellent libraries and easy syntax. Companies are using automation to streamline 
their workflows and achieve great results. The future of AI looks incredibly promising.
"""

# Run the pipeline
result = graph.invoke({"document": sample_document})

# Display results
print("=" * 50)
print("DOCUMENT ANALYSIS RESULTS")
print("=" * 50)
print(f"\nFinal Status: {result['status']}")
print(f"\nFindings (accumulated from all nodes):")
for i, finding in enumerate(result['findings'], 1):
    print(f"  {i}. {finding}")
```

    ==================================================
    DOCUMENT ANALYSIS RESULTS
    ==================================================
    
    Final Status: complete
    
    Findings (accumulated from all nodes):
      1. Keyword found: 'ai'
      2. Keyword found: 'machine learning'
      3. Keyword found: 'data'
      4. Keyword found: 'python'
      5. Keyword found: 'automation'
      6. Sentiment: Positive
      7. Word count: 49
      8. Sentence count: 4



```python

```


```python

```
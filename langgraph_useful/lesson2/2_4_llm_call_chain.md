# Implementing a Simple LLM Call Chain

## Introduction

In this tutorial, you'll learn how to implement a **simple LLM call chain** using the OpenAI SDK directly (without frameworks like LangChain).

### What is an LLM Chain?

An LLM chain is a sequence of operations where:
- One step's output feeds into the next step
- Multiple LLM calls are orchestrated together
- Complex, multi-step workflows are enabled

### What You'll Build

A **writing improvement chain** that follows this pattern:
1. **Generate** - Create a first draft about a topic
2. **Critique** - Analyze what could be improved
3. **Improve** - Rewrite incorporating the feedback

### Prerequisites

- Python 3.12+
- OpenAI API key (set in a `.env` file)
- Basic understanding of functions and string formatting

### Learning Objectives

By the end of this notebook, you will:
- Understand how to chain multiple LLM calls together
- Create a reusable wrapper function for OpenAI API calls
- Implement a three-step writing improvement workflow
- See how intermediate outputs flow through a chain

## Setup: Import Libraries and Load API Key

First, we'll import the necessary libraries and load the OpenAI API key from a `.env` file.

**Note**: Make sure you have a `.env` file in your project directory with the following content:
```
OPENAI_API_KEY=your_api_key_here
```


```python
# Install required packages (uncomment if needed)
# !pip install openai python-dotenv

from openai import OpenAI
from dotenv import load_dotenv
import os

# Load environment variables from .env file
load_dotenv()

# Initialize OpenAI client
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

print("Setup complete! OpenAI client initialized.")
```

    Setup complete! OpenAI client initialized.


## Create a Reusable LLM Wrapper Function

Before building our chain, we'll create a simple helper function that wraps OpenAI API calls. This makes our code cleaner and more maintainable.

The `llm_call()` function:
- Takes a prompt as input
- Sends it to GPT-4
- Returns the text response

This abstraction allows us to focus on chain logic rather than API details.


```python
def llm_call(prompt):
    """
    Simple wrapper for OpenAI API calls.
    
    Args:
        prompt (str): The prompt to send to the LLM
        
    Returns:
        str: The LLM's response text
    """
    response = client.chat.completions.create(
        model="gpt-5",
        messages=[{"role": "user", "content": prompt}]
    )
    return response.choices[0].message.content

# Test the function with a simple prompt
test_response = llm_call("Say 'Hello, World!' in a creative way.")
print("Test response:", test_response)
```

    Test response: - Pixels wake and glow
      Across a wired horizon—
      Hello, World!
    - 👋🌍
    - Morse: .... . .-.. .-.. --- --..-- / .-- --- .-. .-.. -.. -.-.--
    - Base64: SGVsbG8sIFdvcmxkIQ==
    - Pig Latin: Ellohay, Orldway!
    - A global chorus: Hello, World! Bonjour, Monde! Hola, Mundo! こんにちは、世界！
    - From stardust through fiber: Hello, World!


## Implement the Writing Improvement Chain

Now we'll implement the core chain that demonstrates how multiple LLM calls work together.

### The Three-Step Process

1. **Generate Draft**: Create initial content about the topic
2. **Critique Draft**: Analyze what could be improved (clarity, structure, tone, etc.)
3. **Improve Draft**: Rewrite incorporating the critique

Notice how each step uses the output from the previous step - this is the essence of chaining.


```python
def writing_improvement_chain(topic):
    """
    A three-step chain that generates, critiques, and improves writing.
    
    Args:
        topic (str): The topic to write about
        
    Returns:
        dict: Contains 'draft', 'critique', and 'final' versions
    """
    print(f"Starting writing improvement chain for topic: '{topic}'\n")
    print("=" * 70)
    
    # Step 1: Generate first draft
    print("\nSTEP 1: Generating initial draft...")
    draft = llm_call(f"Write a paragraph about {topic}")
    print(f"\nDraft:\n{draft}")
    
    # Step 2: Critique the draft
    print("\n" + "=" * 70)
    print("\nSTEP 2: Analyzing draft for improvements...")
    critique = llm_call(f"What could be improved in this paragraph? Be specific. Do not rewrite the content, only output 3 improvements.\n\n{draft}")
    print(f"\nCritique:\n{critique}")
    
    # Step 3: Rewrite with improvements
    print("\n" + "=" * 70)
    print("\nSTEP 3: Rewriting with improvements...")
    final = llm_call(f"Rewrite this paragraph, keep it the same size as original and incorporate the following feedback:\n\nOriginal:\n{draft}\n\nFeedback:\n{critique}")
    print(f"\nFinal Version:\n{final}")
    
    print("\n" + "=" * 70)
    print("\nChain complete!\n")
    
    return {
        "draft": draft,
        "critique": critique,
        "final": final
    }

print("writing_improvement_chain() function defined successfully!")
```

    writing_improvement_chain() function defined successfully!


## Example: Write About Artificial Intelligence

Let's test our chain with a technical topic. Watch how the chain:
1. Creates an initial draft
2. Identifies areas for improvement
3. Produces a refined final version


```python
result = writing_improvement_chain("artificial intelligence")
```

    Starting writing improvement chain for topic: 'artificial intelligence'
    
    ======================================================================
    
    STEP 1: Generating initial draft...
    
    Draft:
    Artificial intelligence (AI) refers to computer systems designed to perform tasks that typically require human intelligence, such as recognizing patterns, understanding language, making decisions, and learning from data. Powered largely by machine learning and deep learning, AI systems excel at finding subtle relationships in vast datasets and can outperform humans in narrow domains like image recognition or game playing. AI is already embedded in everyday life—from recommendation engines and virtual assistants to medical diagnostics, fraud detection, and autonomous vehicles—driving efficiency and new capabilities across industries. At the same time, AI raises important challenges, including bias and fairness, transparency, privacy, security, and potential impacts on jobs and power dynamics. Ongoing research, standards, and regulation aim to ensure AI is reliable, safe, and aligned with human values, while continued innovation seeks to expand its usefulness and accessibility.
    
    ======================================================================
    
    STEP 2: Analyzing draft for improvements...
    
    Critique:
    - Tighten terminology and scope: briefly define machine learning and deep learning on first mention, clarify that the capabilities described are “narrow”/task-specific AI (not general intelligence), and avoid anthropomorphic phrasing like “understanding language” in favor of “processing/parsing language” to reduce implied comprehension.
    
    - Substantiate performance claims: add concrete, cited examples and, where possible, quantitative context (e.g., ImageNet accuracy rates, AlphaGo’s matches, benchmark results like MMLU or SuperGLUE) and specify domains where humans still outperform (e.g., causal reasoning, out-of-distribution generalization) to balance the claim.
    
    - Broaden and structure the risk/governance section: explicitly include robustness under distribution shift, reliability/calibration, hallucinations, data provenance/IP, environmental impacts, dual-use/misuse, and accountability; reference specific frameworks and regulations (e.g., EU AI Act, NIST AI RMF, ISO/IEC 23894 or 42001); and split the paragraph into two (capabilities vs. challenges/governance) for readability.
    
    ======================================================================
    
    STEP 3: Rewriting with improvements...
    
    Final Version:
    Artificial intelligence (AI) comprises computer systems that automate task‑specific, not general, cognitive abilities—e.g., pattern recognition, language parsing, decision support, and learning from data. Machine learning (fitting models to examples) and deep learning (multi‑layer neural networks) drive recent gains: image classifiers exceed 90% top‑1 on ImageNet; AlphaGo beat Lee Sedol 4–1; language models surpass the SuperGLUE human baseline and reach >80% on MMLU. AI now powers recommenders, assistants, diagnostics, fraud detection, and driver‑assist/autonomy; humans still lead in causal reasoning and out‑of‑distribution generalization.
    
    Risks span bias and fairness, robustness under distribution shift, reliability/calibration, hallucinations, transparency, privacy/security, data provenance/IP, environmental impacts, dual‑use/misuse, accountability, and societal effects on labor and power. Governance advances via the EU AI Act, NIST’s AI RMF, and ISO/IEC 23894 and 42001, plus research, evaluations, and audits to make AI reliable, safe, and values‑aligned.
    
    ======================================================================
    
    Chain complete!
    

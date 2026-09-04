# Integrating Tavily Search Tool into a LangChain Agent

## Overview

In this tutorial, you'll learn how to integrate the **Tavily Search Tool** into a LangChain prebuilt agent. By the end of this notebook, you'll have a working agent that can autonomously decide when to search the web for information to answer user queries.

## What You'll Learn

- How to set up and configure the Tavily search tool
- How to create a prebuilt agent that can use external tools
- How to observe the agent making decisions about when to use web search

## Prerequisites

- **API Keys Required**: You'll need both `OPENAI_API_KEY` and `TAVILY_API_KEY`
  - OpenAI: https://platform.openai.com/api-keys
  - Tavily: https://tavily.com (free account available)
- **Required Packages**: `langchain`, `langchain-openai`, `langchain-tavily`, `python-dotenv`

## Setup Instructions

1. Create a `.env` file in your project directory
2. Add your API keys:
   ```
   OPENAI_API_KEY=your-openai-key-here
   TAVILY_API_KEY=your-tavily-key-here
   ```
3. Install required packages:
   ```bash
   pip install langchain langchain-openai langchain-tavily python-dotenv
   ```

## Step 1: Import Libraries and Load Environment Variables

First, we'll import all necessary libraries and load our API keys from the `.env` file using `python-dotenv`.


```python
import os
from dotenv import load_dotenv
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_tavily import TavilySearch

# Load environment variables from .env file
load_dotenv()

# Verify API keys are loaded
if not os.getenv("OPENAI_API_KEY"):
    raise ValueError("OPENAI_API_KEY not found in environment variables")
if not os.getenv("TAVILY_API_KEY"):
    raise ValueError("TAVILY_API_KEY not found in environment variables")

print("API keys loaded successfully!")
```

    API keys loaded successfully!


## Step 2: Configure the Language Model

We'll create an instance of ChatOpenAI that will power our agent's reasoning capabilities. The agent uses this model to understand queries and decide when to use tools.


```python
# Configure the model with a low temperature for more consistent reasoning
model = ChatOpenAI(
    model="gpt-5",  # Using GPT-5 for reliable tool usage
    temperature=0.1   # Low temperature for more deterministic responses
)

print(f"Model configured: {model.model_name}")
```

    Model configured: gpt-5


## Step 3: Create the Tavily Search Tool

The Tavily search tool provides real-time web search capabilities. We'll configure it with parameters that control how searches are performed:

- **max_results**: Number of search results to return (default: 5)
- **search_depth**: "basic" for faster searches or "advanced" for more comprehensive results
- **include_raw_content**: Whether to include full page content (we'll keep this False for cleaner results)
- **include_images**: Whether to include images in results


```python
# Instantiate the Tavily search tool
search_tool = TavilySearch(
    max_results=5,
    search_depth="basic",  # Use "advanced" for more comprehensive results
    include_raw_content=False,
    include_images=False
)

print("Tavily search tool created successfully!")
print(f"Tool name: {search_tool.name}")
print(f"Tool description: {search_tool.description}")
```

    Tavily search tool created successfully!
    Tool name: tavily_search
    Tool description: A search engine optimized for comprehensive, accurate, and trusted results. Useful for when you need to answer questions about current events. It not only retrieves URLs and snippets, but offers advanced search depths, domain management, time range filters, and image search, this tool delivers real-time, accurate, and citation-backed results.Input should be a search query.


## Step 4: Create the Agent with Tavily Tool

Now we'll create a prebuilt agent using LangChain's `create_agent` function. This agent will:

1. Receive a user query
2. Reason about whether it needs to use the search tool
3. If needed, invoke the Tavily search tool
4. Synthesize the search results into a coherent answer

The agent automatically decides when to use the search tool based on the query.


```python
# Create the agent with the search tool
agent = create_agent(
    model=model,
    tools=[search_tool]  # Pass our Tavily search tool in a list
)

print("Agent created successfully!")
print(f"Agent has access to {len([search_tool])} tool(s)")
```

    Agent created successfully!
    Agent has access to 1 tool(s)


## Step 5: Create a Helper Function for Agent Interactions

To make it easier to interact with our agent, we'll create a helper function that:

- Takes a query string
- Invokes the agent with properly formatted messages
- Extracts and prints the final response

This function simplifies our testing and makes the examples more readable.


```python
def generate_and_print_response(agent, query):
    """
    Invoke the agent with a query and print the response.
    
    Args:
        agent: The LangChain agent instance
        query: The user query string
    """
    print(f"\n{'='*60}")
    print(f"Query: {query}")
    print(f"{'='*60}\n")
    
    # Invoke the agent with the query
    result = agent.invoke({
        "messages": [{"role": "user", "content": query}]
    })
    
    # Extract and print the final response
    final_message = result["messages"][-1]
    print(f"Response: {final_message.content}\n")
    
    return result

print("Helper function defined successfully!")
```

    Helper function defined successfully!


## Step 6: Test the Agent - Current Events Query

Let's test our agent with a query about recent developments. The agent should recognize that it needs current information and automatically use the Tavily search tool.


```python
# Example 1: Current events query
result1 = generate_and_print_response(
    agent,
    "What are the latest developments in artificial intelligence in 2025?"
)
```

    
    ============================================================
    Query: What are the latest developments in artificial intelligence in 2025?
    ============================================================
    
    Response: Here’s a concise rundown of notable AI developments in 2025, with sources for each item:
    
    - Frontier models and multimodality
      - OpenAI: Released GPT-4.5 (Feb), new image-generation API model (Apr), published Sora 2 video model research (Sep), and updated the GPT-5 system card with safety addendum (Oct) (openai.com/news; GPT-4.5: https://openai.com/index/introducing-gpt-4-5/; Image API: https://openai.com/index/image-generation-api/; Sora 2: https://openai.com/index/sora-2/; GPT-5 addendum: https://openai.com/index/gpt-5-system-card-sensitive-conversations/).
      - Google: Upgraded Gemini 2.5 Pro and introduced “Deep Think” reasoning mode; Veo 3.1 and 3.1 Fast video models entered public preview via the Gemini API; rolled out “AI Mode” in Search to US users (Google blog: https://blog.google/technology/google-deepmind/google-gemini-updates-io-2025/; Gemini API changelog: https://ai.google.dev/gemini-api/docs/changelog; The Verge I/O wrap: https://www.theverge.com/news/669408/google-io-2025-biggest-announcements-ai-gemini).
      - Meta: Shipped Llama 4 Scout and Llama 4 Maverick (weights downloadable), with a larger “Behemoth” model still in training (Meta AI blog: https://ai.meta.com/blog/llama-4-multimodal-intelligence/; CNBC: https://www.cnbc.com/2025/04/05/meta-debuts-new-llama-4-models-but-most-powerful-ai-model-is-still-to-come.html).
      - Anthropic: Tightened usage policies for agentic use and expanded “Claude for Financial Services” with Excel add-ins, market data connectors, and prebuilt agent skills (Usage policy: https://www.anthropic.com/news/usage-policy-update; Finance: https://www.anthropic.com/news/advancing-claude-for-financial-services).
    
    - Agentic AI and automation
      - Analyst/industry take: Agent stacks and orchestration are moving from pilots to production, but many initiatives face complexity; Gartner projects >40% of agentic AI projects will be canceled by end of 2027 (Gartner press release: https://www.gartner.com/en/newsroom/press-releases/2025-06-25-gartner-predicts-over-40-percent-of-agentic-ai-projects-will-be-canceled-by-end-of-2027).
    
    - Productivity copilots and platform features
      - Microsoft: Continued rapid updates to Microsoft 365 Copilot (new search “AI Views,” Teams summaries, admin controls) and introduced Copilot Mode in Edge; ongoing vertical copilots and service integrations (TechCommunity Oct update: https://techcommunity.microsoft.com/blog/microsoft365copilotblog/what%E2%80%99s-new-in-microsoft-365-copilot--october-2025/4464046; Copilot blog release notes: https://www.microsoft.com/en-us/microsoft-copilot/blog/2025/08/07/release-notes-august-7-2025/).
    
    - On‑device and consumer AI
      - Apple: “Apple Intelligence” added live translation and system-wide AI features at WWDC; later introduced the M5 chip with a Neural Accelerator per GPU core for faster on-device AI across MacBook Pro, iPad Pro, and Vision Pro (WWDC: https://www.apple.com/newsroom/2025/06/apple-intelligence-gets-even-more-powerful-with-new-capabilities-across-apple-devices/; M5: https://www.apple.com/newsroom/2025/10/apple-unleashes-m5-the-next-big-leap-in-ai-performance-for-apple-silicon/).
    
    - Creative AI and video generation
      - OpenAI Sora 2 (higher-fidelity text-to-video) and Google’s Veo 3.1 preview highlight rapid video model progress; Adobe launched new Firefly audio/video generation tools (Sora 2: https://openai.com/index/sora-2/; Veo 3.1: https://ai.google.dev/gemini-api/docs/changelog#video; Adobe MAX: https://news.adobe.com/news/2025/10/adobe-max-2025-firefly).
    
    - Chips and infrastructure
      - NVIDIA (GTC 2025): Announced Blackwell Ultra and next-gen “Vera Rubin” platforms targeting reasoning-heavy workloads; unveiled a Vera Rubin superchip pairing an 88‑core “Vera” CPU with two Rubin GPUs and SOCAMM memory modules (TechCrunch: https://techcrunch.com/2025/03/18/nvidia-announces-new-gpus-at-gtc-2025-including-rubin/; CRN wrap: https://www.crn.com/news/ai/2025/10-big-nvidia-gtc-2025-announcements-blackwell-ultra-rubin-ultra-dgx-spark-and-more; Tom’s Hardware: https://www.tomshardware.com/pc-components/gpus/nvidia-reveals-vera-rubin-superchip-for-the-first-time-incredibly-compact-board-features-88-core-vera-cpu-two-rubin-gpus-and-8-socamm-modules).
    
    - Regulation and policy
      - EU AI Act: Implementation milestones across 2025–2027, including early obligations for general‑purpose AI and high‑risk systems; European Parliament summary notes standards work and timeline (EPRS brief: https://www.europarl.europa.eu/RegData/etudes/ATAG/2025/772906/EPRS_ATA(2025)772906_EN.pdf; timeline explainer: https://artificialintelligenceact.eu/implementation-timeline/).
      - United States: Executive Order 14179 (Jan) directed an AI action plan to “remove barriers” and accelerate US AI leadership; the administration released an AI Action Plan in July outlining near‑term federal priorities (EO page: https://www.whitehouse.gov/presidential-actions/2025/01/removing-barriers-to-american-leadership-in-artificial-intelligence/; plan overview via Covington: https://www.insidegovernmentcontracts.com/2025/08/july-2025-ai-developments-under-the-trump-administration/).
    
    If you want, I can tailor a deeper dive to any area above (e.g., model benchmarks, enterprise agent architectures, or compliance timelines).
    


## Step 7: Test the Agent - Specific Topic Search

Now let's try a more specific query about a particular technology or company. The agent should use Tavily to find up-to-date information.


```python
# Example 2: Specific topic search
result2 = generate_and_print_response(
    agent,
    "What are the key features of LangChain's newest releases?"
)
```

    
    ============================================================
    Query: What are the key features of LangChain's newest releases?
    ============================================================
    
    Response: Here’s a concise roundup of the most recent LangChain/LangGraph releases and what they add:
    
    Core LangChain v1 (Python and JS)
    - New standard agent API: create_agent / createAgent replaces older patterns, making agent building simpler and more consistent across providers and platforms [Docs v1 (Python, JS), v1 migration guides].
    - Built-in middleware: first-class patterns for PII redaction, summarization/auto-condense, and human-in-the-loop approvals/intercepts; replaces pre/post model hooks [Docs v1].
    - Standard “content blocks”: a unified, multimodal message format used across models and tools for more reliable interactions [v1 announcement].
    - Updated semantics and migration tweaks: system_prompt naming, structured output now via Tool/Provider strategies (prompted structured output removed), updated return types, and streaming node rename [v1 migration guides].
    - New docs site and migration guides to v1 [v1 announcement, Docs v1].
    
    Agent runtime via LangGraph (shipped with v1 focus)
    - Durable execution + persistence: checkpointing, short-term memory, resumability, streaming, and human-in-the-loop patterns are native through LangGraph under the hood [v1 announcement].
    - Prebuilt agents: LangGraph 0.3 introduced prebuilt agents to speed up production agent creation [LangGraph 0.3 blog].
    - Checkpointers 3.0 and runtime improvements: checkpoint cloning, checkpoint_during, task result population, improved cache key hashing; better ergonomics across Python and JS [LangGraph releases, LangGraph.js releases, Release Week recap].
    
    Ecosystem and SDK updates (highlights)
    - Model profiles: new langchain-model-profiles with a profile property on BaseChatModel for standardized model capability descriptions/config [LangChain GitHub releases].
    - Tooling enhancements: better tool metadata, support for custom tools, and more robust streaming of tool-call chunks (especially in JS) [LangChain.js releases].
    - Provider adapters: continued updates across OpenAI/Anthropic/etc., including “reasoning” model support flags and prompt cache key support in JS packages [LangChain.js releases].
    
    Sources
    - LangChain & LangGraph v1 announcement: blog.langchain.com/langchain-langchain-1-0-alpha-releases/
    - What’s new in v1 (Python): docs.langchain.com/oss/python/releases/langchain-v1
    - What’s new in v1 (JS): docs.langchain.com/oss/javascript/releases/langchain-v1
    - v1 migration guides: docs.langchain.com/oss/python/migrate/langchain-v1 and docs.langchain.com/oss/javascript/migrate/langchain-v1
    - LangChain Python releases: github.com/langchain-ai/langchain/releases
    - LangGraph releases: github.com/langchain-ai/langgraph/releases and github.com/langchain-ai/langgraphjs/releases
    - LangGraph 0.3 prebuilt agents: blog.langchain.dev/langgraph-0-3-release-prebuilt-agents
    - LangGraph release week recap: blog.langchain.dev/langgraph-release-week-recap
    - LangChain.js releases: github.com/langchain-ai/langchainjs/releases
    
    If you tell me whether you’re on Python or JS (and which providers you use), I can map these changes to concrete upgrade steps and code snippets.
    

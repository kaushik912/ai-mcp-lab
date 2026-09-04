"""Runs every non-Streamlit example end-to-end and prints a PASS/FAIL summary.
Streamlit apps can't be smoke-tested this way (they need a browser) - see
scripts/streamlit_apps.sh for the commands to launch those by hand.

Run with: python scripts/check_examples.py [--provider {gemini,openai,openrouter}] [--sleep-seconds N]
Requires the matching provider's API key set (in .env or exported). Scripts
with a --provider flag get it passed explicitly; the rest pick it up via the
LLL_DEFAULT_PROVIDER env var (see common/config.py).
A sleep of --sleep-seconds (default 30) is inserted between cases to avoid
provider rate limits, particularly on OpenRouter's free-tier models.
"""
import argparse
import os
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
TIMEOUT_SECONDS = 60


def build_cases(provider: str) -> dict:
    """path (relative to repo root) -> (extra argv, stdin text)."""
    return {
        "examples/01_first_programs/provider_tour.py": ([], f"{provider}\nWhat is 2+2?\n"),
        "examples/01_first_programs/streaming_demo.py": ([], "What is the capital of Japan?\n"),
        "examples/02_debugging_and_streamlit/debug_tools_demo.py": ([], "What is the speed of light?\n"),
        "examples/03_prompt_templates/favorite_topic_prompt.py": ([], "cooking\nWhat is a quick breakfast idea?\n"),
        "examples/05_langgraph_basics/graph_fundamentals_demo.py": ([], "Hello LangGraph\n"),
        "examples/05_langgraph_basics/conditional_routing_demo.py": ([], "I want a refund on my order\n"),
        "examples/08_embeddings_vectorstores/embeddings_similarity_demo.py": (
            ["--provider", provider],
            "I love pizza\nI love pasta\n",
        ),
        "examples/08_embeddings_vectorstores/chroma_vectorstore_demo.py": (
            ["data/product-data.txt", "--provider", provider],
            "What is the return policy?\n",
        ),
        "examples/09_rag/rag_basic_app.py": (
            ["--provider", provider, "--query", "What is the return policy?"],
            "",
        ),
    }


def run_case(rel_path: str, extra_args: list[str], stdin_text: str, env: dict) -> tuple[bool, str, str]:
    cmd = [sys.executable, rel_path, *extra_args]
    start = time.monotonic()
    try:
        result = subprocess.run(
            cmd,
            cwd=REPO_ROOT,
            input=stdin_text,
            text=True,
            capture_output=True,
            timeout=TIMEOUT_SECONDS,
            env=env,
        )
    except subprocess.TimeoutExpired:
        return False, f"timed out after {TIMEOUT_SECONDS}s", ""
    elapsed = time.monotonic() - start
    if result.returncode == 0:
        return True, f"{elapsed:.1f}s", result.stdout.strip()
    tail = "\n".join(result.stderr.strip().splitlines()[-5:])
    return False, f"exit {result.returncode}: {tail}", result.stdout.strip()


def indent(text: str, prefix: str = "    ") -> str:
    return "\n".join(prefix + line for line in text.splitlines())


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--provider", choices=["gemini", "openai", "openrouter"], default="openrouter")
    parser.add_argument(
        "--sleep-seconds", type=float, default=30.0, help="pause between cases to avoid rate limits"
    )
    args = parser.parse_args()

    cases = build_cases(args.provider)
    env = {**os.environ, "LLL_DEFAULT_PROVIDER": args.provider}

    results = []
    for i, (rel_path, (extra_args, stdin_text)) in enumerate(cases.items()):
        if i > 0:
            time.sleep(args.sleep_seconds)
        ok, detail, stdout = run_case(rel_path, extra_args, stdin_text, env)
        results.append((rel_path, ok, detail))
        status = "PASS" if ok else "FAIL"
        print(f"[{status}] {rel_path}  ({detail})")
        if stdout:
            print(indent(stdout))
        print()

    passed = sum(1 for _, ok, _ in results if ok)
    print(f"{passed}/{len(results)} passed")
    if passed != len(results):
        sys.exit(1)


if __name__ == "__main__":
    main()

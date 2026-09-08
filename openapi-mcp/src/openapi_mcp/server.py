"""MCP server: point it at an OpenAPI spec URL, then list/describe endpoints,
generate fake sample payloads from the schema, and fire them at the live API."""

from __future__ import annotations

from typing import Any

from mcp.server.fastmcp import FastMCP

from . import client, sample, spec as spec_mod

mcp = FastMCP("openapi-mcp")

_state: dict[str, spec_mod.LoadedSpec] = {}


def _loaded() -> spec_mod.LoadedSpec:
    if "spec" not in _state:
        raise RuntimeError("No spec loaded yet. Call load_spec(spec_url) first.")
    return _state["spec"]


@mcp.tool()
def load_spec(spec_url: str) -> dict[str, Any]:
    """Fetch and parse an OpenAPI spec (e.g. http://localhost:8080/v3/api-docs).

    Must be called before any other tool. Detects the API's base URL from the
    spec's `servers` entry, falling back to the spec URL's host.
    """
    loaded = spec_mod.load(spec_url)
    _state["spec"] = loaded
    return {
        "base_url": loaded.base_url,
        "endpoint_count": len(loaded.endpoints),
        "endpoints": loaded.endpoints,
    }


@mcp.tool()
def list_endpoints() -> list[dict[str, Any]]:
    """List all method+path endpoints from the currently loaded spec."""
    return _loaded().endpoints


@mcp.tool()
def describe_endpoint(method: str, path: str) -> dict[str, Any]:
    """Show the resolved request-body schema, parameters, and responses for one endpoint."""
    loaded = _loaded()
    op = spec_mod.find_operation(loaded.raw, method, path)
    return {
        "summary": op.get("summary"),
        "parameters": spec_mod.parameters(op),
        "request_body_schema": spec_mod.request_body_schema(loaded.raw, op),
        "responses": {code: r.get("description") for code, r in op.get("responses", {}).items()},
    }


def _generate_for_op(loaded: spec_mod.LoadedSpec, op: dict[str, Any]) -> dict[str, Any]:
    path_params: dict[str, Any] = {}
    query_params: dict[str, Any] = {}
    for p in spec_mod.parameters(op):
        resolved_schema = spec_mod.resolve_schema(loaded.raw, p.get("schema"))
        value = sample.generate(resolved_schema, p.get("name", ""))
        if p.get("in") == "path":
            path_params[p["name"]] = value
        elif p.get("in") == "query":
            query_params[p["name"]] = value

    body_schema = spec_mod.request_body_schema(loaded.raw, op)
    body = sample.generate(body_schema) if body_schema else None

    return {"path_params": path_params, "query_params": query_params, "body": body}


@mcp.tool()
def generate_sample(method: str, path: str) -> dict[str, Any]:
    """Generate a fake path/query params + request body for one endpoint, without sending it.

    Useful to preview what call_endpoint or seed would send.
    """
    loaded = _loaded()
    op = spec_mod.find_operation(loaded.raw, method, path)
    return _generate_for_op(loaded, op)


@mcp.tool()
def call_endpoint(
    method: str,
    path: str,
    path_params: dict[str, Any] | None = None,
    query_params: dict[str, Any] | None = None,
    body: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Call an endpoint on the live API described by the loaded spec.

    Any of path_params/query_params/body left out are auto-generated from the
    schema (e.g. for a quick create/update without hand-writing a payload).
    """
    loaded = _loaded()
    op = spec_mod.find_operation(loaded.raw, method, path)
    generated = _generate_for_op(loaded, op)

    return client.call(
        loaded.base_url,
        method,
        path,
        path_params=path_params if path_params is not None else generated["path_params"],
        query_params=query_params if query_params is not None else generated["query_params"],
        body=body if body is not None else generated["body"],
    )


@mcp.tool()
def seed(method: str, path: str, count: int = 5) -> list[dict[str, Any]]:
    """Repeatedly generate a fresh fake payload and call the endpoint with it.

    Typical use: seed("POST", "/todos", count=10) to create 10 sample todos.
    Returns one result per call.
    """
    loaded = _loaded()
    op = spec_mod.find_operation(loaded.raw, method, path)
    results = []
    for _ in range(count):
        generated = _generate_for_op(loaded, op)
        result = client.call(
            loaded.base_url,
            method,
            path,
            path_params=generated["path_params"],
            query_params=generated["query_params"],
            body=generated["body"],
        )
        results.append(result)
    return results


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()

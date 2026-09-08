"""Fetch and resolve an OpenAPI spec."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any
from urllib.parse import urljoin, urlparse

import httpx


@dataclass
class LoadedSpec:
    spec_url: str
    base_url: str
    raw: dict[str, Any]
    endpoints: list[dict[str, Any]] = field(default_factory=list)


def fetch_spec(spec_url: str) -> dict[str, Any]:
    resp = httpx.get(spec_url, timeout=10)
    resp.raise_for_status()
    return resp.json()


def resolve_ref(raw: dict[str, Any], ref: str) -> dict[str, Any]:
    """Resolve a local '#/components/schemas/Foo' style ref."""
    if not ref.startswith("#/"):
        raise ValueError(f"Only local refs are supported, got: {ref}")
    node: Any = raw
    for part in ref[2:].split("/"):
        node = node[part]
    return node


def resolve_schema(raw: dict[str, Any], schema: dict[str, Any] | None, _depth: int = 0) -> dict[str, Any]:
    """Recursively resolve $ref / allOf in a schema, capping recursion depth."""
    if not schema:
        return {}
    if _depth > 8:
        return {}

    if "$ref" in schema:
        target = resolve_ref(raw, schema["$ref"])
        return resolve_schema(raw, target, _depth + 1)

    if "allOf" in schema:
        merged: dict[str, Any] = {"type": "object", "properties": {}, "required": []}
        for sub in schema["allOf"]:
            resolved = resolve_schema(raw, sub, _depth + 1)
            merged["properties"].update(resolved.get("properties", {}))
            merged["required"].extend(resolved.get("required", []))
        return merged

    for key in ("oneOf", "anyOf"):
        if key in schema and schema[key]:
            return resolve_schema(raw, schema[key][0], _depth + 1)

    if schema.get("type") == "object" and "properties" in schema:
        return {
            **schema,
            "properties": {
                name: resolve_schema(raw, sub, _depth + 1)
                for name, sub in schema["properties"].items()
            },
        }

    if schema.get("type") == "array" and "items" in schema:
        return {**schema, "items": resolve_schema(raw, schema["items"], _depth + 1)}

    return schema


def derive_base_url(raw: dict[str, Any], spec_url: str) -> str:
    servers = raw.get("servers") or []
    if servers and servers[0].get("url"):
        url = servers[0]["url"]
        # springdoc/swagger sometimes emit relative server urls
        if url.startswith("/"):
            parsed = urlparse(spec_url)
            return f"{parsed.scheme}://{parsed.netloc}{url}"
        return url.rstrip("/")
    parsed = urlparse(spec_url)
    return f"{parsed.scheme}://{parsed.netloc}"


def list_endpoints(raw: dict[str, Any]) -> list[dict[str, Any]]:
    out = []
    for path, methods in raw.get("paths", {}).items():
        for method, op in methods.items():
            if method.lower() not in ("get", "post", "put", "patch", "delete"):
                continue
            out.append(
                {
                    "method": method.upper(),
                    "path": path,
                    "operationId": op.get("operationId"),
                    "summary": op.get("summary") or op.get("description"),
                }
            )
    return out


def load(spec_url: str) -> LoadedSpec:
    raw = fetch_spec(spec_url)
    base_url = derive_base_url(raw, spec_url)
    return LoadedSpec(spec_url=spec_url, base_url=base_url, raw=raw, endpoints=list_endpoints(raw))


def find_operation(raw: dict[str, Any], method: str, path: str) -> dict[str, Any]:
    methods = raw.get("paths", {}).get(path)
    if methods is None:
        raise ValueError(f"No such path in spec: {path}")
    op = methods.get(method.lower())
    if op is None:
        raise ValueError(f"No such method {method} on path {path}")
    return op


def request_body_schema(raw: dict[str, Any], op: dict[str, Any]) -> dict[str, Any] | None:
    body = op.get("requestBody")
    if not body:
        return None
    content = body.get("content", {})
    json_content = content.get("application/json") or next(iter(content.values()), None)
    if not json_content:
        return None
    return resolve_schema(raw, json_content.get("schema"))


def parameters(op: dict[str, Any]) -> list[dict[str, Any]]:
    return op.get("parameters", [])

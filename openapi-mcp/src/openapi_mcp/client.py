"""Execute requests against the live API described by the spec."""

from __future__ import annotations

from typing import Any

import httpx


def call(
    base_url: str,
    method: str,
    path: str,
    path_params: dict[str, Any] | None = None,
    query_params: dict[str, Any] | None = None,
    body: Any = None,
) -> dict[str, Any]:
    resolved_path = path
    for key, value in (path_params or {}).items():
        resolved_path = resolved_path.replace("{" + key + "}", str(value))

    url = base_url.rstrip("/") + resolved_path
    resp = httpx.request(
        method.upper(),
        url,
        params=query_params,
        json=body if body is not None else None,
        timeout=10,
    )
    try:
        parsed_body: Any = resp.json()
    except ValueError:
        parsed_body = resp.text

    return {
        "status_code": resp.status_code,
        "ok": resp.is_success,
        "body": parsed_body,
    }

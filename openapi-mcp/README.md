# openapi-mcp

MCP server that points at any OpenAPI spec URL (e.g. a Springdoc
`/v3/api-docs` endpoint) and lets an agent explore endpoints, generate fake
sample payloads from the schema, and fire create/update/delete calls at the
live API.

## Tools

- `load_spec(spec_url)` — fetch + parse the spec, detect the API base URL. Call first.
- `list_endpoints()` — all method+path endpoints.
- `describe_endpoint(method, path)` — resolved request body schema, params, responses.
- `generate_sample(method, path)` — preview a fake payload without sending it.
- `call_endpoint(method, path, path_params?, query_params?, body?)` — call the live API; omitted args are auto-generated from the schema.
- `seed(method, path, count=5)` — call an endpoint N times with a fresh generated payload each time (e.g. `seed("POST", "/todos", 10)`).

## Install

```
uv sync
```

## Run standalone (stdio)

```
uv run openapi-mcp
```

## Register with Claude Code

```
claude mcp add openapi-mcp -- uv --directory /home/kaush/github_projs/ai-mcp-lab/openapi-mcp run openapi-mcp
```

Or add to `.mcp.json` / `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "openapi-mcp": {
      "command": "uv",
      "args": ["--directory", "/home/kaush/github_projs/ai-mcp-lab/openapi-mcp", "run", "openapi-mcp"]
    }
  }
}
```

## Example session

```
load_spec("http://localhost:8080/v3/api-docs")
list_endpoints()
seed("POST", "/todos", count=5)
call_endpoint("GET", "/todos")
```

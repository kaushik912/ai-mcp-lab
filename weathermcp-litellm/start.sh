#!/bin/sh
# Single-container entrypoint (Cloud Run): both MCP servers on localhost,
# the agent + UI on $PORT. Locally, docker-compose still runs them as separate services.
set -e

MCP_HOST=127.0.0.1 python weather_mcp.py &
MCP_HOST=127.0.0.1 python quote_mcp.py &

# Wait until both MCP servers accept connections (agent connects to them on each request).
python - <<'EOF'
import socket, time
for port in (8001, 8002):
    for _ in range(50):
        try:
            socket.create_connection(("127.0.0.1", port), timeout=0.2).close()
            break
        except OSError:
            time.sleep(0.2)
    else:
        raise SystemExit(f"MCP server on :{port} did not start")
EOF

exec uvicorn agent:app --host 0.0.0.0 --port "${PORT:-8000}"

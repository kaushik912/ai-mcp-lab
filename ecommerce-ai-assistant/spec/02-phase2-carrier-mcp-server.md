# Phase 2: Carrier & logistics MCP server (DONE)

## Requirements
1. `carrier-mcp-server` on port 8081, `spring-ai-starter-mcp-server-webmvc`, `spring.ai.mcp.server.protocol=STREAMABLE`.
2. `@McpTool` methods simulating a 3PL:
   - `trackShipment(String trackingId)`: live scan events, current hub, delay reason (e.g. weather delay at Bangalore hub for `ORD-1004`'s tracking id).
   - `generateReturnShippingLabel(String rmaId, String customerName, String pickupPincode, String category)`: label object (AWB, barcode string, carrier name, instructions).
   - `findNearbyDropoffPoints(String pincode)`: 3 realistic drop-off locker/hub locations with distance and operating hours.
3. Clean request logging so terminal/UI shows incoming MCP tool invocations.

## Implementation notes
- Tracking id for `ORD-1004`: `TRK-BLR-77412` (Whitefield Sort Hub, weather delay).
- Deterministic mock data; label AWB derived from rmaId (idempotent).
- No user identity in any MCP tool schema.
- Logging: `McpRequestLoggingFilter` (`[MCP] POST /mcp rpc=tools/call tool=... ms`) + per-tool `[MCP TOOL]` lines.
- Endpoint `http://localhost:8081/mcp`.

## Verification: DONE
4 unit tests + `McpServerSmokeTest` (real Streamable HTTP: initialize, tools/list, tools/call). `mvn verify` green.

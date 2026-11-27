# How to test Phase 2: carrier MCP server

Spec: [../spec/02-phase2-carrier-mcp-server.md](../spec/02-phase2-carrier-mcp-server.md)

Goal: prove `carrier-mcp-server` runs on port 8081, speaks MCP over Streamable HTTP at `/mcp`, exposes 3 tools with no user identity in their schemas, returns the expected mock data, and logs every call.

## Prerequisites

- Java 21, Maven, `curl`, `jq`
- No API key needed.

```bash
cd ecommerce-ai-assistant
mvn -B -q install -DskipTests
```

## Step 1: Run the Phase 2 tests

```bash
mvn -B -q -pl carrier-mcp-server test
grep -h "Tests run" carrier-mcp-server/target/surefire-reports/*.txt
```

Expected:

```
Tests run: 4, Failures: 0, Errors: 0 ... CarrierToolsTest
Tests run: 1, Failures: 0, Errors: 0 ... McpServerSmokeTest
```

`McpServerSmokeTest` boots the real server and performs the MCP handshake over HTTP (`initialize`, then `tools/list`, then `tools/call`).

## Step 2: Start the server

```bash
java -jar carrier-mcp-server/target/carrier-mcp-server-0.1.0-SNAPSHOT.jar
```

Expected: `Started CarrierMcpServerApplication ...` on port 8081.

## Step 3: Talk MCP by hand with curl (second terminal)

MCP over Streamable HTTP has 3 steps: `initialize` returns a session id, you confirm with `notifications/initialized`, then you send requests with that session id. Responses arrive as server-sent events (`data:` lines), so each call strips that prefix before `jq`.

Paste this whole block into one terminal:

```bash
H='-H Content-Type:application/json -H Accept:application/json,text/event-stream'

# 1. initialize -> capture the Mcp-Session-Id header
SID=$(curl -s -D - -o /dev/null localhost:8081/mcp $H \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}' \
  | grep -i mcp-session-id | awk '{print $2}' | tr -d '\r')
echo "session: $SID"

# 2. confirm initialization (expect 202)
curl -s -o /dev/null -w "initialized: %{http_code}\n" localhost:8081/mcp $H -H "Mcp-Session-Id: $SID" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}'

# helper for the rest
mcp() { curl -s localhost:8081/mcp $H -H "Mcp-Session-Id: $SID" -d "$1" | sed -n 's/^data://p'; }
```

Expected: a session UUID, then `initialized: 202`.

### 3a. List tools: exactly 3, no user identity in any schema

```bash
mcp '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  | jq -r '.result.tools[] | "\(.name): \(.inputSchema.properties|keys|join(","))"'
```

Expected:

```
findNearbyDropoffPoints: pincode
generateReturnShippingLabel: category,customerName,pickupPincode,rmaId
trackShipment: trackingId
```

### 3b. trackShipment: delayed order ORD-1004

```bash
mcp '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"trackShipment","arguments":{"trackingId":"TRK-BLR-77412"}}}' \
  | jq -r '.result.content[0].text' | jq '{status,currentLocation,delayed,delayReason,events:(.events|length)}'
```

Expected:

```json
{
  "status": "DELAYED_AT_HUB",
  "currentLocation": "Bangalore Whitefield Sort Hub",
  "delayed": true,
  "delayReason": "Weather delay: heavy rain and flooding on the Hosur Road corridor halted line-haul trucks",
  "events": 5
}
```

Unknown tracking id:

```bash
mcp '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"trackShipment","arguments":{"trackingId":"NOPE"}}}' \
  | jq -r '.result.content[0].text' | jq -r .status
```

Expected: `NOT_FOUND`

### 3c. generateReturnShippingLabel: structured label, same AWB on repeat

```bash
LABEL='{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"generateReturnShippingLabel","arguments":{"rmaId":"RMA-TEST1","customerName":"Alex","pickupPincode":"560100","category":"FOOTWEAR"}}}'
mcp "$LABEL" | jq -r '.result.content[0].text' | jq '{awb,barcode,carrier,packagingInstructions}'
mcp "$LABEL" | jq -r '.result.content[0].text' | jq -r .awb   # call again
```

Expected: an `AWB...` number, a barcode string, `"carrier": "SwiftShip Logistics"` and shoebox packaging instructions. The second call prints the **same** AWB (it's derived from the RMA id).

### 3d. findNearbyDropoffPoints: 3 points with distance and hours

```bash
mcp '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"findNearbyDropoffPoints","arguments":{"pincode":"560100"}}}' \
  | jq -r '.result.content[0].text' | jq -r '.[] | "\(.name) | \(.distanceKm) km | \(.hours)"'
```

Expected:

```
SwiftLocker - Electronic City Phase 1 | 0.8 km | Mon-Sun 08:00-22:00
SwiftShip Hub - Hosur Road | 2.1 km | Mon-Sat 09:00-19:00
Partner Store - Kirana Plus Konappana Agrahara | 3.4 km | Mon-Sun 10:00-21:00
```

Try `400001` (Mumbai) for named Mumbai points, or any other pincode for generic ones.

## Step 4: Check the request logging

In the server terminal you should see one line per JSON-RPC call plus one per tool execution:

```
[MCP] POST /mcp rpc=initialize status=200 ...ms
[MCP] POST /mcp rpc=tools/list status=200 ...ms
[MCP TOOL] trackShipment trackingId=TRK-BLR-77412
[MCP] POST /mcp rpc=tools/call tool=trackShipment status=200 ...ms
```

Stop the server with `Ctrl+C`.

## Pass checklist

- [ ] `CarrierToolsTest` 4/4 and `McpServerSmokeTest` 1/1 pass
- [ ] Server starts on 8081; `initialize` returns an `Mcp-Session-Id`
- [ ] `tools/list` shows exactly 3 tools, none with a user/userId parameter
- [ ] `TRK-BLR-77412`: `DELAYED_AT_HUB`, Bangalore hub, weather delay reason
- [ ] Label has AWB + barcode + carrier + instructions; same AWB on repeat
- [ ] Drop-off returns 3 points with distance and hours
- [ ] Terminal shows `[MCP]` and `[MCP TOOL]` log lines

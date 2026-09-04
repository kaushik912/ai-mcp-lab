.PHONY: test test-servers test-client

# Fast/slice tests only — no external services required.
test: test-servers
	mvn -q -f sample-client/pom.xml test

test-servers:
	mvn -q -f person-mcp-service/pom.xml test
	mvn -q -f account-mcp-service/pom.xml test

# Full integration suite, including sample-client's live multi-tool-orchestration
# tests. Requires person-mcp-service (8060) and account-mcp-service (8040) already
# running, and OPENROUTER_API_KEY set — see README "Running tests".
test-client:
	mvn -q -f sample-client/pom.xml test

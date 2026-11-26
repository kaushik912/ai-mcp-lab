# Convenience targets for quiz-generator. Run `make` to list them.
E2E_PORT ?= 8099

.DEFAULT_GOAL := help
.PHONY: help run test e2e-install e2e e2e-headed e2e-report check

help: ## Show this help
	@grep -E '^[a-z0-9-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-12s %s\n", $$1, $$2}'

run: ## Start the app on :8080 (PORT=8081 make run to change the port)
	./mvnw spring-boot:run

test: ## Java unit tests (fast, no browser)
	./mvnw test

e2e-install: ## One-time setup for browser tests: npm packages + Chromium
	cd e2e && npm install && npx playwright install chromium

e2e: ## Browser tests (starts the app itself on spare port 8099, never touches :8080)
	@test -d e2e/node_modules || { echo "Run 'make e2e-install' first"; exit 1; }
	cd e2e && E2E_PORT=$(E2E_PORT) npx playwright test

e2e-headed: ## Same as e2e, but you can watch the browser
	@test -d e2e/node_modules || { echo "Run 'make e2e-install' first"; exit 1; }
	cd e2e && E2E_PORT=$(E2E_PORT) npx playwright test --headed

e2e-report: ## Open the HTML report of the last e2e run
	cd e2e && npx playwright show-report

check: test e2e ## Everything: Java tests, then browser tests

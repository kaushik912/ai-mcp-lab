# Spec index

Source of truth for the build. Progress tracking lives in [../TASKS.md](../TASKS.md).
How to verify each phase (tests + curl): [../docs/HOW_TO_TEST_PHASE1.md](../docs/HOW_TO_TEST_PHASE1.md) .. `HOW_TO_TEST_PHASE5.md`. Run + demo script: [../README.md](../README.md).

| File | Content |
|---|---|
| [00-overview-and-guardrails.md](00-overview-and-guardrails.md) | Role, objective, architecture, mandatory guardrails |
| [01-phase1-scaffolding-domain-seed.md](01-phase1-scaffolding-domain-seed.md) | Phase 1 (done) |
| [02-phase2-carrier-mcp-server.md](02-phase2-carrier-mcp-server.md) | Phase 2 (done) |
| [03-phase3-rag-and-search.md](03-phase3-rag-and-search.md) | Phase 3 (done) |
| [04-phase4-tools-security-mcp-client.md](04-phase4-tools-security-mcp-client.md) | Phase 4 (done) |
| [05-phase5-ui-and-verification.md](05-phase5-ui-and-verification.md) | Phase 5 (done) |
| [06-demo-scenarios.md](06-demo-scenarios.md) | Seed data x demo prompts x expected outcomes |
| [99-original-prompt.md](99-original-prompt.md) | Original request, verbatim |

Process rule: after each phase, verify compile/tests, summarise, ask user to confirm before the next phase.
Deviations from the original prompt are marked **DEVIATION** in the phase files.

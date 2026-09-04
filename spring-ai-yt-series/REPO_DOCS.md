# spring-ai-yt-series repos (crisp)

- advance_rag — RAG chat: VectorStoreDocumentRetriever + custom RetrievalAugmentationAdvisor, POST /chat
- advisor_app — Spring AI Advisor pattern demo (custom TokenPrintAdvisor), GET /chat + /stream-chat, no vector store
- boot-gemini-app — Gemini via Spring AI, GET /ai (java code lives under src/main/kotlin dir, misleading name)
- chatclient-api — bare ChatClient basics, GET /chat, no advisors/memory/rag
- docker-model-example — empty placeholder dir (Docker Model Runner example, not built out)
- help-desk-backend — full helpdesk AI backend: AiController (chat+stream) + TicketService, /api/v1/helpdesk
- helpdesk — not a repo, a .txt: system-prompt persona for the helpdesk AI assistant
- helpdesk-frontend — React+Vite frontend, pairs with help-desk-backend
- mcp — MCP learning scratch notes (Test.java, notes/, python_topics.txt), not an app
- mcp-app — Model Context Protocol demo, POST /ai/chat + /ai/groq (Groq LLM)
- memory_app — chat with ChatMemory (per-user CONVERSATION_ID), GET /chat + /stream-chat
- ppt — Keynote slides for the YT series (spring ai intro, advisor, chat memory topics)
- rag_app — RAG demo like advance_rag but QuestionAnswerAdvisor path commented out (WIP/earlier version)
- spring-ai-genimi — empty scaffold (.idea only, no code)
- spring-ai-ollam — local Ollama chat demo, GET /chat
- spring-ai-openai — baseline OpenAI ChatClient demo, GET /chat (starter project)
- spring_ai_media — audio: /api/v1/audio text-to-speech, transcript, transcript-with-options
- tool-calling-example — function/tool calling demo, GET /chat

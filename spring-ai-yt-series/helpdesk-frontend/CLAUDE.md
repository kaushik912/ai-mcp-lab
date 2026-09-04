# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- `npm run dev` — start Vite dev server with HMR
- `npm run build` — production build (outputs to `dist/`)
- `npm run preview` — preview the production build locally
- `npm run lint` — run ESLint over the project

There is no test runner configured in this project (no test script, no test files).

## Architecture

This is a React 19 + Vite SPA that is the chat frontend for a Spring AI help-desk backend (sibling directory `../help-desk-backend`, expected at `http://localhost:8081`).

- **Routing**: `src/main.jsx` sets up `react-router` (`BrowserRouter`) with two routes: `/` → `ChatHome` (landing page), `/chat` → `Chat` (main chat UI). `App.jsx` is the default Vite template scaffold and is not wired into the router.
- **Chat flow**: `src/pages/Chat.jsx` holds all chat state (messages, draft, sending, conversationId) locally via `useState`/`useRef` — no global state library. On mount it generates a fresh `conversationId` with `uuid` (`v4`). Sending a message calls `sendMessagesToServer` in `src/services/chat.service.js`, which POSTs to `${baseURL}/api/v1/helpdesk` (axios) with the message as the body and the conversation id passed in a custom `ConversationId` header. The sidebar chat list (`CHATS`) and initial greeting (`CONVERSATION`) in `Chat.jsx` are hardcoded mock/demo data, not fetched from the backend.
- **Backend base URL** is hardcoded in `chat.service.js` (`http://localhost:8081/api/v1`) — not sourced from an env var.
- **UI components**: `src/components/ui/` are shadcn/ui primitives (button, input, card, avatar, scroll-area, separator, spinner), generated per `components.json` (style: "new-york", icon library: lucide, base color: neutral). `src/components/MessageBubble.jsx` is the one custom chat-specific component, rendering user vs. bot bubbles differently based on the `author` prop.
- **Styling**: Tailwind CSS v4 via `@tailwindcss/vite` plugin (not a `tailwind.config.js` — v4 uses CSS-based config in `src/index.css` with `@theme inline` tokens and `tw-animate-css`). `cn()` helper in `src/lib/utils.js` merges class names (`clsx` + `tailwind-merge`).
- **Path alias**: `@/*` maps to `src/*`, configured in both `vite.config.js` (`resolve.alias`) and `jsconfig.json`.

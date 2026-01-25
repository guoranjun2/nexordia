# Task: Tool-capable chat history as transcripts
- **Task Identifier:** 2026-01-24-chat-history-transcripts
- **Scope:** Separate live, tool-capable chat sessions from persisted chat transcripts; persist only human-readable chat records; enable users to start a new tool-capable chat from a transcript with explicit map confirmation.
- **Motivation:** Persisted tool-capable chats cannot safely resume runtime state across sessions, so the system must avoid silent map reassociation while still supporting review and reuse of past conversations.
- **Developer Briefing:** This alternative approach treats persisted chat history as transcripts, not resumable sessions. Live sessions remain tool-capable in-memory, while saved chats store only text plus tool summaries. Starting a new chat from a transcript is explicit and map-agnostic until the user confirms which maps should be used.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java` and `ChatMessageHistory` store chat messages in-memory without persistence.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryController.java` controls LangChain4j chat memory, which is session-local.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java` assigns UUIDs at runtime and does not persist identifiers across sessions.
  - `freeplane/src/main/java/org/freeplane/features/map/MapModel.java` exposes `getURL()`, but unsaved maps have no stable URL.
  - Existing JSON usage (for example in `AIModelCatalog`) indicates Jackson is available for persistence.
- **Design:**
  - Treat persisted history as transcripts: store only user/assistant text and tool call summaries, not live tool call payloads or runtime identifiers.
  - Keep live sessions fully tool-capable in-memory for the current application session only.
  - When a user starts a new chat from a transcript, seed the new chat memory with the transcript text and a system message that forbids assuming any map context until explicitly confirmed.
  - Require explicit user confirmation of map context before tool-capable actions that depend on a specific map.
- **Test specification:**
  - Verify persisted transcripts do not contain runtime identifiers or tool call payloads.
  - Verify starting a new chat from a transcript requires explicit map confirmation before map-specific tool calls.
  - Manual verification that live sessions remain fully tool-capable during the same application session.

## Subtask: Live session continuation and transcript capture
- **Status:** Planning
- **Scope:** Define how live tool-capable sessions continue within the same Freeplane runtime and how their messages are captured for transcript persistence.
- **Motivation:** Live sessions remain the only safe environment for tool-capable continuity; transcripts must be derived without preserving runtime state.
- **Developer Briefing:** Capture message flow from `AIChatPanel` and `ChatMessageHistory` into a transcript-friendly format while keeping tool calls fully functional in-memory.
- **Research:**
  - `AIChatPanel` appends messages via `ChatMessageHistory` and resets state on new chat.
  - `ChatSessionMemoryController` maintains LangChain4j chat memory independently of the UI history.
  - Tool call summaries are already surfaced via `ToolCallSummaryHandler`.
- **Design:**
  - Define a transcript capture path that records user and assistant text plus tool call summaries as plain text blocks.
  - Decide when capture occurs (after each assistant response or on explicit save) while leaving the live session and tool execution untouched.
  - Keep transcript capture decoupled from runtime identifiers to avoid implied resumability.
- **Test specification:**
  - Verify transcript capture preserves message order and tool call summary text.
  - Verify live session tools still operate during capture.

## Subtask: Persisted transcript format and storage
- **Status:** Planning
- **Scope:** Specify the transcript schema, storage location, gzip JSON persistence, and list/load/delete operations for transcripts.
- **Motivation:** A stable, readable, shareable transcript format is needed without embedding runtime state.
- **Developer Briefing:** Implement a transcript store under the user configuration directory with metadata for listing and retrieval, using gzip JSON for compactness.
- **Research:**
  - `ResourceController.getFreeplaneUserDirectory()` provides a user-specific storage root.
  - Jackson `ObjectMapper` usage in the plugin indicates JSON serialization support is already present.
- **Design:**
  - Store transcripts under `${freeplaneUserDirectory}/ai-chats` with deterministic filenames based on timestamp and a sanitized title.
  - Transcript schema includes timestamp, title (first user message), and ordered entries (role + plain text).
  - List/load/delete operations return metadata without attempting to restore runtime identifiers.
- **Test specification:**
  - Verify transcript serialization round-trips and list/delete behavior.
  - Verify transcripts contain no runtime identifiers or tool payloads.

## Subtask: Start new chat from transcript with explicit map confirmation
- **Status:** Planning
- **Scope:** Provide a "start new chat from transcript" flow that seeds the new chat and enforces explicit map selection before map-specific actions.
- **Motivation:** Prevent silent misassociation of maps while still enabling reuse of prior conversations.
- **Developer Briefing:** The new chat starts with transcript context but no map bindings; map context is established only after explicit user confirmation.
- **Research:**
  - `AvailableMaps` provides runtime identifiers only for currently open maps.
  - Current tools require a map identifier to operate, and selection tools can provide current map identifiers for explicit user selection.
- **Design:**
  - Insert a system message when starting from transcript that instructs the assistant to request map confirmation for any map-specific action.
  - Provide a UI affordance to select or confirm maps (for example by reusing existing selection tools or a map picker), then proceed with tool-capable actions.
  - Ensure the assistant remains map-agnostic until confirmation is recorded.
- **Test specification:**
  - Verify the system message is injected for transcript-based chats.
  - Verify map-specific actions are blocked or deferred until explicit map confirmation is provided.

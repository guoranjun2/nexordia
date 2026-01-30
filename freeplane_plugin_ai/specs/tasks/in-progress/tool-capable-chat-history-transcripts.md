# Task: Tool-capable chat history as transcripts
- **Task Identifier:** 2026-01-24-chat-history-transcripts
- **Scope:** Persist chat history as transcript-only gzip JSON records in
  `${freeplaneUserDirectory}/ai-chats`, list saved transcripts, allow
  loading and deletion, capture user/assistant messages only, assign
  automatic names to live chats and saved transcripts with user
  editability, show names and map root short texts in the chat list, and
  enable users to start a new tool-capable chat from a transcript with
  explicit map confirmation.
- **Motivation:** Persisted tool-capable chats cannot safely resume
  runtime state across sessions, so the system must avoid silent map
  reassociation while still supporting review and reuse of past
  conversations.
- **Developer Briefing:** Treat persisted chat history as transcripts,
  not resumable sessions. Live sessions remain tool-capable in-memory,
  while saved transcripts store only user and assistant text. Live
  chats and saved transcripts both carry an auto-assigned name that the
  user can edit, and the list UI shows that name along with shortened
  root node texts for maps involved in the chat. Users can list, load,
  and delete transcripts, or start a new tool-capable chat from one
  with explicit map confirmation before map-specific tools run. Use
  the `org.freeplane.plugin.ai.chat.history` package for the transcript
  store and data model.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    and `ChatMessageHistory` store chat messages in-memory without
    persistence.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryController.java`
    controls LangChain4j chat memory, which is session-local.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java`
    assigns UUIDs at runtime and does not persist identifiers across
    sessions.
  - `freeplane/src/main/java/org/freeplane/core/resources/ResourceController.java`
    provides `getFreeplaneUserDirectory()` for user-level storage roots.
  - Existing JSON usage (for example in `AIModelCatalog`) indicates
    Jackson is available for persistence.
- **Design:**
  - Persist only user and assistant messages; no tool calls, tool output,
    or runtime identifiers are saved.
  - Keep live sessions fully tool-capable in-memory for the current
    application session only.
  - Assign a default name when a live chat starts using the timestamp
    and the first four words of the first user message once available
    unless the user has already edited the name (fallback to timestamp
    only if no user message exists yet).
  - Allow users to edit the live chat name; persist the current name
    into the transcript record when saving.
  - Maintain a per-chat list of referenced map identifiers and compute
    shortened root node texts (40 characters, using existing short-text
    helpers) on demand for display; only persisted transcripts store
    the short texts with counts.
  - Store transcripts under `${freeplaneUserDirectory}/ai-chats` with
    deterministic filenames based on timestamp and a sanitized name.
  - Implement transcript persistence in
    `org.freeplane.plugin.ai.chat.history` with:
    - `ChatTranscriptRecord` for on-disk data.
    - `ChatTranscriptEntry` for ordered message entries.
    - `ChatTranscriptStore` for list/load/save/delete and file system
      concerns.
  - Provide list, load, delete, and rename operations that return
    metadata without attempting to restore runtime identifiers.
  - Capture messages from the chat panel into transcripts without
    altering live tool execution.
  - Persist transcripts on live chat switch and on close; there is no
    explicit save action.
  - Persist transcripts on chat switch and on chat close only; do not
    write on every assistant response.
  - When a user starts a new chat from a transcript, seed the new chat
    memory with the transcript text and a system message that forbids
    assuming any map context until explicitly confirmed.
  - Require explicit user confirmation of map context before
    tool-capable actions that depend on a specific map.
- **Test specification:**
  - Verify persisted transcripts do not contain runtime identifiers or
    tool call payloads.
  - Verify transcript serialization round-trips and list/delete/rename
    behavior.
  - Verify transcript names are auto-assigned, can be edited, and
    appear in list results.
  - Verify map root short texts with counts appear in chat lists and
    update when transcripts are persisted.
  - Verify starting a new chat from a transcript requires explicit map
    confirmation before map-specific tool calls.
  - Manual verification that live sessions remain fully tool-capable
    during the same application session.

## Subtask: Live session continuation
- **Status:** Planning
- **Scope:** Define how live tool-capable sessions continue within the
  same Freeplane runtime, including chat memory behavior and UI state
  across a single application session.
- **Motivation:** Live sessions remain the only safe environment for
  tool-capable continuity, so their behavior must remain unchanged by
  transcript features.
- **Developer Briefing:** Confirm live session behavior in
  `AIChatPanel`, `ChatMessageHistory`, and
  `ChatSessionMemoryController` is preserved while transcript features
  are added.
- **Research:**
  - `AIChatPanel` appends messages via `ChatMessageHistory` and resets
    state on new chat.
  - `ChatSessionMemoryController` maintains LangChain4j chat memory
    independently of the UI history.
- **Design:**
  - Ensure transcript features do not change when chat memory is reset
    or cleared for a new chat.
  - Keep tool-capable memory updates and tool execution flow identical
    to current behavior.
  - Live chat memory (`AIChatService`/LangChain4j memory) remains the
    authoritative source of truth for the session. Transcript data is
    non-authoritative and must not feed back into live tool behavior.
  - Do not move, delay, or alter the existing calls that append to
    `ChatMessageHistory` or update `ChatSessionMemoryController`; the
    transcript adapter must observe those events only.
  - Transcript feature failures (capture, debounce scheduling, store
    errors) must be isolated so they never block UI updates or tool
    execution.
  - Live session name edits are UI-only metadata and must not alter
    map context, tool routing, or chat memory state.
  - When a transcript is loaded for viewing, keep the live tool-capable
    session intact; only the UI history is replaced.
  - Contract for live session operations (no behavior change):
    - `startNewChat()` continues to clear `ChatMessageHistory` and
      reset `ChatSessionMemoryController` as it does today.
    - User message flow remains: UI append → chat memory update →
      tool-capable execution path unchanged.
    - Assistant response flow remains: tool output → UI append →
      chat memory update unchanged.
    - Transcript features may subscribe to these events but must not
      reorder or block them.
  - Failure isolation:
    - Any exception in transcript-related hooks must be caught and
      logged without surfacing to the UI or interrupting the tool
      pipeline.
    - Live chat must remain usable even if transcript persistence is
      disabled or fails.
  - PlantUML sequence diagram (live session invariants):
    ```plantuml
    @startuml
    actor User
    participant AIChatPanel
    participant ChatMessageHistory
    participant ChatSessionMemoryController
    participant AIToolPipeline

    User -> AIChatPanel : send message
    AIChatPanel -> ChatMessageHistory : appendUserMessage(text)
    AIChatPanel -> ChatSessionMemoryController : addUserMessage(text)
    AIChatPanel -> AIToolPipeline : execute tools (unchanged)

    AIToolPipeline -> AIChatPanel : assistant reply
    AIChatPanel -> ChatMessageHistory : appendAssistantMessage(text)
    AIChatPanel -> ChatSessionMemoryController : addAssistantMessage(text)
    @enduml
    ```
- **Test specification:**
  - Verify live session tools still operate during transcript features.
  - Verify starting a new chat still resets UI history and in-memory
    tool session as before.
  - Verify transcript adapter failures do not block message display or
    tool execution.

## Subtask: Live chat list, switch, rename, and close
- **Status:** Planning
- **Scope:** Provide a UI list of active live chats, allow switching the
  active session, support renaming, and allow closing a live chat
  without affecting persisted transcripts.
- **Motivation:** Users need to manage multiple live conversations in a
  single app session before transcripts are persisted.
- **Developer Briefing:** Add a live chat manager UI to list active
  chats, allow switching the active chat, and support rename/close
  actions. This UI should be designed to later merge with persisted
  transcripts in a unified list.
- **Research:**
  - `AIChatPanel` currently manages a single live session and provides
    toolbar/popup actions but no session list.
  - Chat message history and memory are tied to the current session.
- **Design:**
  - Maintain an in-memory registry of live chat sessions, each with:
    - `sessionId` (runtime-only identifier).
    - `displayName` (auto-assigned, user-editable).
    - `mapIds` (runtime map identifiers referenced by the chat).
    - `messageHistory` (existing UI history).
    - `chatMemory` (LangChain4j memory / AIChatService state).
  - Record `mapIds` when a tool call includes a map id parameter.
  - Add a “Chats” action to open a list of live sessions.
  - Switching:
    - Save the current UI history and chat memory into the current
      session entry.
    - Load the selected session’s UI history and chat memory into the
      active panel.
    - Persist the transcript for the session being switched away from.
  - Renaming:
    - Allow inline rename; update only the live session’s display name.
  - Closing:
    - Allow closing a live session, removing it from the list.
    - Persist the transcript on close automatically; no explicit save.
  - Session memory strategy:
    - Keep a separate `ChatSessionMemoryController` per live session so
      switching does not require serialization of tool memory.
    - This keeps live session state authoritative and avoids partial
      restores.
  - Keep live session list separate from persisted transcripts for now,
    but design the list UI to support grouping/merging later.
  - Compute map root short text counts on demand from live `mapIds`
    when rendering the list; do not persist these for live sessions.
  - Implement the UI component under `org.freeplane.plugin.ai.chat`
    (for example `LiveChatListDialog` or `LiveChatListPanel`) and keep
    `org.freeplane.plugin.ai.chat.history` reserved for persisted
    transcript classes.
  - PlantUML class diagram (live chat manager):
    ```plantuml
    @startuml
    package "org.freeplane.plugin.ai.chat" {
      class AIChatPanel
      class LiveChatListDialog
    }

    package "org.freeplane.plugin.ai.chat.history" {
      class LiveChatSessionManager {
        +list(): List<LiveChatSessionSummary>
        +switchTo(sessionId: LiveChatSessionId): void
        +rename(sessionId: LiveChatSessionId, name: String): void
        +close(sessionId: LiveChatSessionId): void
        +createNew(name: String): LiveChatSessionId
      }

      class LiveChatSession {
        +id: LiveChatSessionId
        +displayName: String
        +mapIds: List<String>
        +messageHistory: ChatMessageHistory
        +chatMemory: ChatSessionMemoryController
      }

      class LiveChatSessionSummary {
        +id: LiveChatSessionId
        +displayName: String
        +mapIds: List<String>
      }

      class LiveChatSessionId {
        +value: String
      }
    }

    AIChatPanel --> LiveChatSessionManager
    AIChatPanel --> LiveChatListDialog
    LiveChatListDialog --> LiveChatSessionManager
    LiveChatSessionManager --> LiveChatSession
    LiveChatSessionManager --> LiveChatSessionSummary
    @enduml
    ```
- **Test specification:**
  - Manual: switching chats preserves distinct UI history and memory.
  - Manual: renaming updates the live chat list immediately.
  - Manual: closing a live chat removes it from the list.
  - Manual: switching or closing persists the transcript without
    prompting for save.
  - Manual: map root short texts are computed from live map ids and
    appear in the live chat list.

## Subtask: Transcript generation and persistence
- **Status:** Planning
- **Scope:** Define how transcripts are generated from live session
  messages and persisted on chat switch/close.
- **Motivation:** Transcripts must be derived from live sessions
  without disrupting tool-capable flow, while avoiding excessive gzip
  writes.
- **Developer Briefing:** Implement a live transcript adapter that
  mirrors user/assistant messages and persists on chat switch/close
  through the transcript store.
- **Research:**
  - `AIChatPanel` already receives user/assistant message events and
    updates `ChatMessageHistory`.
- **Design:**
  - Capture only user and assistant messages, ignoring tool output,
    tool calls, and runtime identifiers.
  - Capture at a consistent boundary (after each assistant response),
    so transcripts represent complete exchanges in-memory before a
    switch or close triggers persistence.
  - Keep a lightweight in-memory transcript shadow that is updated on
    new messages but is not used for live tool behavior.
  - Update the transcript shadow map root short text list whenever the
    set of maps in the live session changes (use each map’s root node
    text, shortened for display, grouped with counts).
  - Maintain a minimal adapter that mirrors `ChatMessageHistory`
    updates into a `ChatTranscriptRecord` without altering UI or tool
    logic.
  - Adapter responsibilities:
    - `startNewTranscript()` initializes the in-memory record and
      resets the edited-name flag.
    - `onUserMessage(text)` appends a `USER` entry and, if this is the
      first user message and the name is not edited, updates
      `displayName`.
    - `onAssistantMessage(text)` appends an `ASSISTANT` entry.
    - `updateMapRootShortTextCounts(list)` replaces the in-memory map
      root short text list with the latest shortened root node texts
      grouped with counts.
    - `renameLiveTranscript(name)` sets `displayName` and marks the
      name as edited to prevent auto-replacement.
    - `snapshotForSave()` returns an immutable copy for persistence.
  - Persistence triggers:
    - Flush the most recent in-memory transcript on chat switch.
    - Flush on chat close or shutdown.
  - Suggested data flow:
    - `AIChatPanel` already receives user/assistant message events;
      add adapter calls at the same points that update
      `ChatMessageHistory`.
    - This subtask depends on live session invariants: adapter calls
      are purely observational and must not reorder existing updates.
    - Do not read tool execution output for transcripts; the adapter
      only handles user and assistant text shown in the chat pane.
    - Record map references only when a tool call includes a map id
      parameter.
  - PlantUML component diagram (live capture flow):
    ```plantuml
    @startuml
    package "org.freeplane.plugin.ai.chat" {
      class AIChatPanel
      class ChatMessageHistory
      class ChatSessionMemoryController
    }

    package "org.freeplane.plugin.ai.chat.history" {
      class LiveTranscriptAdapter
      class ChatTranscriptRecord
      class ChatTranscriptStore
    }

    AIChatPanel --> ChatMessageHistory : appendMessage(...)
    AIChatPanel --> ChatSessionMemoryController : addMessage(...)
    AIChatPanel --> LiveTranscriptAdapter : onUserMessage(text)\n/onAssistantMessage(text)\n/startNewTranscript()\n/renameLiveTranscript(name)\n/updateMapRootShortTextCounts(list)
    LiveTranscriptAdapter --> ChatTranscriptRecord : updates entries\nand displayName
    LiveTranscriptAdapter --> ChatTranscriptStore : save on switch/close
    @enduml
    ```
  - PlantUML sequence diagram (message capture timing):
    ```plantuml
    @startuml
    actor User
    participant AIChatPanel
    participant ChatMessageHistory
    participant LiveTranscriptAdapter
    participant ChatSessionMemoryController
    participant ChatTranscriptStore

    User -> AIChatPanel : send message
    AIChatPanel -> ChatMessageHistory : appendUserMessage(text)
    AIChatPanel -> LiveTranscriptAdapter : onUserMessage(text)
    AIChatPanel -> ChatSessionMemoryController : addUserMessage(text)

    AIChatPanel -> ChatSessionMemoryController : addAssistantMessage(text)
    AIChatPanel -> ChatMessageHistory : appendAssistantMessage(text)
    AIChatPanel -> LiveTranscriptAdapter : onAssistantMessage(text)
    LiveTranscriptAdapter -> ChatTranscriptStore : save on switch/close
    @enduml
    ```
- **Test specification:**
  - Verify transcript capture preserves message order and user and
    assistant text.
  - Verify auto naming replaces the default name on first user message
    only when the name has not been edited.
  - Verify persistence runs on chat switch and close.
  - Verify transcript shadow map root short text counts update before
    persistence.
  - Verify live session tools still operate during capture.

## Subtask: Persisted transcript format and storage
- **Status:** Planning
- **Scope:** Specify the transcript schema, storage location, gzip JSON
  persistence, and list/load/delete/rename operations for transcripts.
- **Motivation:** A stable, readable, shareable transcript format is
  needed without embedding runtime state.
- **Developer Briefing:** Implement a transcript store under the user
  configuration directory with metadata for listing and retrieval,
  using gzip JSON for compactness in
  `org.freeplane.plugin.ai.chat.history`.
- **Research:**
  - `ResourceController.getFreeplaneUserDirectory()` provides a
    user-specific storage root.
  - Jackson `ObjectMapper` usage in the plugin indicates JSON
    serialization support is already present.
- **Design:**
  - Store transcripts under `${freeplaneUserDirectory}/ai-chats` with
    deterministic filenames based on timestamp and a sanitized name.
  - Transcript schema:
    - `timestamp` as the chat start time in epoch milliseconds.
    - `displayName` as the user-visible name (auto-assigned, then
      editable).
    - `mapRootShortTextCounts` as a list of shortened root node texts
      with counts for each map referenced by the chat.
    - `entries` as ordered `{role, text}` items for user and assistant.
  - `ChatTranscriptStore` responsibilities:
    - `save(record)` writes gzip JSON atomically via temp file + rename.
    - `list()` scans the directory, reads minimal metadata from each
      file, and returns items sorted by timestamp.
    - `load(id)` reads a full record for UI display.
    - `delete(id)` removes the file and returns success/failure.
    - `rename(id, displayName)` updates the stored name for a
      transcript (load + rewrite).
  - Define a simple `ChatTranscriptSummary` for list results (id,
    timestamp, displayName, mapRootShortTextCounts).
  - Define `ChatTranscriptId` as the filename token to avoid exposing
    raw paths to the UI.
  - Treat the persisted record as the serialized output of the live
    transcript shadow; do not invent or infer entries during
    save/load.
  - Skip unreadable or malformed files with a log entry and continue
    listing.
  - Keep all classes under `org.freeplane.plugin.ai.chat.history`.
  - PlantUML class diagram (storage layer):
    ```plantuml
    @startuml
    package "org.freeplane.plugin.ai.chat.history" {
      class ChatTranscriptStore {
        +save(record: ChatTranscriptRecord): void
        +list(): List<ChatTranscriptSummary>
        +load(id: ChatTranscriptId): ChatTranscriptRecord
        +delete(id: ChatTranscriptId): boolean
        +rename(id: ChatTranscriptId, displayName: String): void
      }

      class ChatTranscriptRecord {
        +timestamp: long
        +displayName: String
        +mapRootShortTextCounts: List<MapRootShortTextCount>
        +entries: List<ChatTranscriptEntry>
      }

      class ChatTranscriptEntry {
        +role: ChatTranscriptRole
        +text: String
      }

      enum ChatTranscriptRole {
        USER
        ASSISTANT
      }

      class ChatTranscriptSummary {
        +id: ChatTranscriptId
        +timestamp: long
        +displayName: String
        +mapRootShortTextCounts: List<MapRootShortTextCount>

      class MapRootShortTextCount {
        +text: String
        +count: int
      }
      }

      class ChatTranscriptId {
        +fileName: String
      }
    }
    ChatTranscriptStore --> ChatTranscriptRecord
    ChatTranscriptRecord "1" o-- "*" ChatTranscriptEntry
    ChatTranscriptSummary --> ChatTranscriptId
    @enduml
    ```
- **Test specification:**
  - Verify transcript serialization round-trips and list/delete/rename
    behavior.
  - Verify transcripts contain no runtime identifiers or tool payloads.
  - Verify list returns transcripts sorted by timestamp and skips
    malformed files.
  - Verify display name edits persist and appear in list summaries.
  - Verify map root short texts with counts persist and appear in list
    summaries.

## Subtask: Transcript list, load, and delete integration
- **Status:** Planning
- **Scope:** Surface stored transcripts in the chat panel, allow
  selecting a prior transcript to view or seed a new chat, and support
  deletion with immediate UI refresh.
- **Motivation:** Users need UI controls to manage saved transcripts
  without restarting the app.
- **Developer Briefing:** Add user interface actions in the chat panel
  to list saved transcripts, load a selection into the message view, and
  delete transcripts with immediate feedback.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    builds a toolbar and popup menu but currently only exposes
    preferences and AI edits actions.
  - The message view is a `JEditorPane` updated by
    `ChatMessageHistory`, and `startNewChat()` resets both the UI
    history and LangChain4j chat memory.
  - `org.freeplane.core.ui.textchanger.TranslatedElementFactory` is
    used elsewhere in `AIChatPanel` to create buttons and tooltips for
    localized UI labels.
  - `org.freeplane.core.util.TextUtils` is used in the chat panel for
    localized label text.
- **Design:**
  - Add a chat transcript action (toolbar button or menu item) that
    opens a simple list dialog/popup backed by the transcript store.
  - When a transcript is loaded for viewing: clear the existing UI
    history and rehydrate messages into `ChatMessageHistory` for
    read-only viewing.
  - Show each transcript’s display name and map root short text counts
    in the list row (format: `Map A (x2), Map B, Map C`).
  - Allow renaming a transcript from the list; persist the new name and
    refresh the list item immediately.
  - When a transcript is deleted: remove the file via the store and
    refresh the list without restarting the panel.
  - Save new transcript entries at consistent boundaries (after each
    assistant response) so the list stays current.
  - Use `TranslatedElementFactory` and `TextUtils` for all new UI
    labels, tooltips, and menu text, and read any toggle preferences
    via `ResourceController` to stay consistent with existing chat panel
    localization and settings patterns.
- **Test specification:**
  - Manual checklist: list shows saved transcripts, load replaces
    current chat content, delete removes entries immediately, rename
    updates the list.
  - Manual checklist: list rows show map root short text counts for
    each transcript.
  - If UI automation hooks exist, add tests for list selection, rename,
    and delete actions.

## Subtask: Start new chat from transcript with explicit map confirmation
- **Status:** Planning
- **Scope:** Provide a "start new chat from transcript" flow that seeds
  the new chat and enforces explicit map selection before map-specific
  actions.
- **Motivation:** Prevent silent misassociation of maps while still
  enabling reuse of prior conversations.
- **Developer Briefing:** The new chat starts with transcript context
  but no map bindings; map context is established only after explicit
  user confirmation.
- **Research:**
  - `AvailableMaps` provides runtime identifiers only for currently open
    maps.
  - Current tools require a map identifier to operate, and selection
    tools can provide current map identifiers for explicit user
    selection.
- **Design:**
  - Insert a system message when starting from transcript that instructs
    the assistant to request map confirmation for any map-specific
    action.
  - Provide a UI affordance to select or confirm maps (for example by
    reusing existing selection tools or a map picker), then proceed with
    tool-capable actions.
  - Ensure the assistant remains map-agnostic until confirmation is
    recorded.
- **Test specification:**
  - Verify the system message is injected for transcript-based chats.
  - Verify map-specific actions are blocked or deferred until explicit
  confirmation is provided.

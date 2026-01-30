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

  ```plantuml
  @startuml
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class LiveChatController
    class LiveChatSessionManager
    class LiveChatListDialog
    class ChatMessageHistory
    class ChatSessionMemoryController
    class ChatMessageStyleApplier
  }
  package "org.freeplane.plugin.ai.chat.history" {
    class ChatTranscriptStore
    class ChatTranscriptRecord
  }
  package "org.freeplane.plugin.ai.maps" {
    class AvailableMaps
  }
  AIChatPanel --> LiveChatController
  AIChatPanel --> ChatMessageHistory
  AIChatPanel --> ChatMessageStyleApplier
  AIChatPanel --> AvailableMaps
  LiveChatController --> LiveChatSessionManager
  LiveChatController --> LiveChatListDialog
  LiveChatController --> ChatMessageHistory
  LiveChatController --> ChatSessionMemoryController
  LiveChatSessionManager --> ChatTranscriptStore : persist on switch/close
  ChatTranscriptStore --> ChatTranscriptRecord
  @enduml
  ```

  `AIChatPanel` owns the UI and delegates live-session lifecycle to
  `LiveChatController`.
  `ChatMessageStyleApplier` centralizes theme-aware message styling.
  `LiveChatController` coordinates list dialogs, session switching, and
  history snapshot/restore.
  `AvailableMaps.findMapModel` exposes overloads with and without a map
  access callback; only tool flows pass the callback so UI rendering
  does not affect per-chat map tracking.
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
- **Status:** Finished
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

  ```plantuml
  @startuml
  actor User
  participant AIChatPanel
  participant LiveChatController
  participant ChatMessageHistory
  participant ChatSessionMemoryController
  participant AIToolPipeline
  User -> AIChatPanel : send message
  AIChatPanel -> ChatMessageHistory : appendUserMessage(text)
  AIChatPanel -> ChatSessionMemoryController : addUserMessage(text)
  AIChatPanel -> AIToolPipeline : execute tools
  AIToolPipeline -> AIChatPanel : assistant reply
  AIChatPanel -> ChatMessageHistory : appendAssistantMessage(text)
  AIChatPanel -> ChatSessionMemoryController : addAssistantMessage(text)
  User -> AIChatPanel : switch chat
  AIChatPanel -> LiveChatController : switch session
  LiveChatController -> ChatMessageHistory : snapshot/restore
  LiveChatController -> ChatSessionMemoryController : replace controller
  @enduml
  ```

  Message send flow remains unchanged; `LiveChatController` handles
  session switching by snapshotting history and swapping memory.
- **Test specification:**
  - Verify live session tools still operate during transcript features.
  - Verify starting a new chat still resets UI history and in-memory
    tool session as before.
  - Verify transcript adapter failures do not block message display or
    tool execution.


## Subtask: Live chat list, switch, rename, and close
- **Status:** Finished
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

  ```plantuml
  @startuml
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class LiveChatController
    class LiveChatListDialog
    class LiveChatSessionManager
    class LiveChatSession
    class LiveChatSessionSummary
  }
  AIChatPanel --> LiveChatController
  LiveChatController --> LiveChatListDialog
  LiveChatController --> LiveChatSessionManager
  LiveChatListDialog --> LiveChatController
  LiveChatSessionManager --> LiveChatSession
  LiveChatSessionManager --> LiveChatSessionSummary
  @enduml
  ```

  `AIChatPanel` owns the chat UI and delegates session management to
  `LiveChatController`.
  `LiveChatController` coordinates dialog actions and session changes.
  `LiveChatSessionManager` tracks live sessions and handles switch,
  rename, and close operations.
  `LiveChatSession` stores per-session memory and message history.
  `LiveChatSessionSummary` provides lightweight list rows.
  `LiveChatListDialog` is the modal UI list for switching/renaming.
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

  ```plantuml
  @startuml
  participant AIChatPanel
  participant LiveChatSessionManager
  participant LiveTranscriptAdapter
  participant ChatTranscriptStore
  AIChatPanel -> LiveTranscriptAdapter : onUserMessage/onAssistantMessage
  AIChatPanel -> LiveChatSessionManager : switch chat
  LiveTranscriptAdapter -> ChatTranscriptStore : save(snapshot)
  AIChatPanel -> LiveChatSessionManager : close chat
  LiveTranscriptAdapter -> ChatTranscriptStore : save(snapshot)
  @enduml
  ```

  Transcript shadow updates per message; persistence happens on
  switch/close.
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
    }
    class ChatTranscriptId { 
      +fileName: String
    }
    class MapRootShortTextCount {
      +text: String
      +count: int
    }
  }
  ChatTranscriptStore --> ChatTranscriptRecord
  ChatTranscriptRecord "1" o-- "*" ChatTranscriptEntry
  ChatTranscriptSummary --> ChatTranscriptId
  @enduml
  ```

  `ChatTranscriptStore` handles gzip JSON I/O.
  `ChatTranscriptRecord` is the on-disk payload.
  `ChatTranscriptSummary` is list metadata for UI.
  `MapRootShortTextCount` deduplicates map labels with counts.
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

  ```plantuml
  @startuml
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class TranscriptListDialog
    class ChatMessageHistory
  }
  package "org.freeplane.plugin.ai.chat.history" {
    class ChatTranscriptStore
  }
  AIChatPanel --> TranscriptListDialog
  TranscriptListDialog --> ChatTranscriptStore
  TranscriptListDialog --> ChatMessageHistory : load for viewing
  @enduml
  ```
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

  ```plantuml
  @startuml
  actor User
  participant TranscriptListDialog
  participant AIChatPanel
  participant ChatSessionMemoryController
  User -> TranscriptListDialog : start new chat from transcript
  TranscriptListDialog -> AIChatPanel : load transcript text
  AIChatPanel -> ChatSessionMemoryController : seed memory + system guard
  User -> AIChatPanel : confirm map context
  @enduml
  ```
- **Test specification:**
  - Verify the system message is injected for transcript-based chats.
  - Verify map-specific actions are blocked or deferred until explicit
  confirmation is provided.

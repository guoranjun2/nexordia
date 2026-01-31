# Task: Tool-capable chat history as transcripts
- **Task Identifier:** 2026-01-24-chat-history-transcripts
- **Scope:** Persist chat history as transcript-only gzip JSON records in
  `${freeplaneUserDirectory}/ai-chats`, list saved transcripts alongside
  live chats, allow loading and deletion, capture user/assistant
  messages only, reuse live chat names for transcripts with user
  editability, show names and map root short texts with counts in the
  unified chat list, and enable users to start a new tool-capable chat
  from a transcript with explicit map confirmation.
- **Motivation:** Persisted tool-capable chats cannot safely resume
  runtime state across sessions, so the system must avoid silent map
  reassociation while still supporting review and reuse of past
  conversations.
- **Developer Briefing:** Treat persisted chat history as transcripts,
  not resumable sessions. Live sessions remain tool-capable in-memory,
  while saved transcripts store only user and assistant text. Live
  chats and saved transcripts share an auto-assigned name that the user
  can edit in the unified list. Map root short text counts exist to
  disambiguate duplicate short texts. Users can list, load, rename, and
  delete transcripts, or start a new tool-capable chat from one with
  explicit map confirmation before map-specific tools run. Use the
  `org.freeplane.plugin.ai.chat.history` package for the transcript
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
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class LiveChatController
    class LiveChatSessionManager
    class ChatListDialog
    class ChatMessageHistory
    class ChatSessionMemoryController
    class ChatMessageStyleApplier
    class ChatTranscriptStore
    class ChatTranscriptRecord
    class AvailableMaps
  }
  AIChatPanel --> LiveChatController : delegate session lifecycle
  AIChatPanel --> ChatMessageHistory : append/render messages
  AIChatPanel --> ChatMessageStyleApplier : apply theme styles
  AIChatPanel --> AvailableMaps : resolve map access
  LiveChatController --> LiveChatSessionManager : manage sessions
  LiveChatController --> ChatListDialog : open unified list
  LiveChatController --> ChatMessageHistory : snapshot/restore
  LiveChatController --> ChatSessionMemoryController : activate memory
  LiveChatController --> ChatTranscriptStore : persist transcripts
  ChatListDialog --> LiveChatSessionManager : list live sessions
  ChatListDialog --> ChatTranscriptStore : list transcripts
  ChatListDialog --> ChatMessageHistory : load transcript view
  LiveChatSessionManager --> ChatTranscriptStore : persist on switch/close
  ChatTranscriptStore --> ChatTranscriptRecord : load/save
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


## Subtask: Transcript generation and persistence
- **Status:** Planning
- **Scope:** Define how transcripts are generated from live session
  messages and persisted on chat switch/close.
- **Motivation:** Transcripts must be derived from live sessions
  without disrupting tool-capable flow, while avoiding excessive gzip
  writes.
- **Developer Briefing:** Implement a live transcript adapter that
  mirrors user/assistant messages and persists on chat switch/close
  through the transcript store. When a live chat is started from a
  transcript, persist back into the same transcript record on
  switch/close. Map root short text counts should reflect duplicate
  root texts when multiple maps share a label.
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
  LiveTranscriptAdapter -> ChatTranscriptStore : save(snapshot, id)
  AIChatPanel -> LiveChatSessionManager : close chat
  LiveTranscriptAdapter -> ChatTranscriptStore : save(snapshot, id)
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
  `org.freeplane.plugin.ai.chat.history`. Schema should align with the
  in-memory data model.
- **Research:**
  - `ResourceController.getFreeplaneUserDirectory()` provides a
    user-specific storage root.
  - Jackson `ObjectMapper` usage in the plugin indicates JSON
    serialization support is already present.
- **Design:**

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
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
  ChatTranscriptStore --> ChatTranscriptRecord : load/save
  ChatTranscriptRecord "1" o-- "*" ChatTranscriptEntry : entries
  ChatTranscriptRecord "1" o-- "*" MapRootShortTextCount : map counts
  ChatTranscriptSummary --> ChatTranscriptId : identifier
  ChatTranscriptSummary "1" o-- "*" MapRootShortTextCount : map counts
  @enduml
  ```

  `ChatTranscriptStore` handles gzip JSON I/O.
  `ChatTranscriptRecord` is the on-disk payload.
  `ChatTranscriptSummary` is list metadata for UI.
  `MapRootShortTextCount` deduplicates map labels with counts.
  Transcripts are stored under
  `${freeplaneUserDirectory}/ai-chats`.
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
  to list live chats and saved transcripts together, load a selection
  into the message view, and delete transcripts with immediate
  feedback. The list should indicate which entry is currently loaded
  (for example with a checkbox or icon). Rename and delete actions
  should be available from the unified list dialog.
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
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class ChatListDialog
    class ChatMessageHistory
    class LiveChatSessionManager
    class ChatListItem
    class ChatTranscriptStore
  }
  AIChatPanel --> ChatListDialog : open unified list
  ChatListDialog --> ChatListItem : rows
  ChatListDialog --> LiveChatSessionManager : live entries
  ChatListDialog --> ChatTranscriptStore : transcript entries
  ChatListDialog --> ChatMessageHistory : load for viewing
  @enduml
  ```
- **Test specification:**
  - Manual checklist: list shows live chats and saved transcripts, load
    replaces current chat content, delete removes entries immediately,
    rename updates the list.
  - Manual checklist: list rows show map root short text counts for
    each transcript.
  - Manual checklist: list marks which entry is currently loaded using
    a checkbox or icon.
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
  user confirmation. Seed a system prompt clause stating that maps must
  be shown by the user and the assistant may ask for them when needed.
  Do not include root node names in the system prompt until a map is
  explicitly confirmed. When the chat is started from a transcript,
  reuse the same list entry and persist updates back into that
  transcript on save.
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
  participant ChatListDialog
  participant AIChatPanel
  participant ChatSessionMemoryController
  User -> ChatListDialog : start new chat from transcript
  ChatListDialog -> AIChatPanel : load transcript text
  AIChatPanel -> ChatSessionMemoryController : seed memory + system guard
  User -> AIChatPanel : confirm map context
  AIChatPanel -> ChatSessionMemoryController : attach map context
  @enduml
  ```
- **Test specification:**
  - Verify the system message is injected for transcript-based chats
    stating maps must be shown by the user and the assistant may ask.
  - Verify root node names are not added to the system prompt until
    explicit map confirmation is provided.
  - Verify map-specific actions are blocked or deferred until explicit
    confirmation is provided.

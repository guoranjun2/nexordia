# Task: Tool-capable chat history as transcripts
- **Task Identifier:** 2026-01-24-chat-history-transcripts
- **Scope:** Persist chat history as transcript-only gzip JSON records in
  `${freeplaneUserDirectory}/ai-chats`, list saved transcripts alongside
  live chats, allow switching and deletion, capture user/assistant
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
  disambiguate duplicate short texts and must accumulate across saves.
  Counts are based on distinct map UUIDs that share the same root text,
  so repeated access to the same map does not increase the counter.
  When persisting a live chat, merge the current session map root short
  text counts with any existing transcript counts, using the max count
  when a short text appears in both. The unified list shows a status
  icon (green for live, yellow for transcript, red for error). Users
  can list, start, rename, and delete transcripts, or start a new
  tool-capable chat from one with explicit map confirmation before
  map-specific tools run. Use the
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
  package "org.freeplane.plugin.ai" {
    package "chat" {
      class AIChatPanel
      class LiveChatController
      class LiveChatSessionManager
      class ChatListDialog
      class ChatMessageHistory
      class ChatSessionMemoryController
      class ChatMessageStyleApplier
    }
    package "chat.history" {
      class ChatTranscriptStore
      class ChatTranscriptRecord
    }
    package "maps" {
      class AvailableMaps
    }
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
  ChatListDialog --> LiveChatSessionManager : switch/start session
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
  - Verify list status icon shows green for live, yellow for
    transcript, and red for error entries.
  - Manual verification that live sessions remain fully tool-capable
    during the same application session.


## Subtask: Transcript generation and persistence
- **Status:** Implementation Review
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
  root texts when multiple maps share a label. Renames made in the
  unified list should persist on the next save or on close.
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
- **Status:** Implementation Review
- **Scope:** Specify the transcript schema, storage location, gzip JSON
  persistence, and list/start/delete/rename operations for transcripts.
- **Motivation:** A stable, readable, shareable transcript format is
  needed without embedding runtime state.
- **Developer Briefing:** Implement a transcript store under the user
  configuration directory with metadata for listing and retrieval,
  using gzip JSON for compactness in
  `org.freeplane.plugin.ai.chat.history`. Schema should align with the
  in-memory data model. Use UUID-based filenames. Sort list entries by
  most recent save time (timestamp updated on each save), and allow
  date-based folder splits if storage grouping changes with the save
  timestamp.
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
  - Verify list returns transcripts sorted by timestamp and marks
    malformed files as error entries.
  - Unit test: `ChatTranscriptStore.save` writes gzip JSON, returns a
    stable id, and `load` restores display name, entries, and map root
    short text counts.
  - Unit test: `ChatTranscriptStore.list` orders summaries by
    timestamp descending, includes error entries for malformed files,
    and populates `errorMessage`.
  - Unit test: `ChatTranscriptStore.rename` updates display name and
    refreshes timestamp without changing the transcript id.
  - Unit test: `ChatTranscriptStore.delete` removes the file and list
    excludes the transcript after deletion.
  - Unit test: map root short text count merging keeps all prior
    values and uses max counts when the same short text appears in
    both live and transcript data.
  - Verify display name edits persist and appear in list summaries.
  - Verify map root short texts with counts persist and appear in list
    summaries.


## Subtask: Transcript list, switch, and delete integration
- **Status:** Implementation Review
- **Scope:** Surface stored transcripts in the chat panel, allow
  switching to live chats or starting a new chat from a transcript, and
  support deletion with immediate UI refresh.
- **Motivation:** Users need UI controls to manage saved transcripts
  without restarting the app.
- **Developer Briefing:** Add user interface actions in the chat panel
  to list live chats and saved transcripts together, switch to a live
  chat or start a new chat from a transcript selection, and delete
  transcripts with immediate feedback. The list should indicate which
  entry is currently loaded
  (for example with a checkbox or icon). The list should show a status
  icon for each entry (green live, yellow transcript, red error).
  Rename and delete actions should be available from the unified list
  dialog. The Close button must close the dialog and do nothing else,
  and it must always be enabled. The Switch button must activate the
  selected chat and close the dialog. The Delete button must delete the
  selected transcript, and for live chats it must delete both the live
  chat and its persisted transcript (if present). If the current live
  chat is deleted, the message pane must reset to an empty chat (the
  same behavior as starting a new chat).
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
  ChatListDialog --> ChatListItem : rows + status icon
  ChatListDialog --> LiveChatSessionManager : live entries
  ChatListDialog --> ChatTranscriptStore : transcript entries
  ChatListDialog --> LiveChatSessionManager : activate selection
  @enduml
  ```
- **Test specification:**
  - Unit test: `ChatListTableModel.refresh` merges live sessions with
    transcript summaries, removes duplicates, and sorts by timestamp
    descending.
  - Unit test: `ChatListTableModel` marks the loaded row via
    `loadedTranscriptSupplier` and propagates status icons per entry
    type (live, transcript, error).
  - Unit test: `ChatListDialog.openChat` invokes `startChatFromTranscript`
    for transcript-only rows and `switchTo` for live sessions, then
    closes the dialog.
  - Unit test: `ChatListDialog.deleteChat` deletes both live session
    and transcript when present, and refreshes the table model.
  - Unit test: `ChatListDialog.closeDialog` only disposes the dialog,
    and the close button is always enabled.


## Subtask: Start new chat from transcript with explicit map confirmation
- **Status:** Implementation Review
- **Scope:** Provide a "start new chat from transcript" flow that seeds
  the new chat and enforces explicit map selection before map-specific
  actions.
- **Motivation:** Prevent silent misassociation of maps while still
  enabling reuse of prior conversations.
- **Developer Briefing:** The new chat starts with transcript context
  but no map bindings; map context is established only after explicit
  user confirmation. Seed a system prompt clause stating that maps must
  be shown by the user and the assistant may ask for them when needed.
  Do not include root node names in the system prompt. When a chat is
  started from a transcript, inject a system message that warns the
  model the currently opened map may differ from maps discussed in the
  transcript and that it should ask the user to confirm map context
  when needed. When the chat is started from a transcript, reuse the
  same list entry and persist updates back into that transcript on save
  (updating the saved timestamp).
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
  ChatListDialog -> AIChatPanel : start chat from transcript
  AIChatPanel -> ChatSessionMemoryController : seed memory + system warning
  @enduml
  ```
- **Test specification:**
  - Verify transcript-based chats seed chat memory with transcript
    entries followed by the hidden system-annotated user message and
    hidden assistant "ok" reply.
  - Verify the hidden exchange is not persisted to transcript storage.
  - Verify the hidden exchange is rendered in the message view only
    when debugging is enabled and appears after restored transcript
    messages.
  - Verify the system prompt itself remains unchanged (no root node
    names added).
  - Unit test: `ChatSessionMemoryController.seedTranscriptWithHiddenExchange`
    preserves ordering (transcript entries first, then hidden user,
    then hidden assistant) and roles (user then assistant).

# Task: AI chat turn undo/redo and interrupted input recovery
- **Task Identifier:** 2026-02-06-chat-turn
- **Scope:** Keep interrupted user requests in the chat input field and
  add `Undo` and `Redo` controls near send in AI chat. `Undo` must
  rewind the conversation by one sent turn, restore that user message
  into the input, and remove the rewound turn from active chat context.
  `Redo` must restore previously rewound turns until a new user message
  is sent, after which redo history is cleared.
- **Motivation:** Users need safe iterative prompting without losing
  interrupted drafts and without manually deleting recent chat context.
- **Developer Briefing:** The current panel already restores one pending
  user message when cancellation is used during an active request. The
  new behavior extends this to multi-step conversation rewind and
  forward navigation at turn granularity, while keeping chat memory,
  on-screen history, and transcript entries consistent.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    keeps request lifecycle state in `pendingHistorySnapshot`,
    `pendingMemorySnapshot`, `pendingTranscriptEntries`, and
    `pendingUserMessage`.
  - `AIChatPanel.beginRequest(...)` snapshots history, chat memory, and
    transcript entries before appending the new user message.
  - `AIChatPanel.cancelActiveRequest()` and
    `AIChatPanel.restoreCancelledRequest()` restore those snapshots and
    put `pendingUserMessage` back into the input field.
  - `AIChatPanel.finishRequest()` clears pending request state after a
    successful completion, so there is no persisted multi-step undo
    history for already completed turns.
  - The input area currently adds only one action button on the east
    side (`sendButton`) and no dedicated undo/redo controls.
  - `ChatMessageHistory` supports full snapshot/restore of rendered
    chat entries through `ChatMessageSnapshot`.
  - `ChatSessionMemoryController` supports full snapshot/restore of
    LangChain4j `ChatMessage` lists.
  - `LiveChatController` supports snapshot/restore of transcript entries
    for the active live session and already records user/assistant
    transcript entries per turn.
  - Chat translation keys for send/cancel/chats are present in
    `freeplane/src/viewer/resources/translations/Resources_en.properties`;
    no undo/redo chat keys exist yet.
  - Existing tests in `freeplane_plugin_ai/src/test/java/.../chat`
    cover memory and transcript helpers, but there is no focused test
    suite for `AIChatPanel` request lifecycle and rewind UI controls.
- **Design:**

  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "ChatTurnRewindController" as Rewind
  participant "AIChatService" as ChatService

  User -> ChatPanel : send user message
  ChatPanel -> Rewind : captureBeforeTurn(snapshot, userMessage)
  ChatPanel -> ChatService : chat(userMessage)
  ChatService --> ChatPanel : assistant response
  ChatPanel -> Rewind : captureAfterTurn(snapshot)
  ChatPanel -> Rewind : clearRedo()

  User -> ChatPanel : click Undo
  ChatPanel -> Rewind : undo(currentSnapshot)
  Rewind --> ChatPanel : restoreSnapshot + input userMessage

  User -> ChatPanel : click Redo
  ChatPanel -> Rewind : redo(currentSnapshot)
  Rewind --> ChatPanel : restoreSnapshot + input afterTurnInput

  User -> ChatPanel : send new message after undo
  ChatPanel -> Rewind : clearRedo()
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class ChatTurnRewindController
    class ChatConversationSnapshot
    class ChatTurnRewindStep
    class LiveChatSession
  }

  AIChatPanel --> ChatTurnRewindController : manage undo/redo
  ChatTurnRewindController --> ChatConversationSnapshot : stores
  ChatTurnRewindController --> ChatTurnRewindStep : pushes/pops
  LiveChatSession --> ChatTurnRewindController : per-session state
  @enduml
  ```

  - Add a compact button group near `sendButton` with `Undo` and `Redo`
    icons or localized labels, and enable each button only when the
    corresponding action is available.
  - Introduce `ChatConversationSnapshot` with:
    `messageSnapshots`, `memoryMessages`, `transcriptEntries`, and
    `inputText`.
  - Introduce `ChatTurnRewindStep` with:
    `beforeTurnSnapshot`, `afterTurnSnapshot`, and `userMessage`.
  - Introduce `ChatTurnRewindController` that owns two stacks:
    `undoSteps` and `redoSteps`, and exposes:
    `recordCompletedTurn(...)`, `undo(...)`, `redo(...)`,
    `clearRedo()`, `clearAll()`, `canUndo()`, and `canRedo()`.
  - Integrate with request lifecycle in `AIChatPanel`:
    - On send start: capture the `before` snapshot for the pending turn.
    - On successful assistant completion: capture `after` snapshot and
      push a completed step to undo; clear redo.
    - On cancellation/interruption before completion: restore pending
      state and keep user message in input without recording a new undo
      step.
  - Undo behavior:
    - Restore `beforeTurnSnapshot`.
    - Put `userMessage` into input with caret at end.
    - Push corresponding step into redo stack.
  - Redo behavior:
    - Restore `afterTurnSnapshot`.
    - Restore `afterTurnSnapshot.inputText` (normally empty).
    - Push step back into undo stack.
  - New message after at least one undo must clear redo immediately
    before dispatching the new request.
  - Keep rewind history per live chat session by storing rewind
    controller state in `LiveChatSession` and reattaching it on session
    activation; starting a new chat initializes empty rewind stacks.
  - Rewind/redo actions are disabled while a request is active.
- **Test specification:**
  - Automated tests:
    - Add focused tests for `ChatTurnRewindController`:
      `undo_restores_previous_snapshot_and_user_input`,
      `redo_restores_rewound_turn`,
      `sending_new_message_after_undo_clears_redo`.
    - Add `AIChatPanel` lifecycle tests for interruption behavior:
      `cancelled_request_restores_pending_user_message_to_input`.
    - Add `AIChatPanel` tests verifying button state transitions:
      `undo_redo_buttons_enabled_only_with_available_steps`,
      `undo_redo_disabled_while_request_active`.
    - Add a session-switch test verifying undo/redo history isolation
      between live sessions.
  - Manual tests:
    - Send two turns, undo twice, verify both user messages are brought
      back in reverse order and chat context shrinks.
    - After undo, redo until latest state, then send a new message and
      verify redo is cleared.
    - Interrupt an in-flight request and verify input restores the
      interrupted user text.

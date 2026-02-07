# Task: AI chat turn undo/redo and interrupted input recovery
- **Task Identifier:** 2026-02-06-chat-turn
- **Scope:** Keep interrupted user requests in the chat input field and
  add `Undo` and `Redo` controls near send in AI chat. `Undo` must
  rewind the conversation by one sent turn, restore that user message
  into the input, and remove the rewound turn from active chat context.
  `Redo` must restore previously rewound turns until a new user message
  is sent, after which redo history is cleared. Add keyboard shortcuts
  for `Undo` and `Redo` aligned with send shortcut handling:
  `Command/Ctrl + ArrowUp` and `Command/Ctrl + ArrowDown`.
- **Motivation:** Users need safe iterative prompting without losing
  interrupted drafts and without manually deleting recent chat context.
- **Developer Briefing:** The current panel already restores one pending
  user message when cancellation is used during an active request. The
  new behavior extends this to multi-step conversation rewind and
  forward navigation at turn granularity. The target architecture uses
  memory-owned cursor movement, not snapshot stacks.
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
  - Send action key handling in `AIChatPanel` already uses
    `MenuUtils.formatKeyStroke(...)` in tooltip text and action map
    bindings from panel/input maps.
  - `ChatMessageHistory` supports full snapshot/restore of rendered
    chat entries through `ChatMessageSnapshot`.
  - `LiveChatController` supports snapshot/restore of transcript entries
    for the active live session and already records user/assistant
    transcript entries per turn.
  - In-progress task
    `ai-specs/tasks/in-progress/unified-conversation-memory.md`
    moves turn ownership to `AssistantProfileChatMemory` and removes
    `ChatSessionMemoryController`.
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
  participant "AssistantProfileChatMemory" as ChatMemory
  participant "AIChatService" as ChatService

  User -> ChatPanel : send user message
  ChatPanel -> ChatMemory : appendUserTurnPart(userMessage)
  ChatPanel -> ChatService : chat(userMessage)
  ChatService --> ChatPanel : assistant response
  ChatPanel -> ChatMemory : appendAssistantTurnPart(response)

  User -> ChatPanel : click Undo
  ChatPanel -> ChatMemory : moveCursor(back)
  ChatMemory --> ChatPanel : activeTimeline + rewoundUserMessage

  User -> ChatPanel : click Redo
  ChatPanel -> ChatMemory : moveCursor(forward)
  ChatMemory --> ChatPanel : activeTimeline

  User -> ChatPanel : send new message after undo
  ChatPanel -> ChatMemory : appendUserTurnPart(newMessage)
  ChatMemory -> ChatMemory : clearForwardBranch
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class AssistantProfileChatMemory
    interface ConversationCursorNavigator
    interface ConversationMessagesView
    class LiveChatSession
  }

  AssistantProfileChatMemory ..|> ConversationCursorNavigator
  AssistantProfileChatMemory ..|> ConversationMessagesView
  AIChatPanel --> ConversationCursorNavigator : undo/redo
  AIChatPanel --> ConversationMessagesView : render + restore input
  LiveChatSession --> AssistantProfileChatMemory : per-session owner
  @enduml
  ```

  - Add a compact button group near `sendButton` with `Undo` and `Redo`
    icons or localized labels, and enable each button only when the
    corresponding action is available.
  - Use one memory-owned turn chain with a current cursor.
  - Integrate with request lifecycle in `AIChatPanel`:
    - On send start: keep interrupted request text in input draft state.
    - On successful assistant completion: append completed turn to
      memory-owned chain.
    - On cancellation/interruption before completion: keep user message
      in input and do not append a completed assistant turn.
  - Undo behavior:
    - Move memory cursor one completed turn back.
    - Render active timeline from memory.
    - Put rewound turn user text into input with caret at end.
  - Redo behavior:
    - Move memory cursor one completed turn forward.
    - Render active timeline from memory.
    - Keep input empty unless another draft is present.
  - New message after at least one undo clears forward branch in memory.
  - Keep cursor state per live chat session by storing memory owner in
    `LiveChatSession`; switching sessions restores each cursor naturally.
  - Rewind/redo actions are disabled while a request is active.
  - Add `Command/Ctrl + ArrowUp` and `Command/Ctrl + ArrowDown`
    key bindings for undo/redo on the same input container scope as
    cancel handling, so they work while focus stays in chat input
    controls.
  - Show undo/redo shortcuts in button tooltips using formatted
    keystroke text, analogous to send tooltip behavior.
- **Test specification:**
  - Automated tests:
    - Add focused tests for memory cursor behavior:
      `undo_moves_cursor_back_and_returns_user_text`,
      `redo_moves_cursor_forward`,
      `sending_new_message_after_undo_clears_forward_branch`.
    - Add `AIChatPanel` lifecycle tests for interruption behavior:
      `cancelled_request_restores_pending_user_message_to_input`.
    - Add `AIChatPanel` tests verifying button state transitions:
      `undo_redo_buttons_enabled_only_with_available_steps`,
      `undo_redo_disabled_while_request_active`.
    - Add `AIChatPanel` tests for shortcut dispatch:
      `undo_shortcut_triggers_undo`,
      `redo_shortcut_triggers_redo`.
    - Add a session-switch test verifying undo/redo history isolation
      between live sessions.
  - Manual tests:
    - Send two turns, undo twice, verify both user messages are brought
      back in reverse order and chat context shrinks.
    - After undo, redo until latest state, then send a new message and
      verify redo is cleared.
    - Interrupt an in-flight request and verify input restores the
      interrupted user text.
    - Press `Command/Ctrl + ArrowUp` and `Command/Ctrl + ArrowDown`
      in chat input and verify they trigger `Undo`/`Redo` and appear
      in button tooltips.

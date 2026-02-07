# Task: Unified conversation memory
- **Task Identifier:** 2026-02-06-conversation-memory
- **Scope:** Move AI chat conversation ownership to
  `AssistantProfileChatMemory` and remove
  `ChatSessionMemoryController`. Keep live chat orchestration focused in
  one runtime class and apply interface segregation so each client
  depends only on the memory capabilities it needs. `Undo` and `Redo`
  must be fully memory-owned with no external state copies. Tool-call
  messages produced by the LLM must remain memory-backed and visible
  after normal panel refresh.
- **Motivation:** Conversation data is currently routed through an extra
  controller layer, which obscures ownership and duplicates flow logic.
  One owner class with segregated interfaces keeps integration with
  LangChain4j clear and reduces accidental complexity.
- **Developer Briefing:** This task is design-first and keeps behavior
  stable. Do not introduce parallel conversation-state abstractions.
  Keep memory ownership and eviction policy in memory classes. Do not
  add external undo stacks, snapshots, or delta copies outside
  `AssistantProfileChatMemory`. MCP tool-call messages are UI-transient:
  visible in the active panel until undo/redo or chat switch.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AssistantProfileChatMemory.java`
    already encapsulates Freeplane-specific memory semantics
    (assistant profile instructions, control markers, and message-window
    eviction).
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryController.java`
    currently wraps lazy memory creation, transcript seeding, and
    snapshot-style operations around `AssistantProfileChatMemory`.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    currently depends on the controller for request lifecycle and cancel
    restore behavior.
  - Panel rebuild currently replays transcript/session entries, so
    messages that exist only as direct UI appends disappear after full
    rebuild.
  - LLM tool-call messages and MCP tool-call messages have different
    ownership:
    - LLM tool calls are part of conversational state and should be
      memory-backed.
    - MCP tool calls are outside chat-memory ownership and remain
      UI-transient by design.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/LiveChatSession.java`
    owns runtime `transcriptEntries` and other per-session fields.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/LiveChatController.java`
    currently orchestrates session lifecycle and transcript
    persistence. This task keeps that orchestration point and extracts
    only mapping responsibility into a focused mapper.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatServiceFactory.java`
    currently reads `ChatMemory` through the controller.
  - The token-window task
    `ai-specs/tasks/backlog/token-based-chat-memory-window.md`
    requires eviction to stay in the memory layer and should not add a
    second eviction owner.
  - The undo/redo task
    `ai-specs/tasks/backlog/ai-chat-turn-undo-redo-and-interrupted-input.md`
    simplifies if cursor movement and turn ownership are exposed
    directly by memory interfaces.
- **Design:**

  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "AssistantProfileChatMemory" as ChatMemory
  participant "LiveChatController" as LiveChat
  participant "TranscriptMemoryMapper" as Mapper
  participant "ChatTranscriptStore" as TranscriptStore
  participant "AIChatServiceFactory" as ServiceFactory

  User -> ChatPanel : send/cancel/undo/redo
  ChatPanel -> ChatMemory : appendUserTurnPart(...)
  ChatPanel -> ChatMemory : appendAssistantTurnPart(...)
  ChatPanel -> ChatMemory : appendToolCallTurnPart(...)
  ChatPanel -> ChatMemory : undo()/redo()
  ChatPanel -> ChatMemory : currentConversationMessages()
  ChatPanel -> ChatPanel : appendTransientMcpToolCall(...)
  ChatPanel -> ChatPanel : rebuildHistoryFromCurrentState()
  ChatPanel -> LiveChat : persistCurrentSessionIfNeeded()
  LiveChat -> LiveChat : currentSession()
  LiveChat -> Mapper : mapSessionToTranscriptEntries(currentSession)
  LiveChat -> TranscriptStore : save/load/rename/delete
  LiveChat -> Mapper : mapTranscriptToMemorySeed(currentSession, entries)
  Mapper -> ChatMemory : seed transcript context when restoring
  ServiceFactory -> ChatMemory : use as ChatMemory
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    interface ConversationTurnWriter
    interface ConversationCursorNavigator
    interface ConversationMessagesView
    interface ConversationTranscriptSeeder
    class AssistantProfileChatMemory
    class AIChatPanel
    class LiveChatController
    class TranscriptMemoryMapper
    class ChatTranscriptStore
    class LiveChatSession
    class ChatTranscriptEntry
    class AIChatServiceFactory
  }

  AssistantProfileChatMemory ..|> ConversationTurnWriter
  AssistantProfileChatMemory ..|> ConversationCursorNavigator
  AssistantProfileChatMemory ..|> ConversationMessagesView
  AssistantProfileChatMemory ..|> ConversationTranscriptSeeder
  AssistantProfileChatMemory ..|> ChatMemory
  LiveChatSession --> AssistantProfileChatMemory : owns per-session memory
  LiveChatSession --> ChatTranscriptEntry : owns runtime transcript entries
  AIChatPanel --> ConversationTurnWriter : writes turns
  AIChatPanel --> ConversationCursorNavigator : undo/redo
  AIChatPanel --> ConversationMessagesView : renders active timeline
  AIChatPanel --> LiveChatController : session actions
  LiveChatController --> LiveChatSession : reads current session
  LiveChatController --> TranscriptMemoryMapper : map only at boundaries
  LiveChatController --> ChatTranscriptStore : persistence
  TranscriptMemoryMapper --> ConversationTranscriptSeeder : seed memory
  TranscriptMemoryMapper --> ChatTranscriptEntry : maps to and from
  AIChatServiceFactory --> ChatMemory : builds service
  @enduml
  ```

  - `AssistantProfileChatMemory` is the single owner of:
    turn chain, current cursor, and eviction policy.
  - Eviction is non-destructive for conversation history: move a memory
    window start cursor instead of deleting historical entries.
  - `AssistantProfileChatMemory` is also the single owner of undo/redo
    state. No undo/redo copies are stored in panel/session/controller.
  - `ChatSessionMemoryController` is removed.
  - `LiveChatController` remains the live-chat orchestration point for
    session lifecycle and persistence calls.
  - `TranscriptMemoryMapper` is a stateless mapper and is called only
    at translation boundaries:
    `LiveChatController` reads current `LiveChatSession` and passes it
    as a parameter to mapper methods.
    - live session state to transcript persistence payload,
    - transcript entries to memory seeding payload.
  - Interface segregation is applied at call sites:
    - `AIChatPanel` depends on writer/cursor/view interfaces.
    - transcript restore paths depend on transcript-seeding interface
      and mapper usage from live orchestration.
    - LangChain service wiring depends on `ChatMemory`.
  - Undo/redo is cursor movement over one memory-owned turn chain.
    New send from rewound position clears forward branch.
  - LLM tool-call messages are memory-backed conversation entries and
    must survive normal response-time UI refresh.
  - MCP tool-call messages are panel-transient entries:
    - they are shown while staying in the active chat panel,
    - they are cleared on undo/redo and on chat switch,
    - they are not persisted into conversation memory or transcript.
  - Interrupted user input remains UI draft state in the panel, while
    persistent conversation data remains memory-owned.
  - Transcript persistence is derived from memory-owned active timeline.
  - No additional store/projector layers are introduced.
  - No snapshot, delta, or mirror copies of conversation state are
    used for undo/redo outside memory owner.
  - This task is intentionally sequential and currently not split into
    independent subtasks. Implementation should proceed in one flow:
    introduce memory interfaces, move undo/redo into memory owner,
    rewire runtime dependencies, add mapper boundary calls, then remove
    legacy paths.
- **Test specification:**
  - Automated tests:
    - `AssistantProfileChatMemory` tests for writer/cursor/view/seeder
      contracts, undo/redo cursor invariants, and eviction invariants.
    - `AIChatPanel` tests for send/cancel/undo/redo behavior via memory
      interfaces with no external undo buffers.
    - `LiveChatController` and session-switch tests proving
      per-session memory/transcript isolation.
    - `TranscriptMemoryMapper` tests for deterministic
      `ChatTranscriptEntry <-> ChatMessage` mapping.
    - `AIChatServiceFactory` tests ensuring service receives the session
      `ChatMemory` directly from memory owner.
  - Manual tests:
    - Send multiple turns, perform undo/redo, then send a new turn and
      verify redo branch is cleared.
    - Cancel an in-flight request and verify user draft is restored in
      input without persisting an extra assistant turn.
    - Switch between live chats and verify each session restores its
      own memory-backed timeline and transcript continuity.

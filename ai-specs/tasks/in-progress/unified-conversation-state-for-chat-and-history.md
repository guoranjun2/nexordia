# Task: Unified conversation state for chat and history
- **Task Identifier:** 2026-02-06-conversation-state
- **Scope:** Introduce a single source of truth for AI chat conversation
  state and migrate chat UI, chat memory, transcript, and input draft to
  derived projections from that state. Provide a history model that
  supports efficient multi-step undo/redo.
- **Motivation:** Current state is distributed across multiple mutable
  components, making turn rewind and redo fragile and expensive to keep
  consistent. A unified immutable-like state model reduces coupling and
  makes undo/redo a first-class operation.
- **Developer Briefing:** This is an architectural refactoring task. It
  defines a migration path from current imperative synchronization toward
  a conversation state store with history navigation (`past/current/future`)
  while preserving current behavior during rollout.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    currently mutates UI history, memory, and transcript separately during
    send/cancel flows.
  - `AIChatPanel` cancellation uses temporary fields
    (`pendingHistorySnapshot`, `pendingMemorySnapshot`,
    `pendingTranscriptEntries`, `pendingUserMessage`) to restore a single
    in-flight turn.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMessageHistory.java`
    stores rendered message entries and supports full snapshot/restore.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryController.java`
    snapshots/restores LangChain4j message lists and seeds transcript
    context independently from UI history.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/LiveChatController.java`
    keeps transcript entries and live session switching state independently
    from `AIChatPanel` request state.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/LiveChatSession.java`
    already carries per-session mutable fields (message snapshots,
    transcript entries, metadata), which can host a unified conversation
    state reference.
  - Existing rewind design task
    `ai-specs/tasks/backlog/ai-chat-turn-undo-redo-and-interrupted-input.md`
    assumes snapshot orchestration between multiple stores; this task can
    replace that with a source-of-truth model.
  - Existing token-memory task
    `ai-specs/tasks/backlog/token-based-chat-memory-window.md` depends on
    reliable ownership of memory state and is easier once memory becomes
    a projection from unified conversation state.
- **Design:**

  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "ConversationStateStore" as Store
  participant "ConversationHistory" as History
  participant "ConversationProjector" as Projector

  User -> ChatPanel : send/undo/redo/cancel
  ChatPanel -> Store : dispatch(command)
  Store -> History : evolve(current, command)
  History --> Store : new current + past/future
  Store -> Projector : project(current)
  Projector --> ChatPanel : rendered messages + input draft
  Projector --> ChatPanel : chat memory view + transcript view
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class ConversationStateStore
    class ConversationHistory
    class ConversationState
    class ConversationTurn
    class ConversationCommand
    class ConversationProjector
    class AIChatPanel
    class ChatSessionMemoryController
    class LiveChatController
  }

  AIChatPanel --> ConversationStateStore : dispatches commands
  ConversationStateStore --> ConversationHistory : manages past/current/future
  ConversationHistory --> ConversationState : contains
  ConversationState --> ConversationTurn : contains
  ConversationStateStore --> ConversationProjector : derives views
  ConversationProjector --> ChatSessionMemoryController : memory projection
  ConversationProjector --> LiveChatController : transcript projection
  @enduml
  ```

  - Introduce `ConversationState` as canonical state with:
    - ordered turn list,
    - input draft,
    - in-flight request metadata,
    - session metadata required by transcript persistence.
  - Introduce `ConversationHistory` as zipper-like structure:
    `past/current/future`; undo moves `current` to `future`, redo moves
    head of `future` back to `past/current`.
  - Introduce command-based updates (`ConversationCommand`) so all state
    mutations go through one reducer/evolver.
  - Add `ConversationProjector` to derive:
    - chat UI render snapshots,
    - LangChain4j chat memory messages,
    - transcript entries for persistence.
  - Keep `AIChatPanel` as thin view/controller:
    no direct mutation of independent stores.
  - Migration phases:
    - Phase 1: dual-write (existing stores + new state) with
      equivalence assertions in tests.
    - Phase 2: switch reads to projections from unified state.
    - Phase 3: remove legacy mutable snapshot fields and redundant sync
      code from panel/controller classes.
  - Per-session behavior:
    each `LiveChatSession` holds its own `ConversationHistory` so chat
    switching preserves undo/redo stacks naturally.
  - Backward compatibility:
    initial state for existing sessions/transcripts is built by mapping
    current transcript/messages into `ConversationState`.
- **Test specification:**
  - Automated tests:
    - Reducer/evolver tests for send, assistant response append, cancel,
      undo, redo, and redo-clear-on-new-send.
    - `ConversationProjector` tests verifying deterministic projection to
      UI history, chat memory messages, and transcript entries.
    - Session isolation tests: undo/redo stacks and drafts are independent
      across live sessions.
    - Migration parity tests: projected outputs equal legacy outputs for
      representative chat scenarios.
  - Manual tests:
    - Multi-turn undo/redo while switching between live chats.
    - Cancel an in-flight response and verify draft restoration and state
      consistency after subsequent send.
    - Persist/reopen chat history and verify projected transcript matches
      visible conversation.

## Subtask: Introduce core conversation state and history reducer
- **Status:** in-progress
- **Scope:** Add foundational domain types (`ConversationState`,
  `ConversationTurn`, `ConversationHistory`, `ConversationCommand`) and
  deterministic reducer logic for send, append-assistant, cancel, undo,
  redo, and redo-clear-on-new-send transitions.
- **Motivation:** Establish one canonical mutation path before wiring UI
  and projections.
- **Developer Briefing:** This subtask should avoid behavior changes in
  current UI flow; it introduces the state kernel and tests only.
- **Research:** Existing panel/controller logic spreads state mutation
  over UI history, memory, and transcript stores.
- **Design:** Use command-driven state evolution with immutable-like
  snapshots and zipper-style history navigation.
- **Test specification:** Add reducer-level tests for all supported
  commands and invariants.

## Subtask: Add projection layer and dual-write parity validation
- **Status:** in-progress
- **Scope:** Introduce `ConversationProjector` that derives chat render
  entries, memory messages, and transcript entries from current state;
  wire dual-write/dual-read checks to compare projected results with
  existing runtime outputs.
- **Motivation:** Validate model correctness before switching production
  read paths.
- **Developer Briefing:** Keep current behavior authoritative while
  parity checks prove projection equivalence on representative flows.
- **Research:** Existing helper classes already expose snapshot/restore
  formats needed for parity comparison.
- **Design:** Add parity assertions in tests and runtime-safe comparison
  hooks for development diagnostics.
- **Test specification:** Add parity tests covering send, cancel,
  transcript restore, and live-session switching flows.

## Subtask: Switch runtime reads to unified state projections
- **Status:** backlog
- **Scope:** Rewire `AIChatPanel`, `ChatSessionMemoryController`, and
  `LiveChatController` integration points so runtime reads come from
  unified state projections rather than independent mutable stores.
- **Motivation:** Remove split-brain state ownership in active runtime
  behavior.
- **Developer Briefing:** Keep public behavior stable; focus on changing
  ownership boundaries and data flow.
- **Research:** Current request lifecycle uses temporary snapshot fields
  in `AIChatPanel` and independent transcript/memory updates.
- **Design:** Treat panel as command dispatcher plus projection consumer;
  delegate state ownership to the per-session state store.
- **Test specification:** Add integration tests for cancel recovery,
  session switch isolation, and undo/redo availability state.

## Subtask: Remove legacy synchronization paths and finalize cleanup
- **Status:** backlog
- **Scope:** Delete obsolete pending snapshot fields and redundant sync
  paths once unified-state runtime flow is stable, and simplify classes
  that became projection adapters.
- **Motivation:** Prevent drift between old and new paths and reduce
  maintenance complexity.
- **Developer Briefing:** This subtask is strictly cleanup and hardening
  after migration cutover.
- **Research:** Legacy sync code lives mainly in `AIChatPanel` request
  lifecycle and related helper update calls.
- **Design:** Keep one active execution path; remove compatibility
  branches unless explicitly required.
- **Test specification:** Re-run full targeted chat suite and verify no
  references to removed legacy fields remain.

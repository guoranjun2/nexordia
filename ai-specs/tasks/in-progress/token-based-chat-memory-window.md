# Task: Token-based chat memory window
- **Task Identifier:** 2026-02-06-token-memory
- **Scope:** Replace chat memory truncation by message count with a
  token-based sliding window as the only active memory policy for AI
  chat sessions, while preserving current assistant profile/system
  instruction semantics, turn grouping/undo-redo behavior, and
  tool-message consistency. Use cursor-based eviction (move window
  start) instead of destructive message deletion.
- **Motivation:** Message-count windows are coarse and can overflow real
  model context when messages are long. Token-based limits align memory
  with model constraints and reduce context overflow risk.
- **Developer Briefing:** The current memory implementation is custom
  because it injects and compacts profile/system instruction messages.
  The migration should keep these behaviors while replacing
  message-count eviction entirely with token-count eviction. LLM tool
  messages are memory-backed; MCP tool messages are UI-transient and are
  not part of memory eviction math.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AssistantProfileChatMemory.java`
    in the baseline implementation evicts by message count
    (`maxMessagesProvider`), not by token totals.
  - `AssistantProfileChatMemory` already contains custom rules for:
    `AssistantProfileSystemMessage` compaction, single
    `RemovedForSpaceSystemMessage`, `TranscriptHiddenSystemMessage`,
    `InstructionAckMessage`, and orphan tool result eviction when a tool
    request message is removed.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMemorySettings.java`
    in baseline exposes message-count settings; token-count settings are
    part of the in-progress branch changes.
  - `freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml`
    does not currently expose chat memory mode/limits in UI settings.
  - `freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties`
    baseline did not provide a committed token-memory default; token
    defaults are currently part of uncommitted in-progress changes.
  - `freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml`
    currently has no chat-memory mode/size controls; this task should
    keep that unchanged unless requested separately.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    and `LiveChatController` currently instantiate memory through
    `AssistantProfileChatMemory.withMaxTokens(...)` and
    `ChatMemorySettings`, so adding token mode should be localized to
    settings + memory construction.
  - Plugin memory continues using custom profile/system/tool behavior
    with message-based eviction in the committed baseline.
  - LangChain4j `1.10.0` provides `TokenCountEstimator` with a local
    `OpenAiTokenCountEstimator` and a network-based
    `GoogleAiGeminiTokenCountEstimator`; this task should use a local
    estimator for memory eviction checks.
  - Planned token accounting must include all stored message types
    without
    exclusions: user, assistant, tool results, profile/system
    instructions, and acknowledgement messages.
  - Planned token accounting must be incremental: maintain a running
    total and
    update it by delta on add/remove/replace operations, rather than
    recomputing token count across all messages on each mutation.
  - Current working tree contains uncommitted token-memory changes; this
    task design describes the target state, not yet-committed baseline.
  - `AIChatService` records `TokenUsage` only when
    `AiServiceResponseReceivedEvent` arrives, so provider token usage is
    available after a successful response.
  - Transcript-restored messages do not have provider token-usage
    deltas; their token cost must be derived from local estimation.
  - `ChatTokenUsageTracker` currently stores cumulative chat totals only
    and has no per-turn/per-block token attribution.
  - `AIChatPanel` currently appends an error message on request failure
    and always calls `finishRequest()`. Restoring pending user input and
    truncating memory is currently done only on cancellation via
    `restoreCancelledRequest()`.
  - Current in-progress structure introduces request orchestration in
    `ChatRequestFlow` and a turn-scoped memory bridge
    (`SingleTurnChatMemory` via `SingleTurnChatMemoryFactory`) so
    rollback/eviction paths can work with `AssistantProfileChatMemory`
    and generic `ChatMemory` uniformly.
  - `AssistantProfileChatMemory` remains the conversation single source
    of truth. Turn rollback/truncation must operate on that memory
    directly (message-count cursor/truncate), not snapshot copies.
  - Current regressions showed that UI-only tool-call rendering can be
    lost on panel rebuild; tool-call visibility rules must be explicit
    in design:
    - LLM tool calls are memory-backed and must rebuild from memory.
    - MCP tool calls are transient panel entries and not transcript data.
  - Existing tests in
    `freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/AssistantProfileChatMemoryTest.java`
    verify current custom semantics for message-window behavior.
  - There are no existing unit tests for `ChatMemorySettings`; those
    tests need to be introduced in this task.
- **Design:**

  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "AIChatService" as ChatService
  participant "ChatTokenUsageTracker" as UsageTracker
  participant "AssistantProfileChatMemory" as Memory
  participant "TokenCountEstimator" as Estimator
  participant "MCP transient panel log" as McpLog

  User -> ChatPanel : send message
  ChatPanel -> Memory : add(user/assistant/tool/system)
  ChatPanel -> McpLog : append MCP tool call (transient)
  ChatPanel -> ChatService : chat(request)
  ChatService -> UsageTracker : record response token usage
  UsageTracker --> ChatPanel : cumulative input/output totals
  ChatPanel -> Memory : commit turn token delta
  Memory -> Memory : post-response eviction by moving window cursor
  alt provider error: context too large
    ChatPanel -> Memory : evict and retry request
  end
  Memory -> Estimator : estimateTokenCountInMessage(...)
  Estimator --> Memory : token count by message delta
  Memory --> ChatPanel : bounded message history after response
  McpLog --> ChatPanel : shown until undo/redo or chat switch
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class ChatMemorySettings
    class AssistantProfileChatMemory
    interface SingleTurnChatMemory
    class SingleTurnChatMemoryFactory
    class ChatRequestFlow
  }

  AssistantProfileChatMemory --> ChatMemorySettings : reads token limit
  AssistantProfileChatMemory --> TokenCountEstimator : estimates tokens per message
  SingleTurnChatMemoryFactory --> SingleTurnChatMemory : creates turn memory bridge
  ChatRequestFlow --> SingleTurnChatMemory : rollback/eviction operations
  @enduml
  ```

  - Make token-window eviction the single active memory policy.
  - Extend `ChatMemorySettings` with token-limit configuration:
    `ai_chat_memory_maximum_token_count` with a safe default.
  - Remove message-count-based eviction logic from
    `AssistantProfileChatMemory`.
  - Keep profile/system/tool eviction semantics unchanged; only the
    capacity metric changes.
  - Eviction must move a conversation window-start cursor instead of
    deleting historical messages from the underlying list.
  - Token counting includes all messages currently held in memory.
  - Use LangChain4j local token estimation in memory:
    `OpenAiTokenCountEstimator`.
  - Document that the token budget is approximate because estimator
    tokenization may differ from the selected provider.
  - Include all message types in token accounting, including tool and
    profile/system instruction messages.
  - LLM tool-call messages are included in token accounting and must be
    rebuilt from memory after panel refresh.
  - MCP tool-call messages are not memory-backed and not included in
    token accounting; they are transient panel entries shown until
    undo/redo or chat switch.
  - Keep turn-level rollback logic cursor-based (sizes/indexes) over the
    same memory instance; do not introduce snapshot-based message copies.
  - Keep running token totals via delta updates on message mutations
    (add/remove/replace), without full-history recount in steady state.
  - Persist per-turn or per-undo-block token cost after successful
    responses using provider token-usage deltas.
  - For transcript-restored or otherwise delta-less blocks, use local
    estimator token cost as fallback.
  - Eviction uses unified block cost: provider delta when available,
    otherwise local estimator fallback.
  - Run eviction after successful responses (post-response eviction).
  - On provider `context too large` error, run eviction and retry the
    pending request with retry limits to avoid infinite loops.
  - Keep provider hard context limits and plugin memory limits separate:
    provider limits gate request success, plugin limits govern retained
    history after each turn.
  - Do not keep message-window compatibility path.
  - Keep transcript seeding and conversation-cursor behavior unchanged
    under token mode.
  - Expose `ai_chat_memory_maximum_token_count` in preferences and add a
    tooltip explaining approximate token counting semantics.
- **Test specification:**
  - Automated tests:
    - Add `AssistantProfileChatMemory` tests for token-window eviction:
      oldest counted messages are evicted until total estimated tokens
      fit limit.
    - Verify profile/system instruction semantics stay unchanged in
      token mode (compaction, hidden/system instruction presence, single
      removed-for-space marker).
    - Verify tool-request eviction in token mode also removes following
      tool-result messages.
    - Add `ChatMemorySettings` parsing tests for token count property
      defaults and invalid values.
    - Add runtime integration tests to verify panel/session flows use
      token mode without changing undo/redo and transcript semantics.
    - Add tests for post-response eviction and context-too-large retry
      flow with bounded retries.
    - Add tests for per-block token delta attribution and fallback
      behavior when provider usage is unavailable.
    - Add tests that transcript-restored blocks are evicted correctly
      using local-estimator costs when provider deltas are absent.
  - Manual tests:
    - Run a long multi-turn chat with large messages and verify old
      turns are removed as token budget is exceeded while recent turns
      remain coherent.
    - Verify runtime stability for OpenRouter, Gemini, and Ollama
      selections with token-window memory.

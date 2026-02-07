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
  - LangChain4j provides local token estimators that can be used to
    approximate token counts for chat messages.
  - `AIChatService` records provider token usage only when
    `AiServiceResponseReceivedEvent` arrives; provider totals are
    incomplete for per-message eviction decisions.
  - Current working tree contains uncommitted token-memory changes; this
    task design describes the target state, not yet-committed baseline.
  - `AIChatService` records `TokenUsage` only when
    `AiServiceResponseReceivedEvent` arrives, so provider token usage is
    available after a successful response.
  - Transcript-restored messages do not have provider token-usage
    deltas; local estimation is needed for consistent eviction when
    transcript messages are part of the active window.
  - `ChatTokenUsageTracker` currently stores cumulative chat totals only
    and has no per-turn/per-block token attribution.
  - Provider token usage includes system instructions, tool metadata,
    and other model-specific overhead that is not directly attributable
    to individual removable chat messages.
  - Accurate per-message token usage is not available from providers,
    so local estimation is the only consistent way to measure removable
    blocks for eviction or counter displays.
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
  participant "ChatRequestFlow" as RequestFlow
  participant "AssistantProfileChatMemory" as Memory
  participant "ChatTokenUsageTracker" as UsageTracker
  participant "AIChatService" as ChatService
  participant "MCP transient panel log" as McpLog

  User -> ChatPanel : send message
  ChatPanel -> Memory : add(user/assistant/tool/system)
  ChatPanel -> McpLog : append MCP tool call (transient)
  ChatPanel -> RequestFlow : start request
  RequestFlow -> ChatService : chat(request)
  ChatService --> RequestFlow : response token usage (optional)
  RequestFlow -> Memory : post-response eviction (cursor move)
  RequestFlow -> UsageTracker : update counters
  Memory --> ChatPanel : active window messages
  UsageTracker --> ChatPanel : counters (or hidden)
  McpLog --> ChatPanel : shown until undo/redo or chat switch
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class AssistantProfileChatMemory
    class ChatMemorySettings
    class ChatTokenUsageTracker
    class ChatTokenEstimator
    class ChatTokenCounterMode
    class ChatRequestFlow
    class AIChatPanel
  }

  AssistantProfileChatMemory --> ChatMemorySettings : reads token limit
  AIChatPanel --> AssistantProfileChatMemory : active window messages
  ChatRequestFlow --> ChatTokenUsageTracker : updates counters
  ChatTokenUsageTracker --> ChatTokenCounterMode : mode selection
  ChatTokenUsageTracker --> AssistantProfileChatMemory : request estimates
  AssistantProfileChatMemory --> ChatTokenEstimator : estimate tokens
  @enduml
  ```

  - Make token-window eviction the single active memory policy.
  - Extend `ChatMemorySettings` with token-limit configuration:
    `ai_chat_memory_maximum_token_count` with a safe default and no
    hardcoded minimum clamp (accept any positive value).
  - Remove message-count-based eviction logic from
    `AssistantProfileChatMemory`.
  - Keep profile/system/tool eviction semantics unchanged; only the
    capacity metric changes.
  - Eviction must move a conversation window-start cursor instead of
    deleting historical messages from the underlying list.
  - Token counting for eviction is based on local estimation of
    removable blocks (user, assistant, tool call, tool result) only.
  - Always-present system and tool instructions are treated as constant
    overhead and are not included in the removable-token tally.
  - Profile control instructions may be compacted or removed by other
    logic; they are excluded from the removable-token tally.
  - LLM tool-call messages are included in token accounting and must be
    rebuilt from memory after panel refresh.
  - MCP tool-call messages are not memory-backed and not included in
    token accounting; they are transient panel entries shown until
    undo/redo or chat switch.
  - Keep turn-level rollback logic cursor-based (sizes/indexes) over the
    same memory instance; do not introduce snapshot-based message copies.
  - Token usage display uses estimates provided by
    `AssistantProfileChatMemory`, keeping estimator details internal.
  - Provider usage callbacks record usage only; token counters refresh
    after assistant responses are appended to memory so estimates are
    consistent with the rendered conversation.
  - `ChatTokenEstimator` is a private helper owned by
    `AssistantProfileChatMemory` and is not accessed by the panel.
  - Token counter display is configurable via preferences with four
    modes (default: hidden):
    - Hidden: no counters are shown.
    - Context window estimates: input/output counters use the estimator
      on removable blocks in the active window.
    - Total chat estimates: input/output counters use the estimator on
      removable blocks across the full chat history.
    - Model response: input/output counters show the last response's
      provider usage only (no accumulation, no estimator).
  - Counter labels use the selected preference text (for example
    "Context window estimates") and avoid the literal word "tokens" to
    keep localization flexible; the label is omitted if no mode label
    is available.
  - Implement the counter mode preference as a `radiobuttons` group
    with four `choice` values and translation keys.
  - Provide explicit English translations for the counter mode choices
    to avoid placeholder labels in preferences.
  - Eviction runs after successful responses (post-response eviction)
    and moves the window cursor by completed turns until estimated
    removable tokens are within the configured limit.
  - Always keep at least one user message in the active window. If the
    earliest user message alone exceeds the token budget, keep it and
    stop evicting (do not advance past that first user message).
  - Refactor `AssistantProfileChatMemory` to make eviction decisions
    fully testable and remove opaque flags:
    - Eviction is triggered only from the post-response usage callback.
    - Keep eviction logic in a single method that returns whether the
      window advanced so callers can rebuild UI immediately.
    - Avoid introducing new classes unless needed for test isolation;
      prefer extracting private helpers that return deterministic
      results.
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
      oldest turns are evicted after a response pushes provider totals
      beyond the configured limit.
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
    - Add tests for local-estimator token accounting for removable
      blocks and eviction based on estimated totals.
    - Add tests for counter display modes:
    - Hidden shows no counters.
    - Context window estimates reflect removable blocks inside the
      active window.
    - Total chat estimates reflect removable blocks across the full
      conversation list.
    - Model response shows last provider usage only and does not
      accumulate across turns.
    - Counter labels use the selected preference text and omit the
      literal word "tokens".
    - Add tests that eviction never removes the last remaining user
      message in the active window, even when it exceeds the token
      limit.
    - Expand `AssistantProfileChatMemory` coverage to include:
      - Eviction does not run while a request is in flight and runs only
        after the post-response usage callback.
      - Eviction advances the active window by exactly one completed
        turn per usage callback and never moves backwards.
      - Context-boundary marker appears immediately after eviction when
        rebuilding the render entries.
      - Undo/redo boundaries interact correctly with a non-zero window
        start index.
      - Tool call summaries render once and tool results are excluded
        when summaries are present.
      - Control instruction and transcript-hidden messages remain
        system-rendered and excluded from transcript entries.
  - Manual tests:
    - Run a long multi-turn chat with large messages and verify old
      turns are removed as token budget is exceeded while recent turns
      remain coherent.
    - Verify runtime stability for OpenRouter, Gemini, and Ollama
      selections with token-window memory.

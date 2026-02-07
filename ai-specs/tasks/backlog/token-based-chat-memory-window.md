# Task: Token-based chat memory window
- **Task Identifier:** 2026-02-06-token-memory
- **Scope:** Replace chat memory truncation by message count with a
  token-based sliding window for AI chat sessions, preserving current
  assistant profile/system instruction semantics and tool-message
  consistency.
- **Motivation:** Message-count windows are coarse and can overflow real
  model context when messages are long. Token-based limits align memory
  with model constraints and reduce context overflow risk.
- **Developer Briefing:** The current memory implementation is custom
  because it injects and compacts profile/system instruction messages.
  The migration should keep these behaviors while changing eviction from
  message-count to token-count.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AssistantProfileChatMemory.java`
    currently evicts by counted message quantity via `maxMessagesProvider`.
  - `AssistantProfileChatMemory` already contains custom rules for:
    `AssistantProfileSystemMessage` compaction, single
    `RemovedForSpaceSystemMessage`, `TranscriptHiddenSystemMessage`,
    `InstructionAckMessage`, and orphan tool result eviction when a tool
    request message is removed.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMemorySettings.java`
    currently exposes only `maximumMessageCount`; no token window
    setting exists.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMemoryMode.java`
    supports `DISABLED` and `MESSAGE_WINDOW`; no token-window mode
    exists.
  - `freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml`
    does not currently expose chat memory mode/limits in UI settings.
  - `freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties`
    has no defaults for `ai_chat_memory_mode` or size limits.
  - LangChain4j `1.10.0` provides `TokenWindowChatMemory` and
    `dev.langchain4j.model.TokenCountEstimator`.
  - `TokenWindowChatMemory` evicts oldest messages until estimated token
    total fits `maxTokens`, preserves one `SystemMessage`, and evicts
    orphan `ToolExecutionResultMessage` with parent tool-request
    `AiMessage`.
  - Current plugin memory behavior cannot be swapped 1:1 with stock
    `TokenWindowChatMemory` because profile/system instruction handling
    is implemented in `AssistantProfileChatMemory`.
  - Existing tests in
    `freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/AssistantProfileChatMemoryTest.java`
    verify current custom semantics for message-window behavior.
- **Design:**

  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "ChatSessionMemoryController" as MemoryController
  participant "AssistantProfileChatMemory" as Memory
  participant "TokenCountEstimator" as Estimator

  User -> ChatPanel : send message
  ChatPanel -> MemoryController : getChatMemory()
  MemoryController -> Memory : add(user/assistant/tool/system)
  Memory -> Estimator : estimateTokenCountInMessages(...)
  Estimator --> Memory : token count
  Memory -> Memory : evict oldest until within maxTokens
  Memory --> MemoryController : bounded message history
  @enduml
  ```

  ```plantuml
  @startuml
  set separator none
  package "org.freeplane.plugin.ai.chat" {
    class ChatMemoryMode
    class ChatMemorySettings
    class ChatSessionMemoryController
    class AssistantProfileChatMemory
    interface ChatTokenEstimatorProvider
    class AdaptiveTokenCountEstimator
  }

  ChatMemorySettings --> ChatMemoryMode : reads
  ChatSessionMemoryController --> ChatMemorySettings : uses
  ChatSessionMemoryController --> AssistantProfileChatMemory : builds
  AssistantProfileChatMemory --> ChatTokenEstimatorProvider : queries
  ChatTokenEstimatorProvider --> AdaptiveTokenCountEstimator : delegates
  @enduml
  ```

  - Add a new chat memory mode `TOKEN_WINDOW` in `ChatMemoryMode`.
  - Extend `ChatMemorySettings` with token-limit configuration:
    `ai_chat_memory_maximum_token_count` with a safe default.
  - Extend `AssistantProfileChatMemory` to support token-based capacity
    in addition to existing message-window capacity, controlled by
    `ChatMemoryMode`.
  - Keep profile/system/tool eviction semantics unchanged; only the
    capacity metric changes when `TOKEN_WINDOW` is selected.
  - Introduce `ChatTokenEstimatorProvider` abstraction so token counting
    can adapt per selected model/provider and fallback safely when exact
    tokenizer is unavailable.
  - Implement `AdaptiveTokenCountEstimator` with conservative fallback
    estimation for providers without tokenizer support and deterministic
    behavior for tests.
  - Keep `MESSAGE_WINDOW` as compatibility mode and default migration
    path explicit in settings.
  - Ensure snapshot/restore (`snapshotMessages`, `restoreMessages`) in
    `ChatSessionMemoryController` continues to work identically with
    token-window mode.
  - Keep transcript seed methods (`seedTranscript`,
    `seedTranscriptWithHiddenExchange`) behavior unchanged.
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
    - Add `ChatMemoryMode` parsing tests including `token_window`.
    - Add `ChatSessionMemoryController` tests to verify memory creation
      mode selection and snapshot/restore behavior in token mode.
  - Manual tests:
    - Run a long multi-turn chat with large messages and verify old
      turns are removed earlier than in message-window mode while recent
      turns remain coherent.
    - Verify switching between `MESSAGE_WINDOW` and `TOKEN_WINDOW`
      preserves runtime stability across OpenRouter, Gemini, and Ollama
      model selections.

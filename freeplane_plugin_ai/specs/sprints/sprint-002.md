# Sprint 002

## Task: Chat session controls, token usage status, and tool call log
- **Status:** Designing
- **Scope:** Add chat memory controls for continuing or restarting sessions, show running token usage totals in the chat panel, and log tool calls without per-tool token counts.
- **Research summary:**
```plantuml
@startuml
class AIChatPanel
class AIChatService
class AiServices
class MessageWindowChatMemory
class TokenWindowChatMemory
class AiServiceStartedEvent
class AiServiceResponseReceivedEvent
class ToolExecutedEvent
class ChatResponse
class TokenUsage
class ToolExecutionRequest
class OpenAiTokenCountEstimator

AIChatPanel --> AIChatService
AIChatService --> AiServices
AiServiceStartedEvent --> AIChatService
AiServiceResponseReceivedEvent --> ChatResponse
ChatResponse --> TokenUsage
ToolExecutedEvent --> ToolExecutionRequest
AiServices --> MessageWindowChatMemory
AiServices --> TokenWindowChatMemory
TokenWindowChatMemory --> OpenAiTokenCountEstimator

note right of AIChatService
AiServices registers only an error listener.
No chat memory is configured, so each call
is a single-turn request with system message,
user message, and any tool calls inside the same
invocation.
end note

note bottom
AiServiceResponseReceivedEvent can fire multiple
times per invocation. TokenUsage is available on
ChatResponse. ToolExecutedEvent provides tool name,
arguments, and result text.
MessageWindowChatMemory and TokenWindowChatMemory
retain tool execution result messages with the
conversation history.
OpenAiTokenCountEstimator can provide token estimates
for OpenAI-compatible models when using TokenWindowChatMemory.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class AIChatPanel
class AIChatService
class ChatSessionMemoryController
class ChatMemorySettings
class ChatMemoryMode
class ChatTokenUsageTracker
class ChatUsageTotals
class AiServiceResponseReceivedEvent
class AiServiceStartedEvent
class ToolExecutedEvent
class LogUtils

AIChatPanel --> ChatTokenUsageTracker
AIChatPanel --> ChatSessionMemoryController
AIChatService --> ChatTokenUsageTracker
AIChatService --> ChatSessionMemoryController
ChatSessionMemoryController --> ChatMemorySettings
ChatSessionMemoryController --> ChatMemoryMode
ChatTokenUsageTracker --> ChatUsageTotals
ChatTokenUsageTracker ..> AiServiceResponseReceivedEvent
ChatTokenUsageTracker ..> AiServiceStartedEvent
ChatTokenUsageTracker ..> ToolExecutedEvent
ChatTokenUsageTracker ..> LogUtils

note right of AIChatPanel
Add a status line for running totals
(input and output tokens).
Add controls to continue or start a new chat.
end note

note right of ChatTokenUsageTracker
Maintain running totals for input and output tokens.
Update the status line after each response event.
Log tool calls via LogUtils.info without token counts.
end note

note bottom
Tool call log entries include tool name and arguments.
ChatSessionMemoryController creates and clears
ChatMemory based on settings and user actions.
end note
@enduml
```
- **Test specification:**
  - Verify chat memory is reused when continuing a session.
  - Verify chat memory is cleared when starting a new session.
  - Verify usage totals update after response events.
  - Verify a tool call event writes to LogUtils.
  - Verify the status line reflects cumulative totals.

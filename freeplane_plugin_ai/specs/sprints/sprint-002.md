# Sprint 002

## Task: Chat session controls, token usage status, and tool call log
- **Status:** Finished
- **Scope:** Add chat memory controls for continuing or restarting sessions, show running token usage totals in the chat panel, and log tool calls without per-tool token counts.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatService.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatServiceFactory.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMemoryMode.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMemorySettings.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryController.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatTokenUsageTracker.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatUsageTotals.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryControllerTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/ChatTokenUsageTrackerTest.java
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

## Task: Review llm feedback for read tools
- **Status:** Implementation Review
- **Scope:** Apply feedback to the read tool by renaming it to readNodeWithContext, flattening parameters, adding section selectors, and omitting null fields in responses.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ContextSection.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContent.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItem.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItemReader.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodeWithContextResponse.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodeWithContextTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/TextualContent.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AttributesContent.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/TagsContent.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/ReadNodeWithContextToolTest.java
- **Research summary:**
```plantuml
@startuml
rectangle "Feedback" as Feedback
rectangle "Decisions" as Decisions
Feedback --> Decisions : rename read tool\nflatten parameters\nadd context sections\nbreadcrumb path default\nomit null fields
@enduml
```
- **Design:**
```plantuml
@startuml
class AIToolSet {
  +readNodeWithContext(mapIdentifier, nodeIdentifier, contextSections)
}
class ReadNodeWithContextTool
class ReadNodeWithContextResponse
enum ContextSection {
  BREADCRUMB_PATH
  PARENT_SUMMARY
  FOCUS_CONTENT
  CHILD_SUMMARIES
}
class AvailableMaps
class NodeContentItemReader
class NodeContentItem
class NodeContent

AIToolSet --> ReadNodeWithContextTool
ReadNodeWithContextTool --> AvailableMaps
ReadNodeWithContextTool --> NodeContentItemReader
ReadNodeWithContextTool --> ReadNodeWithContextResponse
ReadNodeWithContextTool --> ContextSection
ReadNodeWithContextResponse --> NodeContentItem
NodeContentItem --> NodeContent

note right of ReadNodeWithContextTool
Default sections: breadcrumb_path, focus_content, child_summaries.
Parent summary is included only when requested.
end note

note right of NodeContentItem
JsonInclude NON_NULL omits null fields.
Node identifiers are always included.
end note
@enduml
```
- **Test specification:**
  - Verify default sections include focus content, child summaries, and breadcrumb path.
  - Verify parent summary is included when requested.
  - Verify focus content is omitted when not requested.
  - Verify invalid map identifiers fail fast.

## Task: Add icon content to node responses
- **Status:** Designing
- **Scope:** Expose node icons in read responses with icon names and optional emoji decoding for emoji icons.
- **Research summary:**
```plantuml
@startuml
class IconController
interface NamedIcon
interface IconDescription
class UIIcon
class MindIcon
class EmojiIcon
class IconStoreFactory

IconController --> NamedIcon : getIcons(node, style)
UIIcon ..|> NamedIcon
UIIcon ..|> IconDescription
MindIcon --|> UIIcon
EmojiIcon --|> MindIcon
IconStoreFactory --> MindIcon : createMindIcon(name)
IconStoreFactory --> EmojiIcon : createEmojiIcons()

note right of UIIcon
getTranslatedDescription uses TextUtils
and falls back to capitalized name.
end note

note right of EmojiIcon
EmojiIcon stores the emoji character and
overrides getTranslatedDescription to return
the description key. File names are hex
code points used by emoji assets.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class NodeContent
class IconsContent
class IconEntry
class IconsContentReader
class IconController
interface NamedIcon
interface IconDescription
class EmojiIcon
class NodeContentReader

NodeContentReader --> IconsContentReader
IconsContentReader --> IconController
IconsContentReader --> NamedIcon
IconsContent --> IconEntry
IconEntry ..> EmojiIcon : optional emoji decoding
IconEntry ..> IconDescription : optional description

note right of NodeContent
Add iconsContent for FULL preset only.
BRIEF preset stays text only.
end note

note right of IconsContentReader
Use IconController.getIcons(node, StyleOption.FOR_UNSELECTED_NODE)
to include visible icons, not only node-local icons.
end note
@enduml
```
- **Test specification:**
  - Verify icon entries include name and file for each icon.
  - Verify emoji icons include an emoji value when decoding is enabled.
  - Verify no icons content is returned for BRIEF preset.

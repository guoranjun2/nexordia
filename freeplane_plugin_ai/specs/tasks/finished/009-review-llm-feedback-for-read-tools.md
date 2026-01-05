# Task: Review llm feedback for read tools
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

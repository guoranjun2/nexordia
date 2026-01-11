# Task: Review llm feedback for read tools
- **Scope:** Apply feedback to the read tool by renaming it to readNodesWithDescendants, flattening parameters, adding section selectors, returning concatenated unformatted text only, and omitting null fields in responses. Expose a fetchNodesForEditing tool that returns structured editable content for precise edits.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ContextSection.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentResponse.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItem.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItemReader.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodesWithDescendantsResponse.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodesWithDescendantsTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/TextualContent.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AttributesContent.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/TagsContent.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/ReadNodesWithDescendantsToolTest.java
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
  +readNodesWithDescendants(mapIdentifier, nodeIdentifier, contextSections)
  +fetchNodesForEditing(mapIdentifier, nodeIdentifier)
}
class ReadNodesWithDescendantsTool
class ReadNodesWithDescendantsResponse
class FetchNodesForEditingResponse
enum ContextSection {
  BREADCRUMB_PATH
  PARENT_SUMMARY
  FOCUS_CONTENT
  CHILD_SUMMARIES
}
class AvailableMaps
class NodeContentItemReader
class ReadNodesWithDescendantsItem
class NodeContentItem
class NodeContentResponse

AIToolSet --> ReadNodesWithDescendantsTool
ReadNodesWithDescendantsTool --> AvailableMaps
ReadNodesWithDescendantsTool --> NodeContentItemReader
ReadNodesWithDescendantsTool --> ReadNodesWithDescendantsResponse
ReadNodesWithDescendantsTool --> FetchNodesForEditingResponse
ReadNodesWithDescendantsTool --> ContextSection
ReadNodesWithDescendantsResponse --> ReadNodesWithDescendantsItem
FetchNodesForEditingResponse --> NodeContentItem
NodeContentItem --> NodeContentResponse

note right of ReadNodesWithDescendantsTool
Default sections: breadcrumb_path, focus_content, child_summaries.
Parent summary is included only when requested.
fetchNodesForEditing reuses the same helper and returns editable content only.
end note

note right of ReadNodesWithDescendantsItem
readNodesWithDescendants returns unformattedText only,
with no structured fields from NodeContentResponse.
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
  - Verify readNodesWithDescendants returns unformattedText only, with labeled sections for details, note, tags, attributes, and icons when present.

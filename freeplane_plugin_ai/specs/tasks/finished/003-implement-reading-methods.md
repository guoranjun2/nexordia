# Task: Implement reading methods
- **Scope:** Implement read_node_content with a request that requires only map identifier and node identifier, always include node identifiers in the response, and return focus, parent, and child nodes with preset content; update AIToolSet to use the new method and remove the old read context request and response types.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AttributesContentReader.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContent.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItem.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItemReader.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentReader.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodeContentTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodeContentRequest.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ReadNodeContentResponse.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/TagsContentReader.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/TextualContentReader.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/NodeContentReaderTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/ReadNodeContentToolTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/TextualContentReaderTest.java
- **Research summary:**
```plantuml
@startuml
class NodeProxy
class AttributesProxy
class TagsProxy
class MapController
class MapExplorerController
class MapExplorer
class TextController
class DetailModel
class NoteModel
class AttributeController
class MIconController

NodeProxy --> TextController
NodeProxy --> DetailModel
NodeProxy --> NoteModel
AttributesProxy --> AttributeController
TagsProxy --> MIconController
MapController --> MapExplorerController
MapExplorerController --> MapExplorer

note right of NodeProxy
Uses TextController for text access and NodeModel.createID for identifiers.
Reads details via DetailModel.getDetailText and note via NoteModel.getNoteText.
end note

note right of MapExplorerController
getNodeAt supports ID and path-based lookups.
end note

note bottom
Method name: read_node_content.
Request accepts only map identifier and node identifier.
Response includes focus, parent, children only.
Selected node identifiers stay in the system message.
BRIEF uses TextController.getShortPlainText; node.getText is forbidden.
BRIEF uses NodeContent.briefText to avoid confusion with full content.
FULL uses transformed text and transformed objects as shown to users.
Use TextController.getTransformedTextForClipboard for full node, details, and note content to avoid latex icon rendering.
Attributes use transformed object values and then convert to string values.
briefText stays empty for full content.
Explicit content type metadata may be needed later.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class TextualContentReader
class AttributesContentReader
class TagsContentReader
class NodeContentReader
class NodeContentItemReader
class NodeContentItem
class ReadNodeContentRequest
class ReadNodeContentResponse
class AIToolSet
class ReadNodeContentTool
class AvailableMaps
class NodeModel
class TextController
class AttributeController
class IconController
class NodeContent {
briefText
textualContent
attributesContent
tagsContent
}

TextualContentReader --> TextController
AttributesContentReader --> AttributeController
TagsContentReader --> IconController
NodeContentReader o--> TextualContentReader
NodeContentReader o--> AttributesContentReader
NodeContentReader o--> TagsContentReader
NodeContentReader --> NodeContent : briefText
NodeContentItemReader o--> NodeContentReader
NodeContentItemReader --> NodeModel : createID
NodeContentItemReader --> NodeContentItem
ReadNodeContentTool --> NodeContentItemReader
ReadNodeContentTool --> AvailableMaps
ReadNodeContentTool --> ReadNodeContentRequest
ReadNodeContentTool --> ReadNodeContentResponse
AIToolSet --> ReadNodeContentTool

note right of AIToolSet
ReadNodeContentService is removed.
AIToolSet resolves controllers from the current mode controller, builds readers,
and delegates to ReadNodeContentTool.
end note

note bottom
BRIEF uses TextController.getShortPlainText for parent and children.
FULL uses TextController.getTransformedTextForClipboard for focus text, details, and note content.
FULL uses transformed attribute values from TextController.getTransformedObjectNoFormattingNoThrow.
ReadNodeContentTool always requests node identifiers.
NodeContentItemReader calls createID only when identifiers are requested.
NodeContent.briefText is populated only for BRIEF responses.
TextualContent is populated only for FULL responses.
end note
@enduml
```
```plantuml
@startuml
actor Caller
participant AIToolSet
participant ReadNodeContentTool
participant AvailableMaps
participant NodeContentItemReader
participant NodeContentReader
participant MapModel

Caller -> AIToolSet : read_node_content(map id, node id)
AIToolSet -> ReadNodeContentTool : readNodeContent(request)
ReadNodeContentTool -> AvailableMaps : findMapModel(map id)
ReadNodeContentTool -> MapModel : getNodeForID(node id)
ReadNodeContentTool -> NodeContentItemReader : fromNodeModel(focus, FULL, includesNodeIdentifiers)
NodeContentItemReader -> NodeContentReader : buildNodeContent(focus, preset)
ReadNodeContentTool -> NodeContentItemReader : fromNodeModel(parent, BRIEF, includesNodeIdentifiers)
ReadNodeContentTool -> NodeContentItemReader : fromNodeModel(children, BRIEF, includesNodeIdentifiers)
ReadNodeContentTool --> AIToolSet : ReadNodeContentResponse
AIToolSet --> Caller : ReadNodeContentResponse
@enduml
```
- **Test specification:**
  - Verify BRIEF response includes focus, parent, and children with briefText and node identifiers.
  - Verify missing parent returns null and empty children list.
  - Verify invalid map identifier throws a validation error.
- Verify TextController.getShortPlainText is used for BRIEF content.
- Verify TextualContent is null for BRIEF responses.
- Verify TextController.getShortPlainText is used for BRIEF content.
- Add tests for FULL focus content, including text, details, note, attributes, and tags.
- Add a test that full text uses TextController.getTransformedTextForClipboard.

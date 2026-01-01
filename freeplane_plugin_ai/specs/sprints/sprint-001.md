# Sprint 001

## Task: Read context request hierarchy and presets
- **Status:** Finished
- **Scope:** Define a new internal NodeContentRequest hierarchy with content presets for focus, parent, and child nodes.
- **Research summary:**
```plantuml
@startuml
note "NodeContent groups TextualContent, AttributesContent, TagsContent.\nTextualContent includes text, details, note.\nAttributesContent stores AttributeEntry name and value pairs.\nTagsContent stores tag strings.\nPrevious NodeContextRequest included map identifier, node identifier, depth, include flags, output format.\nPrevious NodeContextResponse included map identifier, output format, payload.\nAIToolSet exposed readNodeContext with those types.\nLangChain4j JacksonJsonCodec serializes nulls; missing fields become null; unknown properties rejected unless custom factory." as Research
@enduml
```
- **Design:**
```plantuml
@startuml
note "Define NodeContentRequest, TextualContentRequest, AttributesContentRequest, TagsContentRequest.\nDefine content presets full and brief.\nProvide preset resolver for focus, parent, child." as Design
@enduml
```
- **Test specification:**
  - Not applicable for structure-only change.

## Task: Remove Gson dependency from ai plugin
- **Status:** Implementation Review
- **Scope:** Remove the Gson dependency from the ai plugin build file and use Jackson provided by LangChain4j instead.
- **Modified production files:**
  - freeplane_plugin_ai/build.gradle
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIModelCatalog.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/AIModelCatalogTest.java
- **Research summary:**
```plantuml
@startuml
class AIModelCatalog
class Gson

AIModelCatalog --> Gson

note right of AIModelCatalog
Parses OpenRouter and Ollama model lists.
Uses Gson with SerializedName annotations.
No other Gson usage in the ai plugin.
end note

note bottom
freeplane_plugin_ai/build.gradle declares
lib 'com.google.code.gson:gson:2.13.1'.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class AIModelCatalog
class ObjectMapper
class OpenrouterModelsResponse
class OllamaModelsResponse

AIModelCatalog --> ObjectMapper
ObjectMapper --> OpenrouterModelsResponse
ObjectMapper --> OllamaModelsResponse

note right of AIModelCatalog
Replace Gson with a reusable ObjectMapper.
Configure the mapper to ignore unknown properties.
Use JsonProperty annotations on response fields.
end note

note bottom
Remove Gson dependency from freeplane_plugin_ai/build.gradle.
Keep parsing limited to model list responses.
end note
@enduml
```
- **Test specification:**
  - Add a unit test that parses a sample OpenRouter response and returns the expected model identifiers.
  - Add a unit test that parses a sample Ollama response and returns the expected model names.

## Task: Implement reading methods
- **Status:** Implementation Review
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

## Task: AvailableMaps registry for map identifiers
- **Status:** Finished
- **Scope:** Introduce AvailableMaps to provide session map identifiers backed by weak references and allow lookup by identifier.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/ControllerMapModelProvider.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/MapModelProvider.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/maps/AvailableMapsTest.java
- **Research summary:**
```plantuml
@startuml
note "AvailableMaps can lazily enumerate maps and assign uuid values.\nWeak references avoid retaining closed maps; lifecycle listeners are optional.\nMap identifiers are uuid values in system messages.\nController.getCurrentController().getMapViewManager().getMaps().values() provides open maps; getMap provides current map.\nControllerProxy.getOpenMindMaps de-duplicates map views." as Research
@enduml
```
- **Design:**
```plantuml
@startuml
note "AvailableMaps maintains weak map from MapModel to uuid values and reverse map from uuid values to WeakReference<MapModel>.\nMapModelProvider provides current and open maps; ControllerMapModelProvider uses Controller.getCurrentController().getMapViewManager().\nExpose methods for current identifier, available identifiers, and map lookup." as Design
@enduml
```
- **Test specification:**
  - Verify uuid stability for a map model.
  - Verify identifier list for open maps.
  - Verify lookup from identifier to map model.

## Task: System message map identifiers for reading methods
- **Status:** Finished
- **Scope:** Add the current map identifier, current root node identifier, and current selected node identifier to the system message output for reading methods.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/SystemMessageBuilder.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/ControllerMapModelProvider.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/MapModelProvider.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/SystemMessageBuilderTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/maps/AvailableMapsTest.java
- **Research summary:**
```plantuml
@startuml
note "AIToolSet.systemMessageForChat is the system message provider for AIChatService.\nAIChatPanel creates AIToolSet directly; builder must be constructed inside AIToolSet or provided there.\nController.getCurrentController().getMap() returns the current MapModel; MapModel.getRootNode().getID() provides the root node identifier.\nAvailableMaps provides uuid values for maps.\nSystem message is plain text with map identifier, root node identifier, and selected node identifier; missing values use not available." as Research
@enduml
```
- **Design:**
```plantuml
@startuml
note "SystemMessageBuilder reads AvailableMaps and builds a plain text message with map identifier, root node identifier, and selected node identifier.\nAIToolSet.systemMessageForChat returns this message." as Design
@enduml
```
- **Test specification:**
  - Verify identifiers are present when available.
  - Verify not available when map or selection is missing.

## Task: Node content qualifiers for summary nodes
- **Status:** Finished
- **Scope:** Add node qualifiers so AI can recognize summary and first group nodes without filtering them out; explain qualifiers in the system message.
- **Research summary:**
```plantuml
@startuml
class SummaryNode
class NodeModel
class NodeContentItem
class SystemMessageBuilder

SummaryNode --> NodeModel
SystemMessageBuilder --> NodeContentItem

note right of SummaryNode
SummaryNode.isSummaryNode and SummaryNode.isFirstGroupNode
identify structural nodes that may have empty text.
SummaryNode.isHidden is derived behavior for empty
summary or first group nodes.
end note

note bottom
Summary nodes are structural placeholders that
support navigation and grouping, including nesting.
They should not be filtered out for AI navigation.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class NodeContentItem {
nodeIdentifier
content
qualifiers : List<String>
}
class SummaryNode
class NodeModel

SummaryNode --> NodeModel
NodeContentItem --> NodeModel

note right of NodeContentItem
qualifiers include:
summary_node
first_group_node
end note

note bottom
Qualifiers are identity metadata on NodeContentItem.
No hidden qualifier is stored; hidden is derived.
System message explains the qualifiers and their meanings.
end note
@enduml
```
- **Test specification:**
  - Verify summary nodes include summary_node qualifier.
  - Verify first group nodes include first_group_node qualifier.
  - Verify non summary nodes have no qualifiers.
  - Verify system message includes qualifier descriptions.

## Task: Implement get_breadcrumbs tool
- **Status:** Implementation Review
- **Scope:** Implement get_breadcrumbs to return the root to node path, skipping hidden summary nodes and optionally including node identifiers.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/BreadcrumbsTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItemReader.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/BreadcrumbsToolTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/NodeContentItemReaderTest.java
- **Research summary:**
```plantuml
@startuml
class BreadcrumbLayout
class MapTreeNode
class SummaryNode
class NodeModel
class TextController

BreadcrumbLayout --> SummaryNode
MapTreeNode --> TextController
SummaryNode --> NodeModel

note right of MapTreeNode
Uses TextController.getShortPlainText for outline text.
end note

note right of BreadcrumbLayout
Skips SummaryNode.isHidden nodes when bridging
missing parents in breadcrumbs.
end note

note bottom
NodeModel.getPathToRoot includes parents regardless of
hidden summary nodes. SummaryNode.isHidden is derived
from summary or first group flags plus empty text.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class AIToolSet
class BreadcrumbsTool
class NodeContentItemReader
class BreadcrumbItem
class BreadcrumbsRequest
class BreadcrumbsResponse
class SummaryNode
class NodeModel

AIToolSet --> BreadcrumbsTool
BreadcrumbsTool --> NodeContentItemReader
BreadcrumbsTool --> BreadcrumbsRequest
BreadcrumbsTool --> BreadcrumbsResponse
NodeContentItemReader --> NodeModel
SummaryNode --> NodeModel

note right of AIToolSet
Delegates to BreadcrumbsTool.
end note

note right of BreadcrumbsTool
Resolve map and node identifiers.
Walk parent chain to root.
Skip SummaryNode.isHidden nodes.
Use NodeContentItemReader with BRIEF
to read breadcrumb text.
Include node identifiers only when requested.
end note

note bottom
Breadcrumb text uses short plain text to match
outline behavior.
end note
@enduml
```
- **Test specification:**
  - Verify breadcrumbs include root to target nodes in order.
  - Verify SummaryNode.isHidden nodes are skipped.
  - Verify node identifiers are included only when requested.
  - Verify invalid map or node identifiers raise errors.

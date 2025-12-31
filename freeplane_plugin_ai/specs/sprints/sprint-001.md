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
- **Status:** Identified
- **Scope:** Remove the Gson dependency from the ai plugin build file and use Jackson provided by LangChain4j instead.
- **Research summary:**
```plantuml
@startuml
note "Not started yet." as Research
@enduml
```
- **Design:**
```plantuml
@startuml
note "Not started yet." as Design
@enduml
```
- **Test specification:**
  - Not started yet.

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
FULL returns stored content only in TextualContent; briefText stays empty.
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
class ReadNodeContentRequest
class ReadNodeContentResponse
class AIToolSet
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
AIToolSet --> NodeContentItemReader
AIToolSet --> AvailableMaps
AIToolSet --> ReadNodeContentRequest
AIToolSet --> ReadNodeContentResponse

note right of AIToolSet
ReadNodeContentService is removed.
AIToolSet resolves controllers from the current mode controller and builds readers.
end note

note bottom
BRIEF uses TextController.getShortPlainText for focus, parent, children.
FULL focus content method is pending.
NodeContent.briefText is populated only for BRIEF responses.
TextualContent is populated only for FULL responses.
end note
@enduml
```
```plantuml
@startuml
actor Caller
participant AIToolSet
participant AvailableMaps
participant NodeContentItemReader
participant NodeContentReader
participant MapModel

Caller -> AIToolSet : read_node_content(map id, node id)
AIToolSet -> AvailableMaps : findMapModel(map id)
AIToolSet -> MapModel : getNodeForID(node id)
AIToolSet -> NodeContentItemReader : fromNodeModel(focus)
NodeContentItemReader -> NodeContentReader : buildNodeContent(focus, preset)
AIToolSet -> NodeContentItemReader : fromNodeModel(parent)
AIToolSet -> NodeContentItemReader : fromNodeModel(children)
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
  - Add tests for FULL focus content once implemented.

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

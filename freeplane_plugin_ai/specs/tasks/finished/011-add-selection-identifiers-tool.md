# Task: Add selection identifiers tool
- **Scope:** Add a tool that returns the currently selected map identifier, selected node identifier, and root node identifier.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/SelectedMapAndNodeIdentifiersTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/SelectionIdentifiersResponse.java
- **Research summary:**
```plantuml
@startuml
class AIToolSet
class AvailableMaps
class MapModelProvider
class Controller
class ModeController
class MapController

AIToolSet --> MapModelProvider
MapModelProvider --> Controller
Controller --> ModeController
ModeController --> MapController
MapController --> AvailableMaps
@enduml
```
- **Design:**
```plantuml
@startuml
class AIToolSet {
  +getSelectedMapAndNodeIdentifiers()
}
class SelectionIdentifiersResponse {
  +mapIdentifier
  +nodeIdentifier
  +rootNodeIdentifier
}
class AvailableMaps
class MapModelProvider
class MapController
class NodeModel

AIToolSet --> MapModelProvider
MapModelProvider --> MapController
MapController --> NodeModel
AIToolSet --> AvailableMaps
AIToolSet --> SelectionIdentifiersResponse

note right of AIToolSet
Returns the identifier for the selected map and the
selected node and the root node. Identifiers are resolved through
AvailableMaps.
end note
@enduml
```
- **Test specification:**
  - Verify the tool returns identifiers for the current map, selected node, and root node.
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

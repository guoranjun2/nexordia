# Task: System message map identifiers for reading methods
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
- **Research:**
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

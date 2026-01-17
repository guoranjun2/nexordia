# Task: Indicate nodes created or modified by AI
- **Scope:** Define and apply an AI edits marker extension on nodes created or modified by AI tools, expose undoable actions to clear those markers, add a state icon indicator for marked nodes, and add preferences to control persistence and icon visibility.
- **Motivation:** **Goal:** make artificial intelligence edits **transparent, optional, and reversible** so users can trust what changed and decide how long the signal should stay.
  
  **Why this is needed**
  - **Clarity:** A dedicated marker on nodes created or modified by artificial intelligence (edits or creation, not moves or deletions) provides a consistent way to identify machine edited content without changing the underlying text or metadata users already rely on.
  - **Control:** Users can clear the marker from a single node or from the entire map, and both operations are undoable so that resetting the marker never feels risky.
  - **Visibility choice:** Some users want a visual signal in the user interface, while others prefer a clean map. The state icon is configurable and can be disabled without removing the underlying marker.
  - **Persistence choice:** Some workflows need the marker to survive saving and reopening the map, while others treat it as a temporary cue during a session. Persistence is configurable and defaults can be set per user preference.
  
  **What users can do**
  - **Automatic tagging:** The marker is always applied when artificial intelligence creates or edits node content. It is not applied for moves or deletions.
  - **Reset for map:** One action clears all artificial intelligence markers after review.
  - **Reset for node:** One action clears markers for selected nodes when only specific items are approved.
  - **Show or hide the icon:** Visibility can be turned on or off without removing the marker.
  - **Persist or not:** The marker can be saved with the map or kept as a session only signal.
  
  **Suggestions for future enhancements (not part of this task yet)**
  - **Edit summary:** Store a short summary of the artificial intelligence edits and show it in a tooltip or in a dedicated node area.
  
  **What this supports**
  - Fast review workflows where users quickly verify and then clear markers.
  - Long term auditing workflows where markers remain available across sessions.
  - Shared team conventions without forcing a single visual style or storage policy.
- **Research:** Detailed findings live in the subtasks.
- **Design:** Detailed design decisions live in the subtasks.
- **Test specification:** Planned tests are defined per subtask.
- **Modified files:** Tracked per subtask.

## Subtask: Mark AI edits in tools
- **Status:** Implementation Review
- **Scope:** Add the `AIEdits` extension and attach it to nodes created or edited by AI tools. No persistence or icon behavior yet.
- **Motivation:** Provide immediate AI edit tracking in data without changing storage or user interface.
- **Research:** Tool edit and create paths already support undoable changes.
  ```plantuml
  @startuml
  class AIToolSet
  class CreateNodesTool
  class NodeInserter
  class MMapController
  class NodeContentEditor
  class TextualContentEditor

  AIToolSet --> CreateNodesTool : createNodes
  CreateNodesTool --> NodeInserter : insertNodes
  NodeInserter --> MMapController : insertNode
  AIToolSet --> NodeContentEditor : edit
  NodeContentEditor --> TextualContentEditor

  note right of MMapController
  insertNode uses an undoable actor
  end note
  @enduml
  ```
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java` exposes the `createNodes` and `edit` tools and delegates to `CreateNodesTool` and `NodeContentEditor`.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/create/CreateNodesTool.java` inserts nodes using `NodeInserter`.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/create/NodeInserter.java` calls `MMapController.insertNode`, which is undoable in `freeplane/src/main/java/org/freeplane/features/map/mindmapmode/MMapController.java`.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/edit/NodeContentEditor.java` edits nodes through controllers that are already undo-aware.
- **Design:** Attach `AIEdits` during creation and edits.
  ```plantuml
  @startuml
  class AIEdits
  class CreateNodesTool
  class NodeContentEditor

  CreateNodesTool --> AIEdits
  NodeContentEditor --> AIEdits
  @enduml
  ```
  - Add `AIEdits` marker implementing `IExtension`.
  - When `CreateNodesTool` creates nodes, attach `AIEdits` to the created nodes without adding a separate undo step.
  - When `NodeContentEditor` applies edits, attach `AIEdits` to the edited nodes using an undoable actor.
  - Do not attach `AIEdits` to hidden summary helper nodes; only the visible summary node should be marked when it is created or edited.
- **Test specification:** Tool tests confirm `createNodes` and `edit` attach `AIEdits` to their target nodes.
- **Modified files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/create/CreateNodesTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/create/NodeModelCreator.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/edit/NodeContentEditor.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/AIEdits.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/create/CreateNodesToolTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/edit/NodeContentEditorTest.java

## Subtask: Actions and state icon visibility setting
- **Status:** Implementation Review
- **Scope:** Add undoable actions to clear AI edits from a node or from the whole map, plus a state icon provider controlled by a visibility setting.
- **Motivation:** Make AI edits visible and reversible without persisting them yet.
- **Research:** State icons are registered through `IconController` providers.
  ```plantuml
  @startuml
  class IconController
  interface IStateIconProvider
  class NoteController

  IconController o--> IStateIconProvider
  NoteController ..> IStateIconProvider

  note right of IconController
  getStateIcons asks providers
  and registers returned user interface icons
  end note
  @enduml
  ```
  - State icons are provided through `IStateIconProvider` implementations registered in `IconController`.
  - `freeplane/src/viewer/resources/freeplane.properties` defines the `icons.state` list and `stateIcon.*` mappings for state icons.
  - `freeplane/src/viewer/resources/images/ai.svg` already exists and can be referenced by a state icon entry.
- **Design:** Add visibility settings, state icon provider, and undoable actions.
  ```plantuml
  @startuml
  class AiEditsSettings
  class AiEditsStateIconProvider
  class ClearAiMarkersInMapAction
  class ClearAiMarkersInSelectionAction

  AiEditsStateIconProvider --> AiEditsSettings
  ClearAiMarkersInMapAction --> AIEdits
  ClearAiMarkersInSelectionAction --> AIEdits
  @enduml
  ```
  - Add `AiEditsSettings` with a boolean property for state icon visibility.
  - Add `AiEditsStateIconProvider` returning `ai.svg` when `AIEdits` is present and icon visibility is enabled.
  - Add undoable actions:
    - `ClearAiMarkersInMapAction` clears `AIEdits` for all nodes in the active map.
    - `ClearAiMarkersInSelectionAction` clears `AIEdits` for the current selection.
  - Register the state icon provider and actions in the AI plugin activator.
  - Add a `SetBooleanPropertyAction` for the visibility property and include it alongside the actions in the AI panel menu.
  - Add labels and tooltips for the actions and the visibility option.
  - Refresh node state icons when the visibility property changes.
  - Add the state icon entry for `ai.svg` in `freeplane.properties`.
- **Test specification:** State icon provider returns `ai.svg` only when `AIEdits` exists and visibility is enabled; action tests confirm undo restores removed extensions.
- **Modified files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/Activator.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/AiEditsSettings.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/AiEditsStateIconProvider.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/ClearAiMarkersInMapAction.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/ClearAiMarkersInSelectionAction.java
  - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml
  - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties
  - freeplane/src/viewer/resources/freeplane.properties
  - freeplane/src/viewer/resources/images/ai.svg
  - freeplane/src/viewer/resources/translations/Resources_en.properties
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/edits/AiEditsStateIconProviderTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/edits/ClearAiMarkersActionsTest.java

## Subtask: Persistence setting and serialization
- **Status:** Planning
- **Scope:** Add persistence of `AIEdits` using attribute handlers and extension writers, controlled by a persistence setting.
- **Motivation:** Allow optional storage of AI edit indicators across saves.
- **Research:** `WriteManager` and `ReadManager` support attribute handlers and extension writers.
  ```plantuml
  @startuml
  class WriteManager
  class ReadManager
  class IExtensionAttributeWriter

  WriteManager o--> IExtensionAttributeWriter
  ReadManager ..> IExtensionAttributeWriter
  @enduml
  ```
  - `WriteManager` supports extension attribute writers and extension element writers that can be used instead of `PersistentNodeHook`.
- **Design:** Add persistence builder and settings.
  ```plantuml
  @startuml
  class AiEditsPersistenceBuilder
  class AiEditsSettings

  AiEditsPersistenceBuilder --> AiEditsSettings
  AiEditsPersistenceBuilder --> AIEdits
  @enduml
  ```
  - Add `AiEditsPersistenceBuilder` that registers:
    - a `ReadManager` attribute handler for a node attribute (for example, `AI_EDITS`) to install `AIEdits`
    - a `WriteManager` extension attribute writer that writes the attribute only when persistence is enabled
  - Add a persistence boolean setting with preference and defaults.
  - Register the persistence builder during AI plugin startup.
- **Test specification:** Persistence writer skips output when persistence is disabled and writes when enabled.
- **Modified files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/Activator.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/AiEditsPersistenceBuilder.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/edits/AiEditsSettings.java
  - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml
  - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties

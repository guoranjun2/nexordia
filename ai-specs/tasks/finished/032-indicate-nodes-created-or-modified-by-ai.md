# Task: Indicate nodes created or modified by AI
- **Task Identifier:** 2026-01-17-ai-edits
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
- **Developer Briefing:** AI tools in the plugin already create and edit nodes through undo-aware paths. This task adds an `AIEdits` marker extension, uses AI tooling to attach it on create/edit, and layers user controls (clear actions, icon visibility, persistence) without changing existing AI tool behavior. The subtasks split data marking, UI visibility, and persistence so each increment can be implemented and verified independently.
- **Research:** AI tool flows are already undo-aware and route through `CreateNodesTool`/`NodeInserter` and `NodeContentEditor`. State icon providers are registered in `IconController`, and `ai.svg` already exists. Persistence can be implemented via `ReadManager`/`WriteManager` attribute handlers instead of `PersistentNodeHook`.
- **Design:** Add an `AIEdits` extension that is attached during AI create/edit flows; add undoable actions to clear markers; add a state icon provider gated by a visibility setting; and add persistence that is controlled by a preference and writes the attribute only when enabled.
- **Test specification:** For each subtask, add focused tests: marker attachment on AI create/edit, icon visibility and marker clearing logic, and persistence writer behavior when enabled/disabled.

## Subtask: Mark AI edits in tools
- **Status:** Implementation Review
- **Scope:** Add the `AIEdits` extension and attach it to nodes created or edited by AI tools. No persistence or icon behavior yet.
- **Motivation:** Provide immediate AI edit tracking in data without changing storage or user interface.
- **Developer Briefing:** The AI tools already call undo-aware create and edit paths, so the marker can be attached at the end of those flows. Keep the marker attachment minimal and avoid adding extra undo steps beyond existing create/edit actions.
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

## Subtask: Actions and state icon visibility setting
- **Status:** Finished
- **Scope:** Add undoable actions to clear AI edits from a node or from the whole map, plus a state icon provider controlled by a visibility setting.
- **Motivation:** Make AI edits visible and reversible without persisting them yet.
- **Developer Briefing:** This subtask adds reversible user actions and a UI indicator without changing persistence. The state icon provider should be a thin wrapper over a testable decision helper, and marker removal should be centralized in a helper so the actions stay focused on wiring selections and undo.
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
  - `IconStoreFactory.ICON_STORE` initializes from `ResourceController` at class load time, which is not available in unit tests without framework initialization.
  - The clear marker actions depend on `Controller.getCurrentController()` and `MapController`, which are also not available without additional framework setup in unit tests.
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
  - Move state icon decision logic into a test friendly helper that accepts visibility and marker state, while the provider only supplies the icon when needed.
  - Move marker removal and undo logic into a helper that operates on `NodeModel` collections and `MapController` callbacks, with actions only responsible for wiring the selection and executing the undoable actor.
  - Add undoable actions:
    - `ClearAiMarkersInMapAction` clears `AIEdits` for all nodes in the active map.
    - `ClearAiMarkersInSelectionAction` clears `AIEdits` for the current selection.
  - Register the state icon provider and actions in the AI plugin activator.
  - Add a `SetBooleanPropertyAction` for the visibility property and include it alongside the actions in the AI panel menu.
  - Add labels and tooltips for the actions and the visibility option.
  - Refresh node state icons when the visibility property changes.
  - Add the state icon entry for `ai.svg` in `freeplane.properties`.
- **Test specification:** Helper tests cover icon visibility decisions and marker removal with undo without requiring `ResourceController` or `Controller` initialization; provider tests verify icon presence when visibility is enabled and the marker is set.

## Subtask: Persistence setting and serialization
- **Status:** Implementation Review
- **Scope:** Add persistence of `AIEdits` using attribute handlers and extension writers, controlled by a persistence setting.
- **Motivation:** Allow optional storage of AI edit indicators across saves.
- **Developer Briefing:** Persist the marker via attribute handlers registered with `ReadManager`/`WriteManager`, and gate writes on a preference. Reading should always hydrate stored markers from maps so user data is preserved, while the preference only controls whether new saves emit the attribute.
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
  - `TagBuilder` registers `ReadManager` attribute handlers on `NodeBuilder.XML_NODE` and `NodeBuilder.XML_STYLENODE`, then adds a `WriteManager` extension attribute writer for `Tags`.
  - `NodeEnumerationAttributeHandler` combines `IAttributeHandler` and `IExtensionAttributeWriter` to read/write a simple node attribute through `ReadManager`/`WriteManager`.
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
    - a `ReadManager` attribute handler for a node attribute (for example, `AI_EDITS`) on `NodeBuilder.XML_NODE` and `NodeBuilder.XML_STYLENODE` that installs `AIEdits` when the attribute is present
    - a `WriteManager` extension attribute writer for `AIEdits` that writes the attribute only when persistence is enabled and the marker is present
  - Keep read behavior unconditional so maps containing `AI_EDITS` always rehydrate markers, even when persistence is disabled.
  - When persistence is disabled, do not emit `AI_EDITS` on save even if markers were read from the map file.
  - Add a persistence boolean setting with preference and defaults (default off), scoped under the AI plugin settings class.
  - Register the persistence builder during AI plugin startup alongside other AI edit wiring.
- **Test specification:**
  - Verify the attribute handler attaches `AIEdits` when the `AI_EDITS` attribute is read on both `XML_NODE` and `XML_STYLENODE`.
  - Verify the attribute writer emits `AI_EDITS` only when persistence is enabled and the marker is present, even if the marker came from a prior read.
  - Verify that disabling persistence does not remove existing `AIEdits` markers in memory after a read.

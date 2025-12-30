# Sprint 001

## Task: Read context request hierarchy and presets
**Status:** Finished
**Scope:** Define a new internal NodeContentRequest hierarchy with content presets for focus, parent, and child nodes.
**Research summary:**
*   NodeContent groups TextualContent, AttributesContent, and TagsContent.
*   TextualContent includes text, details, and note.
*   AttributesContent stores a list of AttributeEntry name and value pairs.
*   TagsContent stores a list of tag strings.
*   The previous NodeContextRequest included map identifier, node identifier, depth, include flags, and output format.
*   The previous NodeContextResponse included map identifier, output format, and payload.
*   AIToolSet previously exposed readNodeContext with NodeContextRequest and NodeContextResponse.
*   LangChain4j uses an internal JacksonJsonCodec by default; the ObjectMapper does not enable non-null inclusion, so null fields are serialized, missing fields deserialize as null for reference types and default values for primitives, and unknown properties are rejected unless a custom JsonCodecFactory is provided.
**Plan:**
1. Define internal request types: NodeContentRequest, TextualContentRequest, AttributesContentRequest, TagsContentRequest, and NodeContextContentRequest for focus, parent, and child nodes.
2. Define content presets such as full and brief, with full covering all current content parts and brief covering text only.
3. Add a preset resolver that maps presets to concrete requests for focus, parent, and child nodes.

## Task: Remove Gson dependency from ai plugin
**Status:** Identified
**Scope:** Remove the Gson dependency from the ai plugin build file and use Jackson provided by LangChain4j instead.
**Research summary:**
*   Not started yet.
**Plan:**
1. Not started yet.

## Task: Implement reading methods
**Status:** Implementing
**Scope:** Replace NodeContextRequest and NodeContextResponse with a new read context request and response that only require map identifier and node identifier, always include node identifiers in the response, and return focus, parent, and child nodes with preset content; update AIToolSet to use the new method and remove the old request and response types.
**Research summary:**
*   Research in progress; user approved removing the old read context request and response types and aligning flat list content selection now.
*   NodeProxy reads node text via NodeModel.getText and transformed text via TextController.getTransformedTextNoThrow, with plain text derived through HtmlUtils.htmlToPlain.
*   NodeProxy reads details via DetailModel.getDetailText and note content via NoteModel.getNoteText, with content types coming from TextController and MNoteController.
*   NodeProxy reads node identifiers via NodeModel.createID, parent via NodeModel.getParentNode, and children via ProxyUtils.createListOfChildren, which uses NodeModel.getChildAt and getChildCount.
*   AttributesProxy reads attributes through NodeAttributeTableModel and AttributeController, preserving name and value pairs and optionally transforming values using TextController.
*   TagsProxy reads tags via MIconController.getTags and maps Tag.getContent to tag strings.
*   MapController.getNodeAt delegates to MapExplorerController.getNodeAt with the map root, supporting references like ID-based lookups and path-based lookups.
*   MapExplorerController.getNodeAt resolves references that start with \"ID\" using MapModel.getNodeForID and \"at(...)\" references by URL decoding the path and running MapExplorer.
*   MapExplorer path parsing supports root (\"/\"), global (\":\"), ancestor (\"..\"), descendant (\"**\"), child steps, aliases (\"~alias\"), quoted text matches, and \"*\" shorthand for \"'...'\".
*   NodeMatcher matches alias, exact text, or prefix text (\"...\" suffix) using HtmlUtils.htmlToPlain on node text; counters are supported via numeric alias syntax.
*   MapExplorer steps traverse using NodeModel.getChildren, NodeStream for descendants, and GlobalNodes for global lookups while tracking accessed nodes.
**Plan:**
1. Define a new read context request and response pair that use the fixed focus, parent, and child structure.
2. Replace the old read context request and response types and tool method with the new read context method.
3. Update flat list requests to use NodeContentRequest for content selection while keeping structural flags as booleans.
4. Update related data structures or usages to align with the new response type.
5. Add focused tests for preset selection and the default focus, parent, and child content settings, using the `uut` naming guideline.
6. Verify with `gradle :freeplane_plugin_ai:test` after cleanup.

## Task: AvailableMaps registry for map identifiers
**Status:** Finished
**Scope:** Introduce AvailableMaps to provide session map identifiers backed by weak references and allow lookup by identifier.
**Modified production files:**
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/ControllerMapModelProvider.java
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/MapModelProvider.java
**Modified test files:**
*   freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/maps/AvailableMapsTest.java
**Research summary:**
*   AvailableMaps can lazily enumerate maps on demand (all maps or the selected map) and assign UUIDs without listening to map lifecycle events.
*   Weak references are sufficient to avoid retaining closed maps; explicit lifecycle listeners remain optional.
*   UUIDs should be used internally; system messages can expose the UUID as the map identifier alongside the current root node identifier.
*   Controller.getCurrentController().getMapViewManager().getMaps().values() provides the open MapModel list, and Controller.getCurrentController().getMapViewManager().getMap() provides the current map.
*   ControllerProxy.getOpenMindMaps uses MapViewManager.getMaps().values().stream().distinct() to collapse multiple views of the same map.
**Plan:**
1. Add AvailableMaps with a weak map from MapModel to UUID and a reverse map from UUID to WeakReference<MapModel>, with lazy cleanup when lookups find cleared references.
2. Add a small provider interface (for example MapViewProvider) so AvailableMaps can obtain the current map and open maps without hard-coding static Controller access, and supply a default provider that uses Controller.getCurrentController().getMapViewManager().
3. Expose methods to get or create a UUID for the current map, list available map identifiers, and resolve a UUID back to a MapModel if it is still available.
4. Add unit tests using a stub provider and mock MapModel instances to cover UUID assignment, reuse, and stale reference cleanup; follow the `uut` naming guideline.
5. Verify with `gradle :freeplane_plugin_ai:test` after cleanup.

## Task: System message map identifiers for reading methods
**Status:** Finished
**Scope:** Add the current map identifier, current root node identifier, and current selected node identifier to the system message output for reading methods.
**Modified production files:**
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/SystemMessageBuilder.java
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/ControllerMapModelProvider.java
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/MapModelProvider.java
*   freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
**Modified test files:**
*   freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/SystemMessageBuilderTest.java
*   freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/maps/AvailableMapsTest.java
**Research summary:**
*   AIToolSet.systemMessageForChat currently throws UnsupportedOperationException; it is the system message provider for AIChatService.
*   AIChatPanel creates AIToolSet directly without dependencies; any system message builder must be constructed inside AIToolSet or added as a dependency there.
*   Controller.getCurrentController().getMap() returns the current MapModel; MapModel.getRootNode().getID() provides the root node identifier.
*   AvailableMaps exists in the ai plugin and can provide a UUID identifier for the current MapModel.
*   The system message should be plain text and include only the current map identifier and root node identifier, with \"not available\" when missing.
*   Map identifiers should use UUID values from AvailableMaps.
*   The system message builder can rely on AvailableMaps and keep MapModelProvider hidden behind it.
*   The system message should include the current selected node identifier and use \"not available\" when missing.
**Plan:**
1. Define the plain text system message format for map identifier, root node identifier, and selected node identifier fields, using \"not available\" when missing.
2. Add a system message builder that uses AvailableMaps to resolve the current map UUID, root node identifier, and selected node identifier, keeping MapModelProvider hidden behind AvailableMaps.
3. Update AIToolSet.systemMessageForChat to return the system message with map identifier, root node identifier, and selected node identifier.
4. Add focused unit tests for the system message builder (if a helper class is introduced) and verify with `gradle :freeplane_plugin_ai:test` after cleanup.

# Project Backlog

We will implement the AI tool capabilities in epics, refining each one before execution.

## Epics

### Epic 1: Read Context and Navigation Tools
**Goal:** Implement core read tools used in chat workflows.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want to read node context and navigate paths so I can answer questions accurately.
*   **Tasks:**
    *   [ ] Define internal node content request hierarchy with content presets for read context.
    *   [ ] Implement a new read context method with fixed parent and child structure and remove the old read context request and response types.
    *   [ ] Create AvailableMaps to manage map identifiers using weak references.
    *   [ ] Implement get_breadcrumbs to return the root to node path.
    *   [ ] Implement get_flat_list for branch scanning with optional breadcrumbs.
    *   [ ] Remove the Gson dependency from the ai plugin and rely on Jackson provided by LangChain4j.

### Epic 2: Map Editing Tools
**Goal:** Implement the map modification tools used after chat approval.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want to create and rearrange nodes and apply attributes after user approval.
*   **Tasks:**
    *   [ ] Implement create_nodes with subtree support and insert positioning.
    *   [ ] Implement apply_attributes with merge and remove_missing options.
    *   [ ] Implement move_nodes with ordering and positioning options.

### Epic 3: Condition Discovery and Validation
**Goal:** Provide property and condition names for AI tools using the same translation text as the filter UI, backed by the existing condition controllers.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want to discover valid properties and conditions so I can build valid search requests.
*   **Tasks:**
    *   [ ] Define the translation text mapping for property and condition values backed by controller keys.
    *   [ ] Reuse the filter UI translation pipeline so the tool outputs match existing filter names.
    *   [ ] Implement list_search_properties and exclude attribute conditions.
    *   [ ] Implement list_search_conditions_for_property with value_input_mode and option flags.
    *   [ ] Reject property_name and condition_name values that are not returned by the discovery tools.

### Epic 4: Condition Based Search Execution
**Goal:** Execute searches using the same condition model as filtering.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want to search nodes using controller backed conditions so results match Freeplane filtering behavior.
*   **Tasks:**
    *   [ ] Implement search_nodes_by_condition with scope support and result limits.
    *   [ ] Map translation text names back to controller keys when building conditions.
    *   [ ] Define consistent behavior for conditions that require no value.

### Epic 5: Search Overview Tool
**Goal:** Provide a compact overview and index using a selectable model.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want a low cost overview to guide targeted searches.
*   **Tasks:**
    *   [ ] Implement generate_search_overview with focus_request and model_identifier.
    *   [ ] Define chunking and aggregation rules for themes, sections, and keywords.
    *   [ ] Ensure the summary and index stay within the requested size limits.

### Epic 6: AI Only Filter State
**Goal:** Store and apply an AI only filter without changing the user visible filter state.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want to narrow subsequent tool calls without altering the user view.
*   **Tasks:**
    *   [ ] Implement set_ai_only_filter_condition, get_ai_only_filter_condition, and clear_ai_only_filter_condition.
    *   [ ] Store filter state per map identifier.
    *   [ ] Apply the AI only filter to AI tool read and search operations.

### Epic 7: Attribute Tools
**Goal:** Provide attribute name discovery and attribute search as separate tools.
*   **Status:** Postponed
*   **User Story:** As an AI assistant, I want to search by attributes using a map specific list of attribute names.
*   **Tasks:**
    *   [ ] Implement list_attribute_names_for_map based on map attribute data.
    *   [ ] Implement search_attributes_by_name_and_value with map specific lookup.

### Epic 8: Chat System Message Builder
**Goal:** Grow the system message incrementally as read and search tools require additional context.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want the system message to include just the context needed for the current tool set.
*   **Tasks:**
    *   [ ] Add system message fields alongside each feature (for example, map identifier and root node identifier for reading methods).
    *   [ ] Add the current map identifier and root node identifier to the system message for reading methods.
    *   [ ] Track system message content in specs as new tool capabilities are added.

## Sprint 1

### Task: Read context request hierarchy and presets
**Status:** Implementation Review
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

### Task: Remove Gson dependency from ai plugin
**Status:** Identified
**Scope:** Remove the Gson dependency from the ai plugin build file and use Jackson provided by LangChain4j instead.
**Research summary:**
*   Not started yet.
**Plan:**
1. Not started yet.

### Task: Implement reading methods
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

### Task: AvailableMaps registry for map identifiers
**Status:** Plan Review
**Scope:** Introduce AvailableMaps to provide session map identifiers backed by weak references and allow lookup by identifier.
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

### Task: System message map identifiers for reading methods
**Status:** Identified
**Scope:** Add the current map identifier and root node identifier to the system message output for reading methods.
**Research summary:**
*   Not started yet.
**Plan:**
1. Not started yet.

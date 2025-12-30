# Project Backlog

We will implement the AI tool capabilities in epics, refining each one before execution.

**[Current sprint](./sprints/sprint-001.md)**

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
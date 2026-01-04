# Project Backlog

We will implement the AI tool capabilities in epics, refining each one before execution.

**[Current sprint](./sprints/sprint-002.md)**

## Epics


### Epic 1: Read Context and Navigation Tools
**Goal:** Implement core read tools used in chat workflows.
*   **Status:** In progress
*   **User Story:** As an AI assistant, I want to read node context and navigate paths so I can answer questions accurately.
*   **Tasks:**
    *   [x] Read context request hierarchy and presets.
    *   [x] Implement reading methods.
    *   [x] AvailableMaps registry for map identifiers.
    *   [x] System message map identifiers for reading methods.
    *   [x] Node content qualifiers for summary nodes.
    *   [?] Remove the Gson dependency from the ai plugin and rely on Jackson provided by LangChain4j.
    *   [x] Review llm feedback for read tools and propose revisions.
    *   [x] Return textual content as plain text for read tools.
    *   [ ] Add icon content to node responses with icon names and optional emoji decoding.
    *   [x] Add selected map, node, and root node identifiers tool.

### Epic 2: Map Editing Tools
**Goal:** Implement the map modification tools used after chat approval.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want to create and rearrange nodes and apply attributes after user approval.
*   **Tasks:**
    *   [ ] Implement create_nodes with subtree support and insert positioning.
    *   [ ] Implement apply_attributes with merge and remove_missing options.
    *   [ ] Implement move_nodes with ordering and positioning options.

### Epic 3: Content Search Tools
**Goal:** Provide search tools that use NodeContentRequest scope and pagination.
*   **Status:** Finished
*   **User Story:** As an AI assistant, I want to search nodes by content with explicit scope so I can jump directly to relevant areas.
*   **Tasks:**
    *   [x] Implement search_nodes with subtree roots, matching mode, and pagination.
    *   [x] Support case sensitivity for contains, equals, and regular expression matching.
    *   [x] Enforce response text budget by omitting results instead of truncating values.

### Epic 5: Search Overview Tool
**Goal:** Provide a compact overview and index using a selectable model.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want a low cost overview to guide targeted searches.
*   **Tasks:**
    *   [ ] Implement generate_search_overview with focus_request and model_identifier.
    *   [ ] Define chunking and aggregation rules for themes, sections, and keywords.
    *   [ ] Ensure the summary and index stay within the requested size limits.

### Epic 6: Editable Content for Safe Edits
**Goal:** Provide raw content plus format metadata so edits do not destroy formulas or markup.
*   **Status:** Backlog
*   **User Story:** As an AI assistant, I want access to editable content with format metadata so I can make safe edits.
*   **Tasks:**
    *   [ ] Define EditableContentRequest and EditableContent response structures.
    *   [ ] Expose raw, transformed, and plain text representations for text, details, note, and attributes.
    *   [ ] Expose format metadata such as content type, markup detection, and formula detection.

### Epic 7: Attribute Tools
**Goal:** Provide attribute name discovery and attribute search as separate tools.
*   **Status:** Postponed
*   **User Story:** As an AI assistant, I want to search by attributes using a map specific list of attribute names.
*   **Tasks:**
    *   [ ] Implement list_attribute_names_for_map based on map attribute data.
    *   [ ] Implement search_attributes_by_name_and_value with map specific lookup.

### Epic 8: Chat Session Controls and Usage
**Goal:** Provide chat session controls, usage status, and logging in the chat panel.
*   **Status:** Backlog
*   **User Story:** As a user, I want to control chat memory and see token usage and tool call logs so I can monitor context usage and restart sessions.
*   **Tasks:**
    *   [x] Add chat memory controls, a chat status line for token usage, and a tool call log.

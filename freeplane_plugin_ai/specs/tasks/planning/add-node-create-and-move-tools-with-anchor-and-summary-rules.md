# Task: Add node create and move tools with anchor and summary rules
- **Scope:** Add tools to create nodes and move nodes, including anchor placement rules, ordered group handling, and optional summary creation for a group. Return modified node identifiers and short texts for every successful operation. Run only on specific user requests.
- **Motivation:** Editing requires consistent create and move operations. Newly created nodes only receive identifiers during creation, so the tool response must include those identifiers and short texts for follow up actions.
- **Research summary:**
  - Review existing map controller helpers for creating nodes, inserting at specific positions, and moving nodes.
  - Review summary node creation helpers and constraints for summary anchors.
  - Review how short text is derived for tool responses to keep response size stable.
- **Design:**
  - Require an anchor node and a placement mode for each operation: first child, last child, sibling before, sibling after.
  - Allow creation requests to include recursive children so full subtrees can be created in one request.
  - Require a user summary string in the request and return it in the response for display.
  - Treat a request as an ordered group of nodes; place nodes in the exact provided order without relying on current map order.
  - Reject groups that contain nodes whose top level entries include descendants of other nodes in the same group.
  - Support optional summary creation for a group of created or moved nodes, anchored by the first and last node in the group.
  - Require summary anchor nodes to share the same parent node; return an error otherwise.
  - Enforce map consistency and return errors for invalid operations, such as moving a node into its own subtree.
  - After success, return a structure that includes identifiers and short texts for all modified nodes.
  - Formatting and style manipulation are out of scope for this tool.
- **Test specification:**
  - Verify each anchor placement mode inserts or moves nodes at the correct position.
  - Verify group ordering is preserved exactly as provided.
  - Verify groups containing parent and descendant nodes are rejected.
  - Verify summary creation uses the first and last nodes and rejects different parent nodes.
  - Verify moving nodes into their own subtree returns an error.
  - Verify responses include identifiers and short texts for all modified nodes, including newly created nodes.

## Subtasks

### Subtask: Define request and response structures
- **Status:** Finished
- **Scope:** Define request fields, response shape, and modified node summaries for create and move operations, including shared anchor and ordering structures.
- **Motivation:** Clear contracts are required before implementation and testing, especially for ordered groups, summaries, and user facing confirmation text.
- **Research summary:**
  - Review existing request and response structures for read and search tools to align field naming and default handling.
  - Review existing InsertPosition usage and whether a shared anchor placement enum already exists.
- **Design:**
  - Define a shared anchor placement component with anchor node identifier and placement mode (first child, last child, sibling before, sibling after).
  - Define create request fields: map identifier, anchor placement or summary anchor placement (mutually exclusive), ordered group list of new node definitions with recursive children, and optional summary content.
  - Define move request fields: map identifier, anchor placement or summary anchor placement (mutually exclusive), ordered group list of existing node identifiers, and optional summary content.
  - Require a user summary string in each request and return it in the response for display.
  - Model summary creation as a separate summary block that references the summarized node group; the summary is anchored by the first and last summarized nodes, not by the moved or created group.
  - SummaryAnchorPlacement consists of two node identifier strings that must reference existing nodes to be summarized.
  - Do not include summary content in the request; summary nodes are technical and generally have no text.
  - Summary creation for newly created nodes requires a follow up call after node identifiers exist.
  - Define response fields: map identifier, user summary, modified node list with identifiers and short texts in tool order, and optional summary node identifier when created.
  - Ensure error responses cover invalid identifiers, invalid group ordering, and unsupported operations.
- **Test specification:**
  - Verify request and response schema serialization and default values.
  - Verify response ordering matches the provided group order.
- **Modified files:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AnchorPlacement.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AnchorPlacementMode.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/CreateNodesRequest.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/CreateNodesResponse.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ModifiedNodeSummary.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/MoveNodesRequest.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/MoveNodesResponse.java`
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/SummaryAnchorPlacement.java`

### Subtask: Create NodeModel elements for new nodes
- **Status:** Planning
- **Scope:** Create new NodeModel elements for a request group and place them relative to the anchor before applying content.
- **Motivation:** Separate structural creation from content updates so creation order and placement are deterministic.
- **Research summary:**
  - Review node creation helpers and insertion helpers that support child and sibling placement.
- **Design:**
  - Create new nodes in the exact group order.
  - Apply insertion for the entire group relative to the anchor node.
  - Preserve the provided order, independent of existing map order.
  - Skip undo integration for creation because no existing nodes are modified.
- **Test specification:**
  - Verify each placement mode produces the expected order for newly created nodes.
  - Verify group order is preserved exactly.

### Subtask: Apply content to created nodes with content editors
- **Status:** Planning
- **Scope:** Apply content values to newly created nodes using content editor helpers.
- **Motivation:** Content updates should be consistent across types and reusable for later edit tools.
- **Research summary:**
  - Review how text, details, and note updates are applied through TextController.
  - Review how attribute, tag, and icon updates are applied and how explicit node icons are distinguished from style icons.
- **Design:**
  - Introduce TextualContentEditor, AttributesContentEditor, TagsContentEditor, and IconsContentEditor helpers.
  - Support operation modes with and without undo; use the no undo mode for creation in this task.
  - Apply content for text, details, note, attributes, tags, and explicit node icons.
- **Test specification:**
  - Verify each content editor applies values for its content type.
  - Verify explicit node icons are updated without style icons.
  - Verify no undo recording is used for creation operations.

### Subtask: Implement anchor placement and ordering rules for moves
- **Status:** Planning
- **Scope:** Implement placement for first child, last child, sibling before, and sibling after with strict group ordering for moved nodes.
- **Motivation:** Deterministic placement is needed to keep edits predictable.
- **Research summary:**
  - Review insertion helpers that support child and sibling placement for existing nodes.
- **Design:**
  - Apply insertion for the entire moved group relative to the anchor node.
  - Preserve the provided order, independent of existing map order.
- **Test specification:**
  - Verify each placement mode produces the expected order for moved nodes.
  - Verify group order is preserved exactly.

### Subtask: Validate group and move constraints
- **Status:** Planning
- **Scope:** Reject invalid groups and invalid move targets.
- **Motivation:** Invalid moves can corrupt the map or create cycles.
- **Research summary:**
  - Review ancestry checks and subtree detection helpers.
- **Design:**
  - Reject groups containing nodes that are descendants of other group nodes at the top level.
  - Reject moves into a node's own subtree.
- **Test specification:**
  - Verify ancestor and descendant conflicts are rejected.
  - Verify subtree moves are rejected with a clear error.

### Subtask: Handle clone aware moves and summaries
- **Status:** Planning
- **Scope:** Ensure create and move operations respect clone behavior.
- **Motivation:** Cloned nodes share content and sometimes subtrees, so moves can affect multiple views.
- **Research summary:**
  - Review how clone nodes are represented and how moving a clone affects its siblings.
- **Design:**
  - Ensure move and summary operations do not break clone relationships.
  - Decide whether creation should support creating clones as part of a group.
- **Test specification:**
  - Verify clone relationships are preserved after moves and summary creation.

### Subtask: Support summary creation for a group
- **Status:** Planning
- **Scope:** Create a summary anchored to the first and last nodes of a summarized group that is separate from the moved or created group.
- **Motivation:** Summary creation is a common Freeplane operation and uses its own anchor model based on summarized nodes.
- **Research summary:**
  - Review summary node creation helpers and constraints.
- **Design:**
  - Require first and last summarized nodes to share a parent before creating the summary.
  - Require SummaryAnchorPlacement with two existing node identifiers that define the summarized range.
  - Define how summary content is provided for the new summary node.
  - Require a follow up call to summarize newly created nodes once identifiers exist.
  - Return errors for invalid summary anchors.
- **Test specification:**
  - Verify summary creation succeeds with valid anchors.
  - Verify invalid anchor parents are rejected.

### Subtask: Track and return modified node summaries
- **Status:** Planning
- **Scope:** Return identifiers and short texts for all modified nodes after creation or move.
- **Motivation:** The model needs identifiers for follow up edits, especially for newly created nodes.
- **Research summary:**
  - Review short text generation rules used by read tools.
- **Design:**
  - Return modified node identifiers and short texts in tool order.
  - Include newly created nodes and moved nodes in the response.
- **Test specification:**
  - Verify response includes identifiers and short texts for all modified nodes.

Goal and scope
- Enable a large language model to understand and improve large Freeplane mind maps without reading the entire map at once.
- Emphasize progressive disclosure and reliable, auditable changes driven by tool calls.

Progressive disclosure units
- Local context view: current node, its parent, and its direct children to preserve immediate meaning and decomposition.
- Full branch view: a selected node with all descendants to validate completeness within a topic.
- Breadcrumb path: the chain from the root to the current node to preserve global context.

Node data layers to include
- Textual content: text, details, and note.
- Attributes: list of name and value pairs, allowing repeated names.
- Tags: a list of tag names assigned to the node.

Map identifier policy
- Use a session scoped map identifier for all tool calls.
- Map identifiers are opaque strings that are valid only while the map is open.
- When a map is reopened, it receives a new map identifier.
- Tool requests default to the current map when map_identifier is empty.

Interaction protocol
- Summarize and verify: return a brief summary of the provided branch and confirm understanding before proposing changes.
- Suggestion first: share proposed changes in chat before any write action.
- Consistency check: compare two provided branches and flag conflicts or duplicated meaning.

Tooling specification
Read tools
- read_node_context
  - Purpose: return a node with surrounding context and optional layers.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
    - node_identifier: identifier of the target node; default to the current selection when empty.
    - depth: how many descendant levels to include.
    - include_notes: include note content.
    - include_details: include node details.
    - include_attributes: include attributes.
    - include_ancestors: include the breadcrumb path to the root.
    - include_node_identifiers: include node identifiers in output.
    - max_nodes: cap the number of nodes returned to avoid large responses.
    - output_format: structured text format such as markdown or a structured data object.
- get_breadcrumbs
  - Purpose: return the path from the root to a node.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
    - node_identifier.
    - include_node_identifiers: include node identifiers in the breadcrumb list.
- search_map
  - Purpose: find nodes by text across the map or all open maps.
  - Parameters:
    - map_identifier: optional, defaults to all open maps when empty.
    - query_text.
    - scope_node_identifier: optional root for scoped search.
    - include_notes.
    - include_details.
    - include_attributes.
    - match_mode: exact, contains, regex.
    - case_sensitive.
    - max_results.
- get_flat_list
  - Purpose: return a flat list of nodes under a selected branch.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
    - node_identifier.
    - recursive: include all descendants when true.
    - include_notes.
    - include_details.
    - include_attributes.
    - include_breadcrumbs.
    - max_nodes.
  - Output:
    - items: list of { node_identifier, content, breadcrumbs } where content includes textualContent, attributesContent, tagsContent.
- list_search_properties
  - Purpose: list properties that can be used for both search and filter conditions, excluding attribute conditions.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
  - Output:
    - properties: list of { name } using English property names accepted by the tools.
- list_search_conditions_for_property
  - Purpose: list valid conditions and value input modes for a property.
  - Parameters:
    - property_name: required.
  - Output:
    - conditions: list of { name, value_input_mode, case_sensitive_option_allowed, approximate_matching_option_allowed, ignore_diacritics_option_allowed }.
  - Reject any property_name or condition_name that is not returned by the discovery tools.
- search_nodes_by_condition
  - Purpose: search nodes using the same condition model that powers filtering.
  - Parameters:
    - map_identifier: required.
    - condition: { property_name, condition_name, value, case_sensitive, approximate_matching, ignore_diacritics }.
    - scope_node_identifier: optional root for scoped search.
    - maximum_results: optional cap.
  - Output:
    - node_identifiers: list of matching node identifiers.
- list_attribute_names_for_map
  - Purpose: list available attribute names for a map.
  - Parameters:
    - map_identifier: required.
  - Output:
    - attribute_names: list of strings.
- search_attributes_by_name_and_value
  - Purpose: search nodes by attribute name and value.
  - Parameters:
    - map_identifier: required.
    - attribute_name: required.
    - attribute_value: required.
  - Output:
    - node_identifiers: list of matching node identifiers.
- generate_search_overview
  - Purpose: generate a compact map overview and index for targeted search.
  - Parameters:
    - map_identifier: required.
    - focus_request: optional; if provided, prioritize terms and sections relevant to the request.
    - model_identifier: optional; selects a cheaper or faster model for overview generation.
    - maximum_keyword_count: optional upper bound for keyword entries.
    - maximum_section_count: optional upper bound for section entries.
  - Output:
    - summary: short abstract of what the map is about.
    - themes: list of high level topics.
    - sections: list of { node_identifier, node_text, keywords }.
    - keywords: list of { term, node_identifiers }.

AI only filter tools
- set_ai_only_filter_condition
  - Purpose: set a filter condition that affects only AI tool calls and not the user view.
  - Parameters:
    - map_identifier: required.
    - condition: { property_name, condition_name, value, case_sensitive, approximate_matching, ignore_diacritics }.
  - Output:
    - active_condition: the active ai only filter condition.
- get_ai_only_filter_condition
  - Purpose: retrieve the active ai only filter condition for transparency.
  - Parameters:
    - map_identifier: required.
  - Output:
    - active_condition: condition object or null.
- clear_ai_only_filter_condition
  - Purpose: clear the active ai only filter condition.
  - Parameters:
    - map_identifier: required.
  - Output:
    - cleared: boolean confirmation.

Action tools
- create_nodes
  - Purpose: add new nodes under a target node, including full subtrees.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
    - target_parent_identifier.
    - insert_position: BEFORE_NODE, AFTER_NODE, BEFORE_FIRST_CHILD, AFTER_LAST_CHILD.
    - reference_node_identifier: required when insert_position is BEFORE_NODE or AFTER_NODE.
    - nodes: list of node definitions to create, each containing content and recursive children.
  - Expected output: a structured response with created node identifiers in the order they were created.
- apply_attributes
  - Purpose: apply attributes to selected nodes based on analysis or rules.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
    - schema: list of allowed attribute names.
    - updates: list of node identifiers with attribute entries.
    - merge_mode: replace or merge.
    - remove_missing: remove attributes not present in the update when true.
- move_nodes
  - Purpose: move nodes into a target parent for restructuring and cleanup.
  - Parameters:
    - map_identifier: optional, defaults to the current map.
    - node_identifiers: list of nodes to move.
    - target_parent_identifier.
    - insert_position: BEFORE_NODE, AFTER_NODE, BEFORE_FIRST_CHILD, AFTER_LAST_CHILD.
    - reference_node_identifier: required when insert_position is BEFORE_NODE or AFTER_NODE.
    - preserve_order: keep current order when true.

Chat workflow
- Chat should use read tools to gather context, then answer in the chat pane without modifying the map.
- Suggested changes are reviewed in chat before any tool writes are executed.
- When map scope is unclear, call generate_search_overview with the current user request to build a targeted index.
- Use list_search_properties and list_search_conditions_for_property to build valid condition requests before calling search_nodes_by_condition.
- Apply ai only filter conditions to narrow follow up reads without changing the user visible filter.
- Support iterative structural extraction in chat by allowing branch scoped rewrites after user feedback.

Minimum viable product scope and phased work
- The minimum viable product focuses on chat panel workflows only; inline edit and side panel variations are later.
- Accept or reject changes as a batch and fine grained undo are important but deferred to tool implementation.
- The map may change during a session, and the LLM should assume its view can be stale unless it refreshes context.
- Do not stream every map change to the LLM; rely on explicit read tools when needed.

Deferred ideas (not considered now)
- Inline proofreading with color coded suggestions and accept or reject controls.
- Model provider expansion beyond the initial set, including local models and additional cloud gateways.
- Multimodal context ingestion for files like PDF, DOCX, MP3, or MP4.
- Custom context selection via Groovy scripts and user defined toolbox actions.
- Asynchronous calls and multiple simultaneous conversations.
- Dedicated validation user interface for proposed edits beyond the current chat flow.

Response formats and identifiers
- Tool responses should be strict and machine readable to allow reliable parsing and changes.
- Every change should reference a stable node identifier so the editor applies changes to the correct node.
- Prefer explicit confirmation steps for destructive or large changes.
- All requests and responses include map_identifier.

create_nodes response format
- Response example:
  - {
      "action": "create_nodes",
      "map_identifier": "MAP_IDENTIFIER",
      "target_parent_identifier": "NODE_IDENTIFIER_PARENT",
      "insert_position": "AFTER_LAST_CHILD",
      "reference_node_identifier": "NODE_IDENTIFIER_SIBLING",
      "nodes": [
        {
          "content": {
            "textualContent": {
              "text": "Parent node title",
              "details": "Optional detail",
              "note": "Optional long note"
            },
            "attributesContent": {
              "attributes": [
                {
                  "name": "Status",
                  "value": "Pending"
                }
              ]
            },
            "tagsContent": {
              "tags": [
                "Planning"
              ]
            }
          },
          "children": [
            {
              "content": {
                "textualContent": {
                  "text": "Child node title",
                  "note": "Optional note"
                }
              },
              "children": [
                {
                  "content": {
                    "textualContent": {
                      "text": "Grandchild node title"
                    }
                  }
                }
              ]
            }
          ]
        }
      ]
    }
- nodes supports recursive children to allow full subtree creation.
- insert_position supports BEFORE_NODE, AFTER_NODE, BEFORE_FIRST_CHILD, or AFTER_LAST_CHILD.

Change management and review
- AI edited nodes should be tagged with a temporary state icon for easy review.
- Provide editor actions to keep all AI edits or undo all AI edits as a single step.

langchain4j integration sketch
- Define each tool as a callable function with a schema that mirrors the parameters above.
- Convert Freeplane data structures into structured tool output for the model.
- Parse the model response into deterministic tool actions and then apply changes through Freeplane model operations.
- Keep the interaction loop explicit: collect context, call the model, validate the structured response, then apply changes.

Short note on retrieval and indexing
- If retrieval is added later, treat it as a separate phase with careful unit design so that small nodes are not stripped of context.

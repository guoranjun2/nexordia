Goal and scope
- Enable a large language model to understand and improve large Freeplane mind maps without reading the entire map at once.
- Emphasize progressive disclosure and reliable, auditable changes driven by tool calls.

Progressive disclosure units
- Local context view: current node, its parent, and its direct children to preserve immediate meaning and decomposition.
- Full branch view: a selected node with all descendants to validate completeness within a topic.
- Breadcrumb path: the chain from the root to the current node to preserve global context.

Node data layers to include
- Textual content: text, details, and note, returned as plain text for reading.
- Attributes: list of name and value pairs, allowing repeated names.
- Tags: a list of tag names assigned to the node.

Map identifier policy
- Use a session scoped mapIdentifier for all tool calls.
- Map identifiers are opaque strings that are valid only while the map is open.
- When a map is reopened, it receives a new mapIdentifier.
- Use getSelectedMapAndNodeIdentifiers to discover the current map identifier, selected node identifier, and root node identifier.

Interaction protocol
- Summarize and verify: return a brief summary of the provided branch and confirm understanding before proposing changes.
- Suggestion first: share proposed changes in chat before any write action.
- Consistency check: compare two provided branches and flag conflicts or duplicated meaning.

Tooling specification
Read tools
- readNodeWithContext
  - Purpose: return nodes with surrounding context and optional layers.
  - Parameters:
    - mapIdentifier: required.
    - nodeIdentifiers: list of node identifiers; default to the root node when empty.
    - contextSections: list of BREADCRUMB_PATH, PARENT_SUMMARY, QUALIFIERS.
    - fullContentDepth: depth of full content.
    - summaryDepth: depth of brief summaries beyond fullContentDepth.
    - maximumTotalTextCharacters: total response budget.
    - focusNodeContentRequest: optional override for focus nodes.
    - parentNodeContentRequest: optional override for parent summary nodes.
    - childNodeContentRequest: optional override for child nodes.
  - Output:
    - mapIdentifier, items, and omissions for omitted focus nodes.
- getSelectedMapAndNodeIdentifiers
  - Purpose: return identifiers for the current map, selected node, and root node.
Search tools
- searchNodes
  - Purpose: find nodes by content within a map or subtree.
  - Parameters:
    - mapIdentifier: required.
    - queryText: required.
    - subtreeRootNodeIdentifiers: optional scope roots.
    - nodeContentRequestForSearch: selects which content fields are searched.
    - matchingMode: CONTAINS, EQUALS, REGULAR_EXPRESSION.
    - caseSensitivity: CASE_INSENSITIVE, CASE_SENSITIVE.
    - resultSections: BREADCRUMB_PATH.
    - offset, limit.
    - maximumTotalTextCharacters: total response budget.
  - Output:
    - mapIdentifier, results, and omissions.
Overview tools
- generateSearchOverview
  - Purpose: generate a compact map overview and index for targeted search.
  - Parameters:
    - mapIdentifier: required.
    - focusRequest: optional; if provided, prioritize terms and sections relevant to the request.
    - modelIdentifier: optional; selects a cheaper or faster model for overview generation.
    - maximumKeywordCount: optional upper bound for keyword entries.
    - maximumSectionCount: optional upper bound for section entries.
  - Output:
    - summary: short abstract of what the map is about.
    - themes: list of high level topics.
    - sections: list of { nodeIdentifier, nodeText, keywords }.
    - keywords: list of { term, nodeIdentifiers }.

Attribute tools
- listAttributeNamesForMap
  - Purpose: list available attribute names for a map.
  - Parameters:
    - mapIdentifier: required.
  - Output:
    - attributeNames: list of strings.
- searchAttributesByNameAndValue
  - Purpose: search nodes by attribute name and value.
  - Parameters:
    - mapIdentifier: required.
    - attributeName: required.
    - attributeValue: required.
  - Output:
    - nodeIdentifiers: list of matching node identifiers.

Action tools
- createNodes
  - Purpose: add new nodes under a target node, including full subtrees.
  - Parameters:
    - mapIdentifier: optional, defaults to the current map.
    - targetParentIdentifier.
    - insertPosition: BEFORE_NODE, AFTER_NODE, BEFORE_FIRST_CHILD, AFTER_LAST_CHILD.
    - referenceNodeIdentifier: required when insertPosition is BEFORE_NODE or AFTER_NODE.
    - nodes: list of node definitions to create, each containing content and recursive children.
  - Expected output: a structured response with created node identifiers in the order they were created.
- applyAttributes
  - Purpose: apply attributes to selected nodes based on analysis or rules.
  - Parameters:
    - mapIdentifier: optional, defaults to the current map.
    - schema: list of allowed attribute names.
    - updates: list of node identifiers with attribute entries.
    - mergeMode: replace or merge.
    - removesMissing: remove attributes not present in the update when true.
- moveNodes
  - Purpose: move nodes into a target parent for restructuring and cleanup.
  - Parameters:
    - mapIdentifier: optional, defaults to the current map.
    - nodeIdentifiers: list of nodes to move.
    - targetParentIdentifier.
    - insertPosition: BEFORE_NODE, AFTER_NODE, BEFORE_FIRST_CHILD, AFTER_LAST_CHILD.
    - referenceNodeIdentifier: required when insertPosition is BEFORE_NODE or AFTER_NODE.
    - preservesOrder: keep current order when true.

Chat workflow
- Chat should use read tools to gather context, then answer in the chat pane without modifying the map.
- Suggested changes are reviewed in chat before any tool writes are executed.
- When map scope is unclear, call generateSearchOverview with the current user request to build a targeted index.
- Use searchNodes to locate relevant nodes, then readNodeWithContext for focused context before proposing edits.
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
- All requests and responses include mapIdentifier.

createNodes response format
- Response example:
  - {
      "action": "createNodes",
      "mapIdentifier": "MAP_IDENTIFIER",
      "targetParentIdentifier": "NODE_IDENTIFIER_PARENT",
      "insertPosition": "AFTER_LAST_CHILD",
      "referenceNodeIdentifier": "NODE_IDENTIFIER_SIBLING",
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
- insertPosition supports BEFORE_NODE, AFTER_NODE, BEFORE_FIRST_CHILD, or AFTER_LAST_CHILD.

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

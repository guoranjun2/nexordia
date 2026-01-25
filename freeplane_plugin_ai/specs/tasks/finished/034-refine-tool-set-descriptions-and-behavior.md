# Task: Refine tool set descriptions and behavior
- **Task Identifier:** 2026-01-25-tool-set-refinement
- **Scope:** Shorten MCP tool and schema descriptions while preserving critical constraints; document updated guidance for selection IDs and map lifecycle.
- **Motivation:** Current tool descriptions are verbose and repeat boilerplate; we need a concise, reliable format without a global system message.
- **Developer Briefing:** Tool descriptions live in @Description annotations across tool request/response models. We are shortening these strings to one clear sentence, removing repeated boilerplate, and marking optional fields explicitly. We also adjusted edit semantics to allow DELETE for details/notes by mapping to empty strings, matching Freeplane’s controller behavior. MCP has no global header, so critical rules remain local and concise.
- **Research:**
    - Tool and field descriptions are defined via @Description annotations in request/response classes under `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/**` (e.g., `ReadNodesWithDescendantsRequest`, `FetchNodesForEditingRequest`, `NodeContentEditItem`, `NodeContentWriteRequest`, selection request/response models).
    - Many fields repeat the same boilerplate, especially the phrase “Use another tool call to refresh identifiers if needed.” on map and node identifier fields.
    - Several descriptions include long procedural guidance (for example, `NodeContentEditItem.value` and the `NodeContentWriteRequest` content type fields), which expands MCP tool schemas significantly.
    - Current wording implies identifiers broadly change; the accurate constraint is that selection identifiers can change with user interaction and that operations require an open map.
    - MCP tool schemas are delivered per-tool without a global system message, so any critical operational constraints must be expressed succinctly in each relevant description or enforced by schema constraints.
    - Freeplane controllers treat empty details/note text as removal (MTextController/MNoteController), enabling DELETE semantics by mapping to empty string.
- **Design:**
    - Apply a “single‑sentence” rule: each description is one short sentence (target ~8–14 words), focusing on constraints or defaults, not restating the tool name.
    - Replace boilerplate about identifier refresh with concise, scoped guidance (e.g., “Target map ID” and short selection references) without repeating “refresh” hints.
    - Shorten content type guidance to compact defaults: “Format: PLAIN_TEXT, MARKDOWN, HTML, LATEX, FORMULA. Default: PLAIN_TEXT.” (only where a content type is optional).
    - For prerequisite flows, use compact dependency notes (e.g., “Requires fetchNodesForEditing state.”) instead of multi‑sentence rules.
    - Prefer schema constraints (enums, required fields, min/max) over prose wherever possible; do not duplicate constraints in long text.
    - Standardize wording across repeated objects (e.g., content, summary anchors) with consistent phrasing rather than repeated full explanations.
    - Mark optional fields explicitly in descriptions when the schema allows omission (e.g., NodeContentWriteRequest fields beyond `text`).
    - Edit tool: document valid operations per editedElement; allow DELETE for DETAILS/NOTE by mapping to empty string.
- **Test specification:**
    - Add or update a schema-focused test that inspects a small set of generated tool descriptions (e.g., selection identifiers, edit items, content write requests) to ensure the shortened wording and the “selection IDs may change / map must be open” guidance are present.
    - Update any existing snapshot/schema expectations if they change due to shortened descriptions.
    - Add tests to verify DELETE for DETAILS/NOTE maps to clearing content.

## Subtask: Shorten and clarify tool descriptions
- **Status:** Finished
- **Scope:** Reduce @Description verbosity while preserving constraints; align wording across map/selection/node IDs.
- **Motivation:** Make MCP tool schemas smaller without losing clarity.
- **Developer Briefing:** Focus on @Description strings in request/response classes; keep each as one short sentence.
- **Research:** See main task Research; @Description annotations are the single source of schema text.
- **Design:** Apply one-sentence descriptions, remove boilerplate, and add concise cross-references only where needed.
- **Test specification:** Update or add schema-focused tests if tool schema snapshots exist.

## Subtask: Optional fields and edit DELETE semantics
- **Status:** Finished
- **Scope:** Make NodeContentWriteRequest fields optional except text; allow DELETE for DETAILS/NOTE; document valid operations.
- **Motivation:** Reduce boilerplate payloads and remove ambiguity in edit operations.
- **Developer Briefing:** Use @JsonProperty(required = false) for optional fields; map DELETE for DETAILS/NOTE to empty string to trigger controller removal behavior.
- **Research:** MTextController/MNoteController treat empty string as removal; NodeContentEditor currently enforces REPLACE.
- **Design:** Relax schema requiredness for content fields, add “Optional” descriptions, and allow DELETE for DETAILS/NOTE.
- **Test specification:** Add unit tests that verify DELETE for DETAILS/NOTE results in empty string passed to editors.

## Subtask: Control folding for newly created non-leaf nodes
- **Status:** Finished
- **Scope:** Add an optional folding flag to NodeCreationItem, defaulting to UNFOLD; apply it to newly created non-leaf nodes and ignore it for leaves.
- **Motivation:** Allow createNodes to control whether new parent nodes start folded or unfolded.
- **Developer Briefing:** NodeCreationItem gains an optional enum field (e.g., NodeFoldingState). After the node subtree is created, apply folding to nodes that have children created in the same request; leaf nodes ignore the flag.
- **Research:** CreateNodes uses NodeCreationItem/NodeCreationHierarchyBuilder to construct subtrees; folding can be applied directly to the new NodeModel instances after creation.
- **Design:** Add NodeCreationItem.foldingState (optional, default UNFOLD). Determine non-leaves from the creation list (nodes referenced as parents). Apply folding to those new nodes only, after creation. Unfold the existing target parent if it is folded and `ai_unfolds_parents_on_create` is true.
- **Design:** Add a localized AI preference for `ai_unfolds_parents_on_create` in the AI preferences UI.
- **Design:**
    ```plantuml
    @startuml
    actor ToolCaller
    participant CreateNodesTool
    participant NodeCreationHierarchyBuilder
    participant MapController
    participant Preferences

    ToolCaller -> CreateNodesTool : createNodes(request)
    CreateNodesTool -> NodeCreationHierarchyBuilder : buildNodes(items)
    NodeCreationHierarchyBuilder --> CreateNodesTool : created NodeModels
    CreateNodesTool -> Preferences : read ai_unfolds_parents_on_create
    CreateNodesTool -> MapController : insert new nodes
    CreateNodesTool -> MapController : apply foldingState to new non-leaf nodes
    CreateNodesTool -> MapController : unfold parent if folded\nand preference is true
    @enduml
    ```
- **Test specification:** Add a unit test that creates a parent+child in one request and asserts the parent folding state matches the flag; leaves are unchanged.

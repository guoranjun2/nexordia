# Task: Expose node links and clone metadata in tools
- **Task Identifier:** 2026-01-18-node-links-and-clone-metadata
- **Scope:** Extend the read tool to optionally return outgoing connectors, incoming connectors, node hyperlink, and clone metadata including all clone identifiers plus clone flags. Extend creation and editing tools to create, delete, and modify node hyperlinks. Add a dedicated connector edit tool to create, delete, and modify connectors.
- **Motivation:** Enable link and clone aware automation flows so the assistant can read and manipulate connectors, hyperlinks, and clone metadata without manual intervention.
- **Developer Briefing:** This work splits into three vertical slices: (1) read-time exposure of hyperlink/connectors/clone metadata in `NodeDepthItem` and `NodeContentItem`, (2) hyperlink creation and editing through existing create/edit tools using `MLinkController.setLink`, and (3) a new connector edit tool for add/update/delete with undo-aware operations and explicit ambiguity reporting. Connector creation is no longer part of node creation content and only existing node identifiers are supported for connector endpoints.
- **Research:** The read path currently emits `NodeDepthItem`/`NodeContentItem` without link/clone metadata, and existing edit/create payloads cover only textual content and collections. Link and connector data is stored in `NodeLinks`/`MapLinks` with label accessors on `ConnectorModel`, while clone metadata is exposed by `NodeModel.allClones()` plus clone flags.
- **Design:** Implement new read context sections for hyperlink, outgoing connectors, incoming connectors, and clone metadata. Add hyperlink creation/edit support to the existing create/edit tools. Introduce a new connector edit tool that accepts map id plus source/target node identifiers and returns `ignoredAmbiguousConnectorCount` when multiple connectors match and the first match is edited/deleted.
- **Test specification:** Add read tool tests for the new optional sections; add create/edit tool tests for hyperlink creation and updates; add connector edit tool tests for add/update/delete and ambiguity reporting.

## Subtask: Read node links and clone metadata
- **Status:** done
- **Scope:** Add read-time support for hyperlink, outgoing connectors, incoming connectors, and clone metadata when requested via new context sections, in both `NodeDepthItem` and `NodeContentItem`.
- **Motivation:** Link-aware reads are required for automation flows that depend on connector structure and clone lineage.
- **Developer Briefing:** Extend `ContextSection` and enrich read responses with hyperlink, connector lists, and clone metadata. Read hyperlink from `NodeLinks`, outgoing connectors from `NodeLinks.getLinks`, incoming connectors from `MapLinks.get(targetId)`, and clone metadata from `NodeModel`.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/read/ReadNodesWithDescendantsTool.java` builds `ReadNodesWithDescendantsItem` values with `NodeDepthItem` entries that currently carry node identifier, depth, unformatted text, and optional qualifiers.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/read/ContextSection.java` limits optional sections to breadcrumb path, parent summary, and qualifiers.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/content/NodeContentResponse.java` and `NodeContentItem` are used by read and edit flows, but do not include connectors, hyperlinks, or clone metadata.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/content/NodeContentItemReader.java` builds `NodeContentItem` with qualifiers and node identifiers.
  - `freeplane/src/main/java/org/freeplane/features/link/NodeLinks.java` stores node hyperlink and per node link models; `NodeLinks.getLinkAsString` and `NodeLinks.getValidLink` expose the current hyperlink.
  - `freeplane/src/main/java/org/freeplane/features/link/MapLinks.java` stores incoming link models by target identifier, which can be used to resolve incoming connectors for a node.
  - `freeplane/src/main/java/org/freeplane/features/link/ConnectorModel.java` exposes optional source, middle, and target labels via `getSourceLabel`, `getMiddleLabel`, and `getTargetLabel`.
  - `freeplane/src/main/java/org/freeplane/features/link/NodeConnectorChecker.java` shows how incoming connectors are resolved by reading `MapLinks.get(targetId)` and filtering to `ConnectorModel`.
  - `freeplane/src/main/java/org/freeplane/features/map/NodeModel.java` exposes clone metadata via `allClones()` and clone flags `isCloneTreeRoot()` and `isCloneTreeNode()`.
- **Design:**
  - Add new read sections in `ContextSection` for hyperlink, outgoing connectors, incoming connectors, and clone metadata; expose them only when requested.
  - Extend `NodeDepthItem` and `NodeContentItem` to carry the optional link/clone metadata when the corresponding sections are requested (for read responses) or after edits (for updated node snapshots).
  - Read-time metadata composition:
    - `hyperlink`: use `NodeLinks.getValidLink`/`NodeLinks.getLinkAsString` for the node; omit when absent.
    - `outgoingConnectors`: use `NodeLinks.getLinks(node)` and filter to `ConnectorModel`.
    - `incomingConnectors`: use `MapLinks.get(nodeId)` and filter to `ConnectorModel`.
    - `cloneNodeIdentifiers`: `NodeModel.allClones()` minus the current node identifier.
    - `isCloneTreeRoot`/`isCloneTreeNode`: from `NodeModel.isCloneTreeRoot()` / `NodeModel.isCloneTreeNode()`.
  - Proposed data model (read responses):
    ```plantuml
    @startuml
    enum ContextSection {
      BREADCRUMB_PATH
      PARENT_SUMMARY
      QUALIFIERS
      HYPERLINK
      OUTGOING_CONNECTORS
      INCOMING_CONNECTORS
      CLONE_METADATA
    }

    class ConnectorItem {
      String sourceNodeIdentifier
      String targetNodeIdentifier
      String sourceLabel
      String middleLabel
      String targetLabel
    }

    class CloneMetadata {
      List<String> cloneNodeIdentifiers
      boolean isCloneTreeRoot
      boolean isCloneTreeNode
    }

    class NodeDepthItem {
      String nodeIdentifier
      int depth
      String unformattedText
      List<String> qualifiers
      String hyperlink
      List<ConnectorItem> outgoingConnectors
      List<ConnectorItem> incomingConnectors
      CloneMetadata cloneMetadata
    }

    class NodeContentItem {
      String nodeIdentifier
      NodeContentResponse content
      List<String> qualifiers
      String hyperlink
      List<ConnectorItem> outgoingConnectors
      List<ConnectorItem> incomingConnectors
      CloneMetadata cloneMetadata
    }

    NodeDepthItem --> ConnectorItem
    NodeDepthItem --> CloneMetadata
    NodeContentItem --> ConnectorItem
    NodeContentItem --> CloneMetadata
    @enduml
    ```
- **Test specification:**
  - Add tests for read tool output with hyperlinks, incoming and outgoing connectors, and clone metadata (clone identifiers and clone flags) when the new sections are requested.

## Subtask: Hyperlink create and edit support
- **Status:** done
- **Scope:** Add hyperlink support to node creation content and to the edit tool via a dedicated `EditedElement.HYPERLINK` payload.
- **Motivation:** Hyperlinks are essential node metadata and must be creatable and editable through existing tools.
- **Developer Briefing:** Extend `NodeContentWriteRequest` to accept a hyperlink on create, and extend `NodeContentEditItem` to edit hyperlinks using a separate payload and `MLinkController.setLink` for undo-aware updates.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/content/NodeContentWriteRequest.java` supports text, attributes, tags, and icons for node creation only.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/edit/NodeContentEditItem.java` supports edits for text, details, note, attributes, tags, and icons only.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/edit/EditedElement.java` defines the edit element enum without hyperlink entries.
  - `freeplane/src/main/java/org/freeplane/features/link/mindmapmode/MLinkController.java` provides undo-aware hyperlink updates via `setLink`.
- **Design:**
  - Add `hyperlink` to `NodeContentWriteRequest` for create operations.
  - Add `EditedElement.HYPERLINK` and `hyperlink` to `NodeContentEditItem`. Use `operation` (`REPLACE` to set, `DELETE` to clear) and ignore `value`/`targetKey`/`index`.
  - Apply hyperlink edits via `MLinkController.setLink` to keep undo/redo behavior consistent.
  - Proposed data model (hyperlink create/edit payloads):
    ```plantuml
    @startuml
    class NodeContentWriteRequest {
      String text
      ContentType textContentType
      String details
      ContentType detailsContentType
      String note
      ContentType noteContentType
      List<AttributeEntry> attributes
      List<String> tags
      List<String> icons
      String hyperlink
    }

    enum EditedElement {
      TEXT
      DETAILS
      NOTE
      ATTRIBUTES
      TAGS
      ICONS
      HYPERLINK
    }

    enum EditOperation {
      REPLACE
      ADD
      DELETE
    }

    class NodeContentEditItem {
      String nodeIdentifier
      EditedElement editedElement
      ContentType originalContentType
      String value
      Integer index
      EditOperation operation
      String targetKey
      String hyperlink
    }
    @enduml
    ```
- **Test specification:**
  - Add edit tool tests for hyperlink updates (set and clear).
  - Add creation tool tests verifying hyperlinks are created when provided.

## Subtask: Connector edit tool
- **Status:** done
- **Scope:** Add a new connector edit tool with add/update/delete operations that accepts map id and source/target node identifiers, supports label edits, and reports ignored ambiguous matches. Centralize AI edit marker logic in a utility and apply it to connector source nodes for any connector CUD operation.
- **Motivation:** Connectors are distinct from textual content edits and require explicit operations with clear response semantics.
- **Developer Briefing:** Implement a new tool request/response for connector operations. Use `MLinkController` to add/remove and set labels, filter connectors by source/target (and optional label matchers), and when multiple matches exist edit/delete the first match while reporting `ignoredAmbiguousConnectorCount`. Move `addAiEditsMarkerWithUndo` into a shared utility and invoke it on the connector source node after any connector create/update/delete.
- **Research:**
  - `freeplane/src/main/java/org/freeplane/features/link/NodeLinks.java` stores per-node links and returns connector clones via `getLinks`.
  - `freeplane/src/main/java/org/freeplane/features/link/MapLinks.java` stores incoming link models by target identifier.
  - `freeplane/src/main/java/org/freeplane/features/link/ConnectorModel.java` exposes label getters/setters for source, middle, and target labels.
  - `freeplane/src/main/java/org/freeplane/features/link/mindmapmode/MLinkController.java` provides undo-aware connector creation (`addConnector`), deletion (`removeArrowLink`), and label edits (`setSourceLabel`, `setMiddleLabel`, `setTargetLabel`).
- **Design:**
  - New request payload contains `mapIdentifier` plus a list of `ConnectorEditRequestItem` entries.
  - Each `ConnectorEditRequestItem` includes `sourceNodeIdentifier`, `targetNodeIdentifier`, `operation` (`ADD`, `DELETE`, `REPLACE`), and optional `sourceLabel`, `middleLabel`, `targetLabel`.
  - Use `matchSourceLabel`, `matchMiddleLabel`, `matchTargetLabel` to disambiguate existing connectors.
  - Only existing `nodeIdentifier` values are supported; connector endpoints must refer to existing nodes.
  - For edits/deletes, filter connectors from `NodeLinks.getLinks(source)` to the `ConnectorModel` instances targeting `targetNodeIdentifier`, then apply match label filters. If multiple remain, apply the change to the first match (in iteration order) and report the count of ignored matches via `ignoredAmbiguousConnectorCount`.
  - Extract `addAiEditsMarkerWithUndo` from `NodeContentEditor` into a shared utility class (in the same package or a new `tools.utilities` class), and call it after connector add, update, or delete operations against the connector source node.
  - Proposed data model (connector edit tool):
    ```plantuml
    @startuml
    enum EditOperation {
      REPLACE
      ADD
      DELETE
    }

    class ConnectorEditRequestItem {
      String sourceNodeIdentifier
      String targetNodeIdentifier
      EditOperation operation
      String sourceLabel
      String middleLabel
      String targetLabel
      String matchSourceLabel
      String matchMiddleLabel
      String matchTargetLabel
    }

    class ConnectorEditRequest {
      String mapIdentifier
      List<ConnectorEditRequestItem> items
    }

    class ConnectorEditResultItem {
      String sourceNodeIdentifier
      String targetNodeIdentifier
      String action
      int ignoredAmbiguousConnectorCount
      ConnectorItem connector
    }

    class ConnectorEditResponse {
      String mapIdentifier
      List<ConnectorEditResultItem> items
    }

    ConnectorEditResponse --> ConnectorEditResultItem
    ConnectorEditResultItem --> ConnectorItem
    ConnectorEditRequest --> ConnectorEditRequestItem
    @enduml
    ```
- **Test specification:**
  - Add connector edit tool tests for connector add, label update, and delete operations.
  - Add connector edit tool tests confirming `ignoredAmbiguousConnectorCount` when multiple matches exist and only the first is edited/deleted.
  - Add tests covering that connector CUD operations add the AI edits marker to the source node (including undo-aware behavior).
  - Implemented: connector CUD tests now assert the AI edits marker is applied to the source node.

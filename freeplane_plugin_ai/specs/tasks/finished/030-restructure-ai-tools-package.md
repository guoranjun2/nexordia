# Task: Restructure ai tools package with vertical slices
- **Scope:** Split `org.freeplane.plugin.ai.tools` into vertical slice subpackages so each tool lives with its request/response, helpers, and tests.
- **Motivation:** The current tools package is large and hard to navigate; vertical slices improve discoverability and local reasoning.
- **Research:**
  - The tools package currently contains tool wiring, request/response DTOs, helper builders, and tests in a single namespace.
- **Design:**
  - Create per-tool packages (selection, delete, create, move, read, search, edit) and move each tool with its related request/response and helpers into the same package.
  - Keep a small core package for wiring (`AIToolSet`, `AIToolSetBuilder`, tool call summary utilities).
  - Update imports and any package-private visibility as needed.
  - Move tests into corresponding subpackages to match production layout.
  - Provide a minimal move plan that prioritizes one slice at a time to reduce review risk.
  - Proposed package layout:
    ```plantuml
    @startuml
    skinparam packageStyle rectangle
    note as N1
      Visibility legend:
      + public class
      ~ package-private class
    end note
    package "org.freeplane.plugin.ai.tools" {
      class "+AIToolSet"
      class "+AIToolSetBuilder"
      class "+ToolCaller"
      class "+ToolCallSummary"
      class "+ToolCallSummaryHandler"
      class "~ToolCallSummaryFormatter"
      class "+ToolErrorHandler"
      class "+ToolExecutorFactory"
      class "+ToolExecutorRegistry"
      class "+EventDispatchToolExecutor"
    }
    package "org.freeplane.plugin.ai.tools.selection" {
      class "+SelectedMapAndNodeIdentifiersTool"
      class "+SelectionIdentifiersBuilder"
      class "+SelectionIdentifiersRequest"
      class "+SelectionIdentifiersResponse"
      class "+SelectionCollectionMode"
      class "+SelectedNodeSummary"
      class "+SelectSingleNodeTool"
      class "+SelectSingleNodeRequest"
    }
    package "org.freeplane.plugin.ai.tools.delete" {
      class "+DeleteNodesTool"
      class "+DeleteNodesRequest"
      class "+DeleteNodesResponse"
    }
    package "org.freeplane.plugin.ai.tools.create" {
      class "+CreateNodesTool"
      class "+CreateNodesRequest"
      class "+CreateNodesResponse"
      class "+NodeCreationHierarchyBuilder"
      class "+NodeCreationItem"
      class "+NodeCreationHierarchy"
      class "+NodeModelCreator"
      class "+NodeInserter"
      class "+AnchorPlacement"
      class "+AnchorPlacementMode"
      class "+AnchorPlacementResult"
      class "+AnchorPlacementCalculator"
      class "+InsertPosition"
    }
    package "org.freeplane.plugin.ai.tools.move" {
      class "+MoveNodesTool"
      class "+MoveNodesRequest"
      class "+MoveNodesResponse"
      class "+MoveNodesIntoSummaryTool"
      class "+MoveNodesIntoSummaryRequest"
      class "+MoveNodesIntoSummaryResponse"
      class "+SummaryAnchorPlacement"
      class "+SummaryNodeCreator"
    }
    package "org.freeplane.plugin.ai.tools.read" {
      class "+ReadNodesWithDescendantsTool"
      class "+ReadNodesWithDescendantsRequest"
      class "+ReadNodesWithDescendantsResponse"
      class "+ReadNodesWithDescendantsItem"
      class "+FetchNodesForEditingRequest"
      class "+FetchNodesForEditingResponse"
      class "+BreadcrumbsTool"
      class "+BreadcrumbsRequest"
      class "+BreadcrumbsResponse"
      class "+BreadcrumbItem"
      class "+NodeDepthItem"
      class "+ContextSection"
    }
    package "org.freeplane.plugin.ai.tools.search" {
      class "+SearchNodesTool"
      class "+SearchNodesRequest"
      class "+SearchNodesResponse"
      class "+SearchResult"
      class "+SearchResultItem"
      class "+SearchResultSection"
      class "+SearchCaseSensitivity"
      class "+SearchMatchingMode"
      class "+SearchConditionRequest"
      class "+SearchConditionDefinition"
      class "+SearchConditionState"
      class "+SearchOverviewRequest"
      class "+SearchOverviewResponse"
      class "+SearchOverviewSection"
      class "+SearchOverviewKeyword"
      class "+Omissions"
      class "+OmissionReason"
    }
    package "org.freeplane.plugin.ai.tools.edit" {
      class "+EditRequest"
      class "+NodeContentEditItem"
      class "+NodeContentEditor"
      class "+EditOperation"
      class "+EditedElement"
      class "+TextualContentEditor"
      class "+AttributesContentEditor"
      class "+TagsContentEditor"
      class "+IconsContentEditor"
      class "+TextContentWriteController"
      class "+TextContentWriteControllerAdapter"
      class "+NoteContentWriteController"
      class "+NoteContentWriteControllerAdapter"
      class "+ApplyAttributesRequest"
      class "+ApplyAttributesResponse"
    }
    package "org.freeplane.plugin.ai.tools.content" {
      class "+NodeContentApplier"
      class "~NodeContentFactories"
      class "+NodeContentItem"
      class "+NodeContentItemReader"
      class "+NodeContentPreset"
      class "+NodeContentReader"
      class "+NodeContentRequest"
      class "+NodeContentResponse"
      class "+NodeContentValueMatcher"
      class "+NodeContentWriteRequest"
      class "+ContentType"
      class "+ContentTypeConverter"
      class "+TextualContent"
      class "+TextualContentRequest"
      class "+TextualContentReader"
      class "+AttributesContent"
      class "+AttributesContentRequest"
      class "+AttributesContentReader"
      class "+TagsContent"
      class "+TagsContentRequest"
      class "+TagsContentReader"
      class "+IconsContent"
      class "+IconsContentRequest"
      class "+IconsContentReader"
      class "+AttributeEntry"
      class "+AttributeUpdate"
      class "+EditableContent"
      class "+EditableContentField"
      class "+EditableContentRequest"
      class "+EditableContentReader"
      class "+EditableAttribute"
      class "+EditableTag"
      class "+EditableIcon"
      class "+EditableText"
      class "+ModifiedNodeSummary"
      class "+ModifiedNodeSummaryBuilder"
      class "~IconDescriptionResolver"
      class "+ListAvailableIconsTool"
      class "+ListAvailableIconsResponse"
    }
    package "org.freeplane.plugin.ai.tools.text" {
      class "+EnglishTextProvider"
      class "+DefaultEnglishTextProvider"
    }
    @enduml
    ```
- **Test specification:**
  - Run the module tests after each slice move to catch import or visibility regressions.

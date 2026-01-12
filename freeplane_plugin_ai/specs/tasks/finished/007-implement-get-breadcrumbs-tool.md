# Task: Implement get_breadcrumbs tool
- **Scope:** Implement get_breadcrumbs to return the root to node path, skipping hidden summary nodes and optionally including node identifiers.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/BreadcrumbsTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentItemReader.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/BreadcrumbsToolTest.java
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/NodeContentItemReaderTest.java
- **Research:**
```plantuml
@startuml
class BreadcrumbLayout
class MapTreeNode
class SummaryNode
class NodeModel
class TextController

BreadcrumbLayout --> SummaryNode
MapTreeNode --> TextController
SummaryNode --> NodeModel

note right of MapTreeNode
Uses TextController.getShortPlainText for outline text.
end note

note right of BreadcrumbLayout
Skips SummaryNode.isHidden nodes when bridging
missing parents in breadcrumbs.
end note

note bottom
NodeModel.getPathToRoot includes parents regardless of
hidden summary nodes. SummaryNode.isHidden is derived
from summary or first group flags plus empty text.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class AIToolSet
class BreadcrumbsTool
class NodeContentItemReader
class BreadcrumbItem
class BreadcrumbsRequest
class BreadcrumbsResponse
class SummaryNode
class NodeModel

AIToolSet --> BreadcrumbsTool
BreadcrumbsTool --> NodeContentItemReader
BreadcrumbsTool --> BreadcrumbsRequest
BreadcrumbsTool --> BreadcrumbsResponse
NodeContentItemReader --> NodeModel
SummaryNode --> NodeModel

note right of AIToolSet
Delegates to BreadcrumbsTool.
end note

note right of BreadcrumbsTool
Resolve map and node identifiers.
Walk parent chain to root.
Skip SummaryNode.isHidden nodes.
Use NodeContentItemReader with BRIEF
to read breadcrumb text.
Include node identifiers only when requested.
end note

note bottom
Breadcrumb text uses short plain text to match
outline behavior.
end note
@enduml
```
- **Test specification:**
  - Verify breadcrumbs include root to target nodes in order.
  - Verify SummaryNode.isHidden nodes are skipped.
  - Verify node identifiers are included only when requested.
  - Verify invalid map or node identifiers raise errors.

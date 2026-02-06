# Task: Node content qualifiers for summary nodes
- **Scope:** Add node qualifiers so AI can recognize summary and first group nodes without filtering them out; explain qualifiers in the system message.
- **Research:**
```plantuml
@startuml
class SummaryNode
class NodeModel
class NodeContentItem
class SystemMessageBuilder

SummaryNode --> NodeModel
SystemMessageBuilder --> NodeContentItem

note right of SummaryNode
SummaryNode.isSummaryNode and SummaryNode.isFirstGroupNode
identify structural nodes that may have empty text.
SummaryNode.isHidden is derived behavior for empty
summary or first group nodes.
end note

note bottom
Summary nodes are structural placeholders that
support navigation and grouping, including nesting.
They should not be filtered out for AI navigation.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class NodeContentItem {
nodeIdentifier
content
qualifiers : List<String>
}
class SummaryNode
class NodeModel

SummaryNode --> NodeModel
NodeContentItem --> NodeModel

note right of NodeContentItem
qualifiers include:
summary_node
first_group_node
end note

note bottom
Qualifiers are identity metadata on NodeContentItem.
No hidden qualifier is stored; hidden is derived.
System message explains the qualifiers and their meanings.
end note
@enduml
```
- **Test specification:**
  - Verify summary nodes include summary_node qualifier.
  - Verify first group nodes include first_group_node qualifier.
  - Verify non summary nodes have no qualifiers.
  - Verify system message includes qualifier descriptions.

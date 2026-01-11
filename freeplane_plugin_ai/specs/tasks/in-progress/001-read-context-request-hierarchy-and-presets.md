# Task: Read context request hierarchy and presets
- **Scope:** Define a new internal NodeContentRequest hierarchy with content presets for focus, parent, and child nodes.
- **Research summary:**
```plantuml
@startuml
note "NodeContentResponse groups TextualContent, AttributesContent, TagsContent.\nTextualContent includes text, details, note.\nAttributesContent stores AttributeEntry name and value pairs.\nTagsContent stores tag strings.\nPrevious NodeContextRequest included map identifier, node identifier, depth, include flags, output format.\nPrevious NodeContextResponse included map identifier, output format, payload.\nAIToolSet exposed readNodeContext with those types.\nLangChain4j JacksonJsonCodec serializes nulls; missing fields become null; unknown properties rejected unless custom factory.\nNodeContentResponse is now used by fetchNodesForEditing and edit responses, while information reads return concatenated unformattedText." as Research
@enduml
```
- **Design:**
```plantuml
@startuml
note "Define NodeContentRequest, TextualContentRequest, AttributesContentRequest, TagsContentRequest.\nDefine content presets full and brief.\nProvide preset resolver for focus, parent, child." as Design
@enduml
```
- **Test specification:**
  - Not applicable for structure-only change.

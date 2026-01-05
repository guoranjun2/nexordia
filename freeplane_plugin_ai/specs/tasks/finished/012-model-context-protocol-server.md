# Task: Model Context Protocol server
- **Scope:** Expose existing read tools through a Model Context Protocol server over an HTTP endpoint bound to the local interface, with startup controlled by preferences.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/Activator.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolServer.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolTool.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolToolDispatcher.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolToolRegistry.java
  - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties
  - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml
- **Research summary:**
```plantuml
@startuml
class AIToolSet
class ReadNodeWithContextTool
class AvailableMaps
class ModelContextProtocolServer
class ModelContextProtocolToolRegistry
class ModelContextProtocolToolDispatcher
class ToolSpecifications
class ToolSpecification
class DefaultToolExecutor
class HTTPServer
class ResourceController

AIToolSet --> ReadNodeWithContextTool
ReadNodeWithContextTool --> AvailableMaps
ModelContextProtocolServer --> ModelContextProtocolToolRegistry
ModelContextProtocolServer --> ModelContextProtocolToolDispatcher
ModelContextProtocolToolDispatcher --> AIToolSet
ModelContextProtocolToolRegistry --> ToolSpecifications
ModelContextProtocolToolRegistry --> ToolSpecification
ModelContextProtocolToolDispatcher --> DefaultToolExecutor
ModelContextProtocolServer --> HTTPServer
ModelContextProtocolServer --> ResourceController
@enduml
```
- **Design:**
```plantuml
@startuml
class ModelContextProtocolServer {
  +start()
  +stop()
}
class ModelContextProtocolToolRegistry {
  +registerToolsFromToolSet()
  +listTools()
}
class ModelContextProtocolToolDispatcher {
  +dispatch(toolName, arguments)
}
class ToolSpecification
class ToolExecutionRequest
class DefaultToolExecutor
class ToolInvocationResponse {
  +resultJson
  +error
}
class AIToolSet
class HTTPServer
class ResourceController

ModelContextProtocolServer --> ModelContextProtocolToolRegistry
ModelContextProtocolServer --> ModelContextProtocolToolDispatcher
ModelContextProtocolServer --> HTTPServer
ModelContextProtocolServer --> ResourceController
ModelContextProtocolToolRegistry --> ToolSpecification
ModelContextProtocolToolDispatcher --> ToolInvocationResponse
ModelContextProtocolToolDispatcher --> AIToolSet
ModelContextProtocolToolRegistry --> ToolSpecifications
ModelContextProtocolToolDispatcher --> DefaultToolExecutor
ModelContextProtocolToolDispatcher --> ToolExecutionRequest

note right of ModelContextProtocolToolRegistry
Build tool schemas from @Tool methods using
ToolSpecifications.toolSpecificationsFrom.
end note

note right of ModelContextProtocolToolDispatcher
Dispatch uses DefaultToolExecutor with ToolExecutionRequest
to reuse LangChain4j argument coercion and result
serialization. Tool parameter names must remain stable.
end note

note right of ModelContextProtocolServer
Uses io.fusionauth:java-http. Server binds to 127.0.0.1
and starts only when ai_mcp_server_enabled is true.
The port is read from ai_mcp_server_port.
end note
@enduml
```
- **Test specification:**
  - Verify tool registry returns schemas for read tools.
  - Verify dispatch routes a tool call to AIToolSet and returns JSON output.
  - Verify invalid tool name returns a protocol error response.

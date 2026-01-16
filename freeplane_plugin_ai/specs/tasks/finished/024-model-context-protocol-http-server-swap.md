# Task: Model Context Protocol HTTP server swap
- **Scope:** Replace the Model Context Protocol HTTP server implementation to use the JDK `com.sun.net.httpserver` APIs, remove the external HTTP server dependency, and align Eclipse Java Development Tools settings with the module language level.
- **Motivation:** Use the built-in HTTP server to reduce dependency footprint while keeping the same server behavior.
- **Research:**
```plantuml
@startuml
class ModelContextProtocolServer
class HTTPServer
class HTTPHandler
class HTTPRequest
class HTTPResponse
class ResourceController

ModelContextProtocolServer --> HTTPServer
ModelContextProtocolServer --> HTTPHandler
ModelContextProtocolServer --> HTTPRequest
ModelContextProtocolServer --> HTTPResponse
ModelContextProtocolServer --> ResourceController

note right of ModelContextProtocolServer
Current server uses io.fusionauth:java-http and binds to 127.0.0.1.
Pros of switching to com.sun.net.httpserver:
- Removes an external HTTP server dependency.
- Uses a JDK-provided API with no extra packaging.
- Simplifies dependency management for the plugin.
- Avoids virtual thread usage that conflicts with security manager permissions.
- Uses a JDK package that is considered public API.
Assumptions:
- The server remains localhost-only and is not intended for public exposure.
- Throughput demands are low, so a basic HTTP server is sufficient.
Cons of switching to com.sun.net.httpserver:
- API is basic and has fewer built-in conveniences.
- Throughput and feature set are limited compared to dedicated servers.
- Requires manual handling of request parsing and response headers.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class ModelContextProtocolServer {
  +start()
  +stop()
}
class HttpServer
class HttpHandler
class HttpExchange
class ResourceController

ModelContextProtocolServer --> HttpServer
ModelContextProtocolServer --> HttpHandler
ModelContextProtocolServer --> HttpExchange
ModelContextProtocolServer --> ResourceController

note right of ModelContextProtocolServer
Create a localhost-bound HttpServer context that accepts POST requests.
Return JSON responses with the same payloads and status codes as today.
Use 204 for notifications, 405 for non-POST requests, and 200 for JSON replies.
end note
@enduml
```
- **Test specification:**
  - Add an integration test that starts the MCP server on localhost and posts `initialize`, `tools/list`, and `resources/read` requests, asserting 200 responses with expected JSON keys.
  - Verify that a non-POST request returns status 405.
  - Verify that a notification request returns status 204 without a body.
- **Modified files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolServer.java
  - freeplane_plugin_ai/build.gradle
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolServerIntegrationTest.java

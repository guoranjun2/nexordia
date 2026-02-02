# Task: Add MCP tool calls to AI panel logging
- **Scope:** Ensure MCP tool invocations are reported inside the AI panel like the other tool calls, while distinguishing them visually and using the same non-null callback wiring we already rely on in the LangChain4j session.
- **Motivation:** AI operators expect every MCP tool call to appear in the panel for traceability; the current wiring only logs the LangChain4j tool list. Using a distinct style class keeps the panel layout consistent while signaling external tool calls.
- **Research:**
  - `AIToolSet` publishes summaries through the `ToolCallSummaryHandler` that the panel registers when building the tool set (`AIToolSetBuilder.toolCallSummaryHandler(...)` and `AIChatPanel.handleToolCallSummary` call out to the panel’s logging component).
  - Every tool currently instantiates a `ToolCallSummary` with `toolName` and `summaryText`, and `AIChatPanel` already displays those in the chat log.
  - The LangChain4j session always supplies a non-null handler/callback; MCP tool callers need the same guarantee so the panel sees their invocations as soon as they happen.
- **Design:**
  - Provide MCP callers with a non-null `ToolCallSummaryHandler` (like the LangChain4j session does) so their summary objects reach the same logging path used by `AIChatPanel`.
  - Reuse `AIToolSet.publishToolCallSummary(...)` so the AI panel doesn’t need separate logic for MCP flows; the non-null handler ensures MCP calls are logged even when nobody subscribed to the summary yet.
  - Introduce `ToolCaller` (`CHAT`, `MCP`) and let `AIToolSetBuilder` transmit the desired caller so the internally created handler can tag each `ToolCallSummary` before the panel renders it—that way the panel can pick distinct styling and prepend `MCP: ` for MCP calls based on `summary.toolCaller`.
  - Wire the MCP tool set in the activator: it creates the `AIChatPanel`, pulls its `ToolCallSummaryHandler`, and injects that handler into the MCP `AIToolSetBuilder` while keeping the server decoupled from the panel.
  ```plantuml
  @startuml
  class AIToolSetBuilder {
    +toolCaller(toolCaller: ToolCaller)
    +build(): AIToolSet
  }
  class ToolCallSummaryHandler {
    +handleToolCallSummary(summary: ToolCallSummary)
  }
  class AIChatPanel {
    +handleToolCallSummary(summary: ToolCallSummary)
  }
  class ToolCallSummary {
    -toolName: String
    -summaryText: String
    -toolCaller: ToolCaller
  }
  enum ToolCaller {
    CHAT
    MCP
  }
  AIToolSetBuilder --> ToolCallSummaryHandler
  ToolCallSummaryHandler --> AIChatPanel
  ToolCallSummaryHandler -> ToolCallSummary : sets toolCaller
  AIChatPanel -> ToolCallSummary : uses toolCaller to pick highlight
  @enduml
  ```
- **Test specification:**
  - Verify the panel’s log receives entries for MCP tool calls using the MCP-specific style class and `MCP: ` prefix.
  - Ensure the tooling callback is always non-null and MCP calls do not silently drop when no panel is attached.

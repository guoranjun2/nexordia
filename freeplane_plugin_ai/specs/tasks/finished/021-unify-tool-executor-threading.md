# Task: Unify tool executor threading for LC4J and MCP

## Scope
Create a shared tool executor layer that enforces event-dispatch thread execution and consistent exception wrapping for both LangChain4j and MCP tool calls.

## Motivation
Freeplane tool access must happen on the event-dispatch thread. The current LC4J execution path runs tools on background threads, while MCP has an explicit event-thread workaround. A unified executor prevents threading bugs and keeps error handling consistent without forking LangChain4j.

## Research
- `AIChatService` creates LC4J tool executors directly today, so there is no shared executor map across LC4J and MCP.
- `ModelContextProtocolToolDispatcher` already marshals tool execution onto the event-dispatch thread.
- Tool argument errors need to be reported back to the LLM, which currently differs between LC4J and MCP paths.
- Tool call summaries already marshal UI updates to the event-dispatch thread via `AIChatPanel.appendChatMessage`, and token usage updates are synchronized with UI updates dispatched via `invokeLater`.
- LangChain4j `ToolService` uses `DefaultToolExecutor.builder()` with `wrapToolArgumentsExceptions(true)` and `propagateToolExecutionExceptions(true)`, which should be preserved for LC4J but not for MCP so JSON-RPC error codes keep working.

## Design
- Introduce a shared executor factory that builds a tool name -> executor map.
- Wrap the default executor in an event-dispatch executor that calls `SwingUtilities.invokeAndWait` when needed.
- Preserve LangChain4j tool executor flags for LC4J (`wrapToolArgumentsExceptions=true`, `propagateToolExecutionExceptions=true`).
- Use different executor flags for MCP so argument parsing errors still propagate and map to JSON-RPC errors.
- Update LC4J and MCP wiring to use the shared executor map with their respective settings.

## Test specification
- Validate that LC4J tool calls run on the event-dispatch thread.
- Validate that MCP tool calls run on the event-dispatch thread.
- Validate that tool argument errors return the same message format in both paths.

## Modified files
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/EventDispatchToolExecutor.java`
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ToolExecutorFactory.java`
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ToolExecutorRegistry.java`
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatService.java`
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolToolDispatcher.java`
- Deleted: `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/ToolExecutorSettings.java`

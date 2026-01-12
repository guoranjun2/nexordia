# Task: Emit tool call summaries from service listeners

## Status
In Progress

## Scope
Emit tool call summaries for argument parsing failures by enhancing the LangChain4j tool arguments error handler and by adding an equivalent trace in the MCP server wrapper.

## Motivation
We only see tool call summaries after a tool finishes. When the model hallucinates icon names or fails tool argument parsing, no summary is shown. This hides the failed attempt and makes it look like nothing happened.

## Design
- Keep the existing tool-side summaries for successful calls.
- Extend `AIChatService` to inject a tool-call summary emitter into `TOOL_ARGUMENTS_ERROR_HANDLER`.
- When argument parsing fails, emit an "attempted tool call" summary using the tool name and error message available in the handler context.
- Add the same "attempted tool call" summary in the MCP server wrapper for MCP clients.
- In the MCP wrapper, emit a summary before tool dispatch that includes the tool name and raw arguments.
- If MCP argument parsing or tool lookup fails, emit an error summary that includes the tool name (if available) and the error message returned to the client.
- Keep the existing tool-side summaries for successful MCP calls so successful executions continue to include tool-specific details.
- Ensure MCP summaries are emitted inside the same `invokeAndWait` event loop used for tool execution so UI updates and tool traces stay ordered with the dispatch.
- The attempted summary should include tool name, raw arguments when available, and the error message if parsing fails.
- The handler and MCP wrapper should emit the attempted summary even when the tool is never invoked.

## Notes
- This is not about changing tool behavior, only tracing and visibility.
- This should help diagnose schema mismatches, missing icon names, and other pre-execution errors.

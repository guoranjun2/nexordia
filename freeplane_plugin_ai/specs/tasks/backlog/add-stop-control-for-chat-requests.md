# Task: Add stop control for chat requests
- **Task Identifier:** 2026-01-18-stop-chat-requests
- **Scope:** Provide a stop control in the AI chat panel so the same send button toggles to stop (icon or text) during an active request, and support canceling in flight chat responses and tool execution loops. Add an optional thinking display area that shows only the current request thinking output and clears when a new user message is sent.
- **Motivation:** Users need to interrupt long AI responses; the current flow blocks until completion.
- **Research:**
  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "SwingWorker" as Worker
  participant "AIChatService" as ChatService
  participant "AIAssistant" as Assistant
  participant "ChatModel" as ChatModel

  User -> ChatPanel: click Send
  ChatPanel -> ChatPanel: disable send button
  ChatPanel -> Worker: execute background task
  Worker -> ChatService: chat(message)
  ChatService -> Assistant: chat(message)
  Assistant -> ChatModel: request completion
  ChatModel --> Assistant: full response
  Assistant --> ChatService: full response
  ChatService --> Worker: full response
  Worker --> ChatPanel: publish final response
  ChatPanel -> ChatPanel: enable send button
  note right of ChatPanel
    No streaming or cancellation.
    Send button stays disabled
    until the response finishes.
  end note
  @enduml
  ```
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java` uses a `SwingWorker` to call `chatService.chat` and disables the send button until the worker completes.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatService.java` wraps a LangChain4j `ChatModel` and exposes a blocking `chat` method without a cancellation hook.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatModelFactory.java` creates non streaming chat models for OpenRouter, Gemini, and Ollama.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/utilities/ToolExecutorFactory.java` wraps each LangChain4j `DefaultToolExecutor` with `EventDispatchToolExecutor`, so tool executions are marshaled to the Swing event dispatch thread.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/utilities/EventDispatchToolExecutor.java` uses `ViewController.invokeAndWait` to run tool execution on the event dispatch thread when not already on it, and throws an error if interrupted or if the delegate fails.
- **Design:**
  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "ChatRequestController" as RequestController
  participant "StreamingChatService" as StreamingService
  participant "StreamingChatModel" as StreamingModel

  User -> ChatPanel: click Send
  ChatPanel -> RequestController: startRequest(message)
  RequestController -> ChatPanel: set button label Stop
  RequestController -> StreamingService: startStreaming(message)
  StreamingService -> StreamingModel: stream response
  StreamingModel --> StreamingService: token events
  StreamingService --> ChatPanel: append partial output
  User -> ChatPanel: click Stop
  ChatPanel -> RequestController: cancelRequest()
  RequestController -> StreamingService: cancel streaming
  RequestController -> ChatPanel: reset button label Send
  note right of RequestController
    If streaming cancellation is not
    supported, ignore remaining output
    and reset the user interface state.
  end note
  @enduml
  ```
  - Introduce a request controller that owns the active chat request state and exposes `startRequest` and `cancelRequest` for the chat panel to call.
  - Replace the blocking `chat` call with a streaming chat service that emits partial text updates to the chat panel and provides a cancellation handle.
  - Update the chat panel to reuse the same button and toggle its label or icon between Send and Stop while a request is active, and to re enable input on completion or cancellation.
  - Add a dedicated thinking display area that is populated only from partial thinking callbacks during the active request, and clear it when a new user message is sent or when the request is canceled or completed.
  - Gate the thinking display behind a preference toggle so it can be enabled by default but hidden when disabled by the user.
  - Show the thinking display only when partial thinking output arrives for the active request, and keep it hidden for models that do not emit thinking output.
  - Prefer LangChain4j streaming chat model support where available; if a provider does not expose streaming cancellation, treat Stop as a soft cancel that stops updates and restores the user interface state.
  - For non streaming or tool execution loops, stop should set a cancellation flag owned by the chat request controller; the tool execution loop and tool executor should check the flag before each tool call and bail out with a cancellation error, while still restoring the user interface state.
  - Keep the existing chat history append behavior but allow partial assistant output to accumulate into a single assistant message for streaming updates.
- **Test specification:**
  - Add unit tests for the request controller covering start, cancel, and completion transitions, including button label state changes.
  - Add a streaming service test using a fake streaming model that emits multiple tokens, then verify that cancel stops further updates.
  - Add a user interface test or manual verification checklist for the thinking display area, including that it clears on new send and on cancellation.

## Subtask: Streaming and stop control
- **Status:** Planning
- **Scope:** Add streaming chat support, map the send button to stop during active requests, and wire cancellation into the request lifecycle.
- **Motivation:** Enable user controlled cancellation and incremental response updates.
- **Research:** Use the existing AI chat panel and chat service flow, with the current tool executor behavior noted in the main task research.
- **Design:** Introduce a request controller that manages active request state, streaming callbacks, and stop behavior, while keeping cancellation flag checks in tool execution.
- **Test specification:** Add focused tests for request start, stop, and completion state transitions, including cancellation flag handling.

## Subtask: Thinking display area
- **Status:** Planning
- **Scope:** Add a thinking display area that shows only the current request thinking output and clears on new send, cancel, or completion, and respects a user preference toggle.
- **Motivation:** Surface useful interim thinking output to help users decide when to stop a request.
- **Research:** Review the chat panel layout and configuration storage for existing preference toggles related to chat display.
- **Design:** Add a thinking panel that is hidden until partial thinking output arrives and is suppressed when the preference is disabled or when the model does not emit thinking output.
- **Test specification:** Add manual verification steps for showing, clearing, and hiding the thinking display area under each toggle and model condition.
  - Add a non streaming cancellation test that cancels a long running request and verifies the stop action prevents additional tool calls and restores the user interface state.
  - Perform a manual check in the chat panel to confirm Send toggles to Stop during a request and that Stop returns the user interface to an idle state without appending more output.

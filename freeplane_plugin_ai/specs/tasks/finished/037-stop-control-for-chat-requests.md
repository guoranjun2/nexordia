# Task: Stop control for chat requests without streaming
- **Task Identifier:** 2026-01-31-stop-chat-non-streaming
- **Scope:** Provide a stop control in the AI chat panel so the same
  send button toggles to stop (icon or text) during an active request,
  and support canceling in-flight chat responses and tool execution
  loops without using streaming APIs. Allow sending a new message
  immediately after stop without waiting for the prior response.
- **Motivation:** Users need to interrupt long AI responses; the
  current flow blocks until completion.
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
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    uses a `SwingWorker` to call `chatService.chat` and disables the
    send button until the worker completes.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatService.java`
    wraps a LangChain4j `ChatModel` and exposes a blocking `chat`
    method without a cancellation hook.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatModelFactory.java`
    creates non-streaming chat models for OpenRouter, Gemini, and
    Ollama.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/utilities/ToolExecutorFactory.java`
    wraps each LangChain4j `DefaultToolExecutor` with
    `EventDispatchToolExecutor`, so tool executions are marshaled to
    the Swing event dispatch thread.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/utilities/EventDispatchToolExecutor.java`
    uses `ViewController.invokeAndWait` to run tool execution on the
    event dispatch thread when not already on it, and throws an error
    if interrupted or if the delegate fails.
- **Design:**
  ```plantuml
  @startuml
  actor User
  participant "AIChatPanel" as ChatPanel
  participant "ChatRequestController" as RequestController
  participant "AIChatService" as ChatService

  User -> ChatPanel: click Send
  ChatPanel -> RequestController: startRequest(message)
  RequestController -> ChatPanel: set button label Stop
  RequestController -> ChatService: chat(message)
  User -> ChatPanel: click Stop
  ChatPanel -> RequestController: cancelRequest()
  RequestController -> ChatPanel: reset button label Send
  note right of RequestController
    Cancellation sets a flag so the tool
    execution loop stops before the next
    tool call and the UI resets without
    waiting for the model response.
  end note
  @enduml
  ```
  - Introduce a request controller that owns the active chat request
    state and exposes `startRequest` and `cancelRequest` for the chat
    panel to call.
  - Keep the existing blocking `chat` call but add a cancellation flag
    that suppresses tool execution and discards late responses.
  - Update the chat panel to reuse the same button and toggle its
    label or icon between Send and Stop while a request is active, and
    to re-enable input on completion or cancellation.
  - For tool execution loops, check the cancellation flag before each
    tool call and bail out with a cancellation error while restoring
    the user interface state.
  - On cancellation, remove the in-flight user message and any related
    tool/assistant output from both chat memory and the message pane,
    then restore the user text in the input field.
  - While a request is active, lock the input field to prevent editing
    and use the Send button as the Stop control.
  - Starting a new chat or opening a chat from history must cancel the
    current request using the same cancellation flow.
- **Test specification:**
  - Add unit tests for the request controller covering start, cancel,
    and completion transitions, including button label state changes.
  - Add a non-streaming cancellation test that cancels a long-running
    request and verifies stop prevents additional tool calls and
    restores the user interface state.
  - Verify cancel removes the in-flight user message and related tool
    output from the message pane and chat memory, and restores the
    message text into the input field.
  - Verify the input field is locked while a request is active and
    Send acts as Stop.
  - Verify New Chat and Open History trigger the same cancellation
    flow before switching context.

## Subtask: Stop control without streaming
- **Status:** Finished
- **Scope:** Add a stop control that cancels a non-streaming chat
  request and allows a new message without waiting for completion.
- **Motivation:** Enable user-controlled cancellation without adding
  streaming complexity.
- **Research:** Use the existing AI chat panel and chat service flow,
  with the current tool executor behavior noted in the main task
  research.
- **Design:** Introduce a request controller that manages active
  request state, cancellation flag checks, and stop button behavior.
- **Test specification:** Add focused tests for request start, stop,
  and completion state transitions, including cancellation flag
  handling.

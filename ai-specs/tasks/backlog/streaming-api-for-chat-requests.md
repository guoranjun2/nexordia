# Task: Streaming API for chat requests
- **Task Identifier:** 2026-01-31-streaming-chat
- **Scope:** Introduce streaming chat support for AI responses, emit
  partial output updates, and support cancellation through the
  streaming model APIs, building on the non-streaming stop-control
  requirements with the only difference that intermediate results can
  be kept.
- **Motivation:** Provide faster feedback and smoother user
  interaction during long responses.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatService.java`
    uses a blocking `chat` method without streaming.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatModelFactory.java`
    currently builds non-streaming models.
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
  @enduml
  ```
  - Replace the blocking `chat` call with a streaming chat service that
    emits partial text updates to the chat panel and provides a
    cancellation handle.
  - Keep partial assistant output aggregated into a single assistant
    message during streaming updates.
  - Reuse the non-streaming stop-control behavior, except that
    intermediate assistant output produced before cancellation may be
    kept in the message history.
- **Test specification:**
  - Add a streaming service test using a fake streaming model that
    emits multiple tokens, then verify cancel stops further updates.
  - Add a unit test for request controller streaming transitions,
    ensuring stop resets the user interface state.

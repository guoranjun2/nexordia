# Task: Live chat session management and list
- **Task Identifier:** 2026-01-24-live-chat
- **Scope:** Preserve live chat session continuity within a single
  Freeplane runtime, support switching between multiple live sessions,
  allow renaming, and provide a live chat list with map root short text
  counts for disambiguation.
- **Motivation:** Users need to manage multiple live tool-capable chats
  safely within one app session, with clear visibility of which maps
  were used.
- **Developer Briefing:** Live sessions remain tool-capable in-memory.
  The live chat list shows display names and map root short text counts
  derived from currently open maps. Users can switch, rename, or close
  a live session without impacting transcript persistence features.
- **Research:**
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java`
    handles chat UI and message flow for the active live session.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatSessionMemoryController.java`
    manages LangChain4j chat memory for a single session.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java`
    provides runtime map identifiers for open maps.
- **Design:**

  ```plantuml
  @startuml
  actor User
  participant AIChatPanel
  participant LiveChatController
  participant ChatMessageHistory
  participant ChatSessionMemoryController
  participant AIToolPipeline
  User -> AIChatPanel : send message
  AIChatPanel -> ChatMessageHistory : appendUserMessage(text)
  AIChatPanel -> ChatSessionMemoryController : addUserMessage(text)
  AIChatPanel -> AIToolPipeline : execute tools
  AIToolPipeline -> AIChatPanel : assistant reply
  AIChatPanel -> ChatMessageHistory : appendAssistantMessage(text)
  AIChatPanel -> ChatSessionMemoryController : addAssistantMessage(text)
  User -> AIChatPanel : switch chat
  AIChatPanel -> LiveChatController : switch session
  LiveChatController -> ChatMessageHistory : snapshot/restore
  LiveChatController -> ChatSessionMemoryController : replace controller
  @enduml
  ```

  ```plantuml
  @startuml
  package "org.freeplane.plugin.ai.chat" {
    class AIChatPanel
    class LiveChatController
    class LiveChatListDialog
    class LiveChatSessionManager
    class LiveChatSession
    class LiveChatSessionSummary
  }
  AIChatPanel --> LiveChatController
  LiveChatController --> LiveChatListDialog
  LiveChatController --> LiveChatSessionManager
  LiveChatListDialog --> LiveChatController
  LiveChatSessionManager --> LiveChatSession
  LiveChatSessionManager --> LiveChatSessionSummary
  @enduml
  ```

  Message send flow remains unchanged; `LiveChatController` handles
  session switching by snapshotting history and swapping memory.
  `LiveChatSessionManager` tracks live sessions and supports rename and
  close operations for the list dialog.
- **Test specification:**
  - Verify live session tools still operate during UI list actions.
  - Verify starting a new chat resets UI history and in-memory tool
    session as before.
  - Manual: switching chats preserves distinct UI history and memory.
  - Manual: renaming updates the live chat list immediately.
  - Manual: closing a live chat removes it from the list.
  - Manual: map root short texts are computed from live map ids and
    appear in the live chat list.


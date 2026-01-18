# Task: Respect selection for chat copy and drag
- **Task Identifier:** 2026-01-18-chat-copy-selection
- **Scope:** Update the chat copy and drag and drop behavior so a selected snippet is transferred instead of the full response, while keeping full message transfer when no selection exists. Add a select all shortcut for the chat history pane.
- **Motivation:** Users expect the copy shortcut and drag actions to use the selected text, not the full assistant response, especially for long replies.
- **Research:**
  ```plantuml
  @startuml
  actor User
  participant "ChatMessageTransferHandler" as TransferHandler
  participant "ChatMessageHistory" as MessageHistory
  participant "MessageEntry" as Entry

  User -> TransferHandler: copy or drag selection
  TransferHandler -> MessageHistory: createTransferable(selectionStart, selectionEnd)
  MessageHistory -> Entry: select entries by overlap
  MessageHistory --> TransferHandler: full message text for each entry
  note right of MessageHistory
    Selected range is ignored
    beyond entry overlap.
  end note
  @enduml
  ```
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMessageTransferHandler.java` forwards selection offsets to `ChatMessageHistory.createTransferable`.
  - `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/ChatMessageHistory.java` collects full message entries that overlap the selection and concatenates their full source text, ignoring the selected substring.
  - The current transfer data always represents full message text and markup, even when only a snippet is selected.
- **Design:**
  - Add selection aware transfer logic that extracts the selected text from the chat history document and uses it for the plain text transfer.
  - When the selection spans multiple message entries, still return only the selected text, not the full entries, for the plain text transfer.
  - For markup transfer, provide a markup fragment that reflects the selected range when possible; otherwise fall back to full message markup only when no selection exists.
  - If the selection start and end offsets fall within the same message entry, omit the outer message div and its style class from the markup transfer so the fragment does not include the surrounding container.
  - Keep the current full message transfer behavior when there is no selection or when selection extraction fails.
  - Use the same selection aware transferable for copy and drag so drag and drop carries only the selected snippet.
  - Add a select all shortcut that selects the full chat history content within the message history pane.
- **Test specification:**
  - Add a unit test for `ChatMessageHistory.createTransferable` to verify a partial selection returns only the selected text.
  - Add a unit test for multi message selections to verify only the selected substring is returned.
  - Perform a manual check that copy and drag and drop actions use selected text and that full message transfer still works when nothing is selected.
  - Perform a manual check that the select all shortcut selects all chat history text.

# Task: Refresh chat memory token setting live
- **Task Identifier:** 2026-03-17-refresh-memory
- **Scope:** Make the AI chat memory maximum token count setting read the
  current property value during chat-memory decisions instead of using a
  value snapshotted when chat memory or session controllers were created.
- **Motivation:** Increasing the setting during an existing chat should
  immediately affect later memory compaction decisions. The current
  implementation ignores the updated value because it caches the parsed
  number.
- **Scenario:** A user raises the AI chat memory maximum token count
  while an existing chat remains open. Later turns should use the new
  limit when deciding how much conversation context to keep.
- **Briefing:** `ChatMemorySettings` currently parses the value once in
  its constructor. `AIChatPanel` and `LiveChatController` both rely on
  `ChatMemorySettings.getMaximumTokenCount()` when creating
  `AssistantProfileChatMemory` instances or dynamic max-token providers.
- **Research:**
  - `ChatMemorySettings` caches `maximumTokenCount` in an instance field.
  - `AIChatPanel` already passes a dynamic max-token callback to
    `AssistantProfileChatMemory`, but that callback closes over the
    cached `ChatMemorySettings` value.
  - `LiveChatController` also stores a `ChatMemorySettings` instance and
    uses it for later session creation.
- **Design:** Store the `ResourceController` in `ChatMemorySettings` and
  parse the property in `getMaximumTokenCount()` so each call sees the
  latest configured value. Keep callers unchanged so ongoing chats and
  future live sessions pick up the new limit without recreating the
  panel.
- **Test specification:**
  - Automated tests:
    - Add a unit test proving `ChatMemorySettings.getMaximumTokenCount()`
      reflects property changes across repeated calls on the same
      instance.
  - Manual tests:
    - N/A

# Task: Align prompt chat model and tool session overrides
- **Task Identifier:** 2026-05-17-prompt-overrides
- **Scope:**
  Track the follow-up work from `031-add-ai-prompts.md` in a separate
  task file. First align shown prompt chats with prompt-specific model
  selection so model overrides survive follow-up messages and transcript
  restore. Then add prompt-specific tool selection using the same
  session-override pattern.
- **Motivation:**
  The prompt model path originally existed only for the first shown-
  prompt request, while prompt tools were still fixed to editing.
  Landing the model session lifecycle first reduced risk, validated the
  visible-chat override pattern, and now gives the tool increment a
  concrete implementation to mirror instead of a parallel design.
- **Scenario:**
  A user runs a shown prompt with an explicit model. The chat model
  selector shows that prompt-applied value, and follow-up messages in
  the same chat keep using it.

  If the user changes the model in the chat UI, that explicit user
  action becomes the new global default for future chats and clears the
  model session override for the current chat.

  In the follow-up increment, prompt tool selection follows the same
  lifecycle: prompt launch can show a chat-specific value without
  changing defaults automatically, while an explicit tool change in the
  chat UI updates the global default and clears that session override
  for that dimension.
- **Constraints:**
  - Keep `031-add-ai-prompts.md` as the historical record of the
    finished prompt feature; this task owns the follow-up increments.
  - Prompt launch must not change global defaults automatically.
  - When the user explicitly changes model or tool in the chat UI of a
    shown prompt chat, that explicit user action must update the
    corresponding global default and clear the session override for that
    dimension.
  - When a dimension has no session override, it follows the current
    global default.
  - If persisted transcripts lack the new session-metadata fields,
    restore defaults to regular-chat semantics.
  - Keep the current chat-side control placement for this task.
- **Briefing:**
  The prompt manager and prompt persistence live under
  `org.freeplane.plugin.ai.prompt` and
  `org.freeplane.plugin.ai.prompt.ui`. `AIChatPanel` owns prompt launch,
  visible-chat activation, the chat model selector, and the popup-menu
  tool control. Live session state lives in `LiveChatSession` and
  `LiveChatController`, while transcript persistence uses
  `ChatTranscriptRecord` and `ChatTranscriptStore`.

## Subtask: Align shown prompt chats with model session overrides
- **Status:** done
- **Scope:**
  Make shown prompt chats keep prompt-specific model selection as a live
  session override for follow-up messages and transcript restore, show
  that effective model in the chat selector, and treat an explicit
  model change in the chat UI as a switch back to the normal global
  model path for that chat. Hidden prompts keep using the prompt model
  only for the launched request. Prompt tool behavior stays unchanged in
  this increment.
- **Motivation:**
  The original prompt model selection affected only the first shown-
  prompt request. Follow-up messages recreated the chat service from the
  global model selection, so the shown chat no longer matched the
  prompt that opened it. Fixing the model path first gave one clear
  session-override pattern that the later tool subtask can reuse.
- **Scenario:**
  A user saves prompt `Rewrite branch` with
  `OpenRouter: openai/gpt-4.1-mini` and `Show in chat` enabled. Running
  it opens a fresh prompt chat whose first request and follow-up
  messages keep using that model. The chat model selector shows that
  model while the prompt chat is active.

  If the user then changes the selector to Gemini in that chat, the
  current chat switches to Gemini through the normal global model path,
  the model session override is cleared, and future normal chats use the
  same Gemini selection.

  If the shown prompt chat is restored from transcript before that user
  change, it restores the prompt-chat assistant-profile-disabled
  semantics together with the explicit model override.
- **Constraints:**
  - Keep the current prompt-tool behavior unchanged in this increment.
    Shown and hidden prompts continue to use the existing editing-tool
    behavior until the tool subtask becomes current.
  - Applying a prompt's model override to a shown chat must not write
    `ai_selected_model` automatically on prompt launch.
  - A shown prompt chat must display its effective model in the
    existing chat model selector.
  - If the user explicitly changes the model from the chat selector,
    that explicit chat-side change must update `ai_selected_model`,
    clear the current chat's `selectedModelOverride`, and move that chat
    to the normal global-model path from then on.
  - If `selectedModelOverride` is absent, the active chat uses the
    current global model selection.
  - Transcript restore must use explicit metadata when present. Missing
    metadata defaults to regular-chat semantics with assistant profiles
    enabled and no model override.
  - No display-name-based prompt fallback is retained for the new
    model-restore behavior.
- **Completion:**
  Implemented in commit `9ad0826bec`.

  Delivered behavior:
  - shown prompt chats persist `selectedModelOverride` in live-session
    state and transcript metadata;
  - visible prompt follow-up requests reuse that model override;
  - activating a shown prompt chat updates the chat model selector to
    show the effective session value without persisting a new global
    default;
  - explicit user model changes in the selector clear the session
    override and keep using the global model path;
  - transcript restore now uses explicit `assistantProfileEnabled` and
    `selectedModelOverride` metadata instead of
    `ai_prompt_session_prefix` inference.
- **Verification:**
  - Automated tests added/updated:
    - `AIModelSelectionControllerTest`
    - `LiveChatControllerTest`
  - Full module suite passed:
    - `gradle -Djava.net.preferIPv6Addresses=true -Djava.awt.headless=true :freeplane_plugin_ai:test`

## Subtask: Add prompt tool selection using the model override pattern
- **Status:** in-progress
- **Scope:**
  Add optional per-prompt tool selection in the prompt manager via a
  non-editable dropdown beside the existing model selector, and make
  shown prompt chats keep the prompt's explicit tool choice as a live
  session override for follow-up messages and transcript restore,
  without changing the global default used by new chats or prompts that
  still choose current/default tools.
- **Motivation:**
  Prompt tools are still hardcoded to editing behavior even after the
  model-session work. The visible-chat override lifecycle now exists for
  model selection, so tool selection should follow the same pattern
  instead of inventing a second semantics for prompt-created chats.

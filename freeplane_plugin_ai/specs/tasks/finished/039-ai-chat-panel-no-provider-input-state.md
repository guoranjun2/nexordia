# Task: AI chat panel no provider input state
- **Task Identifier:** 2026-02-01-ai-chat
- **Scope:**
  - Update the AI chat panel input and send button behavior when no AI
    provider is configured and Ollama is disabled.
  - Add a translated message that explains no AI provider is configured
    for the read-only input state.
  - Keep the existing menu/popup, but add a preferences icon and update
    the send button behavior when no provider is configured, including
    the preferences icon and tooltip for the send button in that state.
  - Keep the input-field send action as a no-op when no provider is
    configured.
  - Reload the model selection list when AI-related configuration
    properties change.
- **Motivation:**
  - Make the initial AI chat experience clear and guide users to
    preferences when no provider is configured.
- **Developer Briefing:**
  - The AI chat panel should detect the initial configuration where
    no provider is usable and present a read-only input with a
    translated message, while the send button opens AI preferences.
- **Research:**
  - `AIChatPanel` creates `inputArea` and `sendButton`, wires send and
    cancel actions, and already exposes `openPreferences()` to open
    the AI preferences panel. `sendButton` currently toggles between
    send and cancel states only. `inputArea` is editable except during
    an active request.
  - The toolbar uses a menu button and popup menu; the preferences menu
    item can be updated to show an icon without changing the menu.
  - `AIProviderConfiguration` exposes the selected model value, keys
    for OpenRouter and Gemini, and an Ollama enabled flag; there is no
    existing helper to determine whether any provider is configured.
  - `AIChatPanel.ensureChatService()` appends error chat messages when
    model selection is missing, keys are missing, or Ollama is
    disabled, but there is no pre-send UI guard for the initial
    configuration state.
  - Translation keys for AI chat UI exist in
    `freeplane/src/viewer/resources/translations/Resources_en.properties`.
    There is no existing key for a "no provider configured" message.
- **Design:**

```plantuml
set separator none
package freeplane {
  package ai.chat {
    class AIChatPanel {
      - inputArea: JTextArea
      - sendButton: JButton
      - sendIcon: Icon
      - stopIcon: Icon
      - sendTooltipText: String
      - cancelTooltipText: String
      - preferencesTooltipText: String
      + openPreferences(): void
      - updateInputState(): void
      - isProviderConfigured(): boolean
      - setNoProviderState(): void
      - setProviderReadyState(): void
    }
    class AIProviderConfiguration {
      + getSelectedModelValue(): String
      + getOpenRouterKey(): String
      + getGeminiKey(): String
      + isOllamaEnabled(): boolean
    }
  }
}
AIChatPanel ..> AIProviderConfiguration : reads provider settings
AIChatPanel ..> ResourceController : listens for AI property changes
AIChatPanel ..> TextUtils : resolves translation
```

The toolbar menu button and popup remain. The preferences menu item
uses `generic_settings.svg?useAccentColor=true` for its icon.
When the send button opens preferences, it uses the same generic
settings icon and a preferences tooltip.
AI provider configuration changes also trigger a refresh of the model
selection list so the dropdown reflects updated properties.

- **Test specification:**
  - Manual tests:
    - With no OpenRouter/Gemini keys, no selected model, and Ollama
      disabled, the input area is read-only, shows the translated
      "no provider configured" message, and clicking send opens
      Preferences > Plugins > AI.
    - After enabling Ollama or setting a provider key and selecting a
      model, the input area becomes editable, the placeholder message
      is cleared, and send behaves normally.
    - While a request is active, cancel behavior still works and does
      not get replaced by preferences navigation.
    - The preferences menu item shows the generic settings icon and
      the popup menu remains accessible.
    - When the send button opens preferences, it shows the generic
      settings icon and a preferences tooltip.
    - The input-field send action does nothing when no provider is
      configured.
    - Changing any AI provider configuration (keys, service addresses,
      allowlists, or provider selection) refreshes the model selection
      dropdown list.

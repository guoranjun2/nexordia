# Task: Configure AI panel message font size with scaled HTML styling
- **Task Identifier:** 2026-02-14-ai-panel-font-size
- **Scope:** Update AI panel message rendering to use scaled HTML style
  handling and add a configurable main chat message font size in AI
  plugin settings.
- **Motivation:** AI panel message text should follow Freeplane display
  scaling behavior and allow users to adjust readability through
  preferences instead of hardcoded values.
- **Developer Briefing:** Replace plain HTML stylesheet usage in AI chat
  message rendering with scaled stylesheet support and wire the main
  message font size to a plugin preference with defaults and translation
  keys. Keep changes minimal and focused on AI panel rendering.
- **Research:**
  - `ChatMessageStyleApplier` currently creates a plain `StyleSheet` and
    hardcodes `body` font size `12pt`.
  - Core provides `org.freeplane.core.ui.components.html.ScaledStyleSheet`
    and `ScaledEditorKit` for display-scale-aware HTML rendering.
  - AI plugin preferences are defined through
    `freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml`
    and defaults in `defaults.properties`, with labels in
    `freeplane/src/viewer/resources/translations/Resources_en.properties`.
- **Design:**

```plantuml
@startuml
set separator none
package "freeplane_plugin_ai" {
  package "chat" {
    class AIChatPanel
    class ChatMessageStyleApplier
    class AIChatMessageStyleSettings
  }
  package "core html" {
    class ScaledEditorKit
    class ScaledStyleSheet
  }
  package "config" {
    class AiPreferencesXml
    class AiDefaultsProperties
    class AiTranslations
  }
}

AIChatPanel --> ScaledEditorKit : sets editor kit for chat history
AIChatPanel --> AIChatMessageStyleSettings : reads configured font size
AIChatPanel --> ChatMessageStyleApplier : applies message styles
ChatMessageStyleApplier --> ScaledStyleSheet : creates scaled style sheet
AIChatMessageStyleSettings --> AiDefaultsProperties : reads default value
AiPreferencesXml --> AIChatMessageStyleSettings : exposes setting in UI
AiTranslations --> AiPreferencesXml : translated label and tooltip
@enduml
```

Introduce `AIChatMessageStyleSettings` to read a new AI preference
property for chat message font size (main body size).

Update AI panel setup to use `ScaledEditorKit` for message history and
pass configured font size to `ChatMessageStyleApplier`.

Update `ChatMessageStyleApplier` to build styles on top of
`ScaledStyleSheet`, applying configured main font size to message body.
Derive the secondary small text size automatically as `5/6` of the main
size (no separate user preference).

Add new preference entries:
- property key in `defaults.properties`, with default value;
- `<number ...>` item in `preferences.xml` with reasonable bounds;
- translation keys in `Resources_en.properties` for label and tooltip.
Do not add a second preference for small/context text size.
- **Test specification:**
  - Automated tests:
    - Add/adjust AI chat style tests verifying scaled stylesheet usage
      and configured body font size application.
    - Run targeted AI plugin tests for chat style and bootstrap classes
      affected by editor kit/style changes.
  - Manual tests:
    - Open AI panel on standard scale and confirm message rendering is
      unchanged at default font setting.
    - Change new font-size setting in preferences and confirm AI panel
      message text size updates after reopening panel.

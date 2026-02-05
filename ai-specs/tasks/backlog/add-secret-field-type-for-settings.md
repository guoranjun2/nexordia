# Task: Add secret field type for settings
- **Task Identifier:** 2026-02-05-secret-settings
- **Scope:** Add a new settings field type `secret` in addition to
  `string`. Secret fields are masked by default in the UI and include a
  show/hide toggle.
- **Motivation:** Secrets such as API keys should not be visible by
  default in the settings UI, while still allowing the user to reveal
  them when needed.
- **Developer Briefing:** Extend the settings schema to support a new
  `secret` field type alongside `string`. Update the settings UI to
  render `secret` fields as masked inputs with a show/hide toggle. No
  special handling for logging or redaction is required.
- **Research:**
  - The AI plugin settings currently support `string` fields.
  - Settings are rendered in the UI with input components tied to their
    field type.
- **Design:**

TBD

The settings schema adds a `secret` field type. The UI renderer maps
`secret` fields to a masked input component with a show/hide toggle. The
stored value remains the actual secret string; only the display is
masked by default.
- **Test specification:**
  - Deferred. No tests planned yet per request.

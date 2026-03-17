# Task: Clarify createNodes optional field omission
- **Task Identifier:** 2026-03-17-clarify-create
- **Scope:** Adjust the `createNodes` AI tool guidance so optional
  fields are omitted unless intentionally set, and replace the blank
  `mainStyle` failure with a repair-oriented validation message.
- **Motivation:** The tool currently accepts a request shape that can
  lead some models to send `mainStyle: ""`, which then fails with an
  unhelpful style lookup error instead of telling the model to omit the
  field.
- **Scenario:** A model prepares a `createNodes` call and does not want
  to control style. The tool description should bias it to omit
  `mainStyle` rather than send an empty value. If it still sends an
  empty `mainStyle`, the error should say to omit the field to keep the
  default style.
- **Briefing:** The relevant tool description lives on the `createNodes`
  method in `AIToolSet`. Blank-style validation currently falls through
  to generic style lookup from the create path via
  `NodeStyleContentEditor.setInitialMainStyle(...)`.
- **Research:**
  - The `createNodes` tool description already says optional fields
    override defaults, but only calls out empty optional textual
    fields.
  - `mainStyle` blank values currently reach style lookup and produce
    `style '' not found`.
  - The create path can reject blank `mainStyle` before style lookup
    without changing non-blank style handling.
- **Design:** Add one short sentence to the `createNodes` tool
  description establishing the global omit-unused-optional-fields rule.
  Reject blank `mainStyle` in
  `NodeStyleContentEditor.setInitialMainStyle(...)` with a verbose
  message that tells the caller to omit the field to use the default
  style.
- **Test specification:**
  - Automated tests:
    - Add a focused unit test that verifies blank `mainStyle` is
      rejected with the new repair-oriented message.
  - Manual tests:
    - N/A

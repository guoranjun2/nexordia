Title: Local estimator for removable blocks
Date: 2026-02-07
Status: Accepted

Context
Provider token usage totals are authoritative but cannot be decomposed
into per-message costs, which makes eviction and counters inconsistent
when the context window moves. We need deterministic, immediate token
estimates for removable messages (user, assistant, tool call, tool
result) while treating always-present system/tool instructions as
constant overhead.

Decision
Use a local token estimator for all removable blocks. Eviction and
token counters are computed from the same estimator-based metric. The
estimator is encapsulated inside AssistantProfileChatMemory, and the
panel receives only totals. Provider token usage is used only for the
optional "model response" counter mode.

Consequences
- Eviction and counters are consistent and update immediately on
  eviction/undo/redo.
- Estimates may differ from provider-reported totals.
- Always-present system/tool instructions are excluded from removable
  tallies, reducing noise in eviction decisions.

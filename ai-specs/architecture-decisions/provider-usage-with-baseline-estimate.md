Title: Provider usage with baseline estimate
Date: 2026-02-07
Status: Superseded

Context
We need token usage numbers for AI chat memory and UI counters. Provider
usage totals are authoritative but only arrive after each response and
do not expose per-message token costs. Local token estimation can
provide per-message costs but is approximate and may diverge from the
provider. We also need to account for always-present system and tool
instructions, plus the current profile description.

Decision
This approach was replaced by a local-estimator strategy for removable
blocks to keep eviction and counters consistent with the same metric.

Consequences
- Token usage counters reflect provider totals, with a stable lower
  bound derived from the baseline estimate.
- Eviction remains provider-driven and happens post-response.
- Baseline estimation introduces a small approximation that is limited
  to always-present context and does not affect turn-by-turn usage.
- Local estimation is not used for per-message costs, avoiding drift in
  the eviction logic.

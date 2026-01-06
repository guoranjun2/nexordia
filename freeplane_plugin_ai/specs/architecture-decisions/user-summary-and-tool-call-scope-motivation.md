# Title
User summary and optional tool call scope and motivation

# Date
2026-01-06

# Status
Proposed

# Context
Modifying tools will be shown to users for confirmation and review. A concise, user facing summary of each tool call improves transparency and makes confirmations easier to understand. There is also a question about whether the model should provide additional scope and motivation strings for each modifying tool call.

# Decision
Require a user summary string for modifying tool requests and return it in the response for display. Evaluate whether to add optional scope and motivation strings to modifying tool requests.

# Consequences
Tool request and response schemas for modifying actions will include a user summary field. If scope and motivation are added later, tool schemas and user interface confirmation text will need to be updated to display them consistently.

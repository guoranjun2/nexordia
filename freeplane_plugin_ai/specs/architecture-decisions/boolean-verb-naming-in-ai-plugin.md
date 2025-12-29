# Boolean verb naming in the ai plugin

Date: 2025-12-29
Status: Accepted

## Context
The ai plugin uses boolean fields across requests, responses, and internal models. We want consistent naming for boolean fields and their accessors, and we want the serialized property names to match the preferred naming style.

## Decision
Use a verb with "s" for boolean field and method names, such as "includesText", "allowsRefresh", and "preservesOrder". Fields and accessors use the same verb form.

## Consequences
- Field names and method names remain consistent across ai plugin structures.
- Serialized property names follow the verb naming style.
- Future boolean additions should follow the same naming pattern.

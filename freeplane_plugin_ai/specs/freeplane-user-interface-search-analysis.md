No code until design suggestions approved.

# Freeplane user interface search analysis

## Scope
This note summarizes how search is wired in the existing Freeplane user interface, with emphasis on the filter based search flow and how `IElementaryConditionController` is used.

## Entry points and flow
- `FilterController.install()` wires search and filter actions, including `FindAction`, `QuickFindAction`, and `QuickFindAllAction`.
- `FindAction` opens a dialog that embeds `FilterConditionEditor` with a search variant. The dialog binds enter and directional shortcuts to find next or previous.
- `QuickFindAction` uses the toolbar editor and triggers a one step search without opening a dialog.

## Condition pipeline
- `FilterConditionEditor` presents three core selectors: property, condition, and value.
- When the property changes, `FilterConditionEditor` asks `ConditionFactory.getConditionController(selectedProperty)` to provide valid conditions and values.
- When a search is executed, `FilterConditionEditor.getCondition()` builds an `ASelectableCondition` by calling `ConditionFactory.createCondition(...)`, which delegates to the matching `IElementaryConditionController`.
- The condition is evaluated using `condition.checkNode(node)` in `FilterController.findNextInSubtree(...)`.

## Search execution and traversal
- `FilterController.findNextInSubtree(...)` uses `MapNavigationUtils` to step through nodes in a direction and test each candidate against the selected condition.
- A search root can be set to constrain traversal; `MapView` and `IMapSelection` track the current search root.
- `QuickFindAction` uses `selection.getEffectiveSearchRoot()` and only searches within the subtree when the selection is in that subtree.

## Role of IElementaryConditionController
`IElementaryConditionController` is a small controller contract that covers:
- Ability checks for a property and condition combination.
- Condition creation for a property and value.
- Provider of value list models, value editors, and value renderers.
- Capability flags for case sensitivity and approximate matching.
- Condition loading from persisted xml elements.

Existing implementations include:
- `NodeTextConditionController` for node text, note, details, and related text fields.
- `AttributeConditionController`, `IconConditionController`, `PriorityConditionController`, and other feature specific controllers.
- Specialized controllers in plugins such as `ScriptConditionController`.

## Notes for ai search
Possible paths for reuse:
- **Direct reuse of existing controllers**: AI search could accept structured filters that are already modeled as `IElementaryConditionController` and `ASelectableCondition`. This would allow reuse of the existing property, condition, and value vocabulary, plus xml persistence.
- **Hybrid search**: AI search can use `IElementaryConditionController` as a prefilter for candidate nodes, then apply semantic scoring. This keeps existing filter semantics intact while adding relevance ranking.
- **New controller for ai search**: A dedicated `IElementaryConditionController` could be added to expose an ai semantic match condition in the same editor, but it would need to implement list models, editors, and persistence even if those are minimal.

Potential friction:
- The controller interface is oriented toward Swing model and editor types, which may be heavy for a pure ai search panel.
- The existing controllers are oriented around exact field matching, not semantic similarity.

Open questions for design:
- Should ai search be exposed in the same editor as filter search, or as a dedicated search panel?
- Do we want ai search to operate on the same `ASelectableCondition` structure, or map ai results to a new condition type that still implements `ICondition`?

## Proposed ai tool flow for condition discovery
This flow avoids exposing the entire condition tree upfront and instead reveals a relevant subtree once a property is chosen.

### Method 1: list search properties
- Returns English property names used by the ai to request conditions.
- Attribute conditions are excluded; attributes are handled by separate attribute tools.

### Method 2: list property specific conditions
- Input: property name.
- Output: available condition names and value input mode.
- Value input mode can be one of:
  - `none`: no value allowed.
  - `free_text`: `canEditValues(...)` is true and no fixed list is required.
  - `select_only`: `canSelectValues(...)` is true and editing is disabled.
  - `select_or_free_text`: both selecting and editing are allowed.
- Also return supported flags: case sensitivity, approximate matching, ignore diacritics.

### Method 3: list attribute names for a map
- Input: map identifier.
- Output: attribute names available in the map.
- This is separate because attribute names are user data and map specific.

### Method 4: search attributes by name and value
- Input: map identifier, attribute name, attribute value.
- Output: matching nodes or node identifiers.
- This fits a two string parameter model (name and value), so it is a distinct tool from condition discovery.

### Method 5: search nodes by condition
- Input: map identifier and a condition built from the shared property and condition model.
- Output: matching nodes or node identifiers.
- This method is shared by search and ai only filter state, so it uses the same condition shape.

### Method 6: manage ai only filter state
- Input: map identifier and a condition built from the shared property and condition model.
- Output: the active ai only filter condition, or a confirmation of clearing it.
- This keeps ai only filters separate from the user visible filter state.

## Concrete tool schema proposal
These schemas are intentionally small and stable. They expose English names for deterministic ai prompts, with internal mapping to controller keys.

### Tool: list search properties
Input:
- `mapIdentifier`: string, optional. If present, allows a map specific context for properties that are map dependent.

Output:
- `properties`: array of objects with:
  - `name`: string, English property name accepted by the tool.

### Tool: list search conditions for property
Input:
- `propertyName`: string, required.

Output:
- `conditions`: array of objects with:
  - `name`: string, English condition name accepted by the tool.
  - `valueInputMode`: string, one of `none`, `free_text`, `select_only`, `select_or_free_text`.
  - `allowsCaseSensitiveOption`: boolean.
  - `allowsApproximateMatchingOption`: boolean.
  - `allowsIgnoreDiacriticsOption`: boolean.
- Tools should reject property or condition names that are not in the list returned by the discovery methods.

### Tool: search nodes by condition
Input:
- `mapIdentifier`: string, required.
- `condition`: object with:
  - `propertyName`: string.
  - `conditionName`: string.
  - `value`: string.
  - `caseSensitive`: boolean.
  - `approximateMatching`: boolean.
  - `ignoreDiacritics`: boolean.
- `scopeNodeIdentifier`: string, optional.
- `maximumResults`: integer, optional.

Output:
- `nodeIdentifiers`: array of strings.
- Optional extensions: `nodeText`, `nodePath`, or both.

### Tool: list attribute names
Input:
- `mapIdentifier`: string, required.

Output:
- `attributeNames`: array of strings.

### Tool: search attributes by name and value
Input:
- `mapIdentifier`: string, required.
- `attributeName`: string, required.
- `attributeValue`: string, required.

Output:
- `nodeIdentifiers`: array of strings.
  - Optional extensions: `nodeLabel`, `nodePath`, or both.

### Tool: set ai only filter condition
Input:
- `mapIdentifier`: string, required.
- `condition`: object with:
  - `propertyName`: string.
  - `conditionName`: string.
  - `value`: string.
  - `caseSensitive`: boolean.
  - `approximateMatching`: boolean.
  - `ignoreDiacritics`: boolean.

Output:
- `activeCondition`: object mirroring the input condition.

### Tool: get ai only filter condition
Input:
- `mapIdentifier`: string, required.

Output:
- `activeCondition`: object or null if no ai only filter is active.

### Tool: clear ai only filter condition
Input:
- `mapIdentifier`: string, required.

Output:
- `cleared`: boolean.

### Supporting data structures
- `SearchPropertyDefinition`:
  - `name`: string.
- `SearchConditionDefinition`:
  - `name`: string.
  - `valueInputMode`: string.
  - `allowsCaseSensitiveOption`: boolean.
  - `allowsApproximateMatchingOption`: boolean.
  - `allowsIgnoreDiacriticsOption`: boolean.
- `SearchConditionRequest`:
  - `propertyName`: string.
  - `conditionName`: string.
  - `value`: string.
  - `caseSensitive`: boolean.
  - `approximateMatching`: boolean.
  - `ignoreDiacritics`: boolean.

## Provider mapping to Freeplane classes
This section suggests where each tool can be implemented using existing controllers and models.

### list search properties
- `FilterController.getConditionFactory().conditionIterator()` to enumerate `IElementaryConditionController`.
- For each controller, call `getFilteredProperties()` to get `TranslatedObject` entries.
- Translate controller keys into English names and exclude attribute condition properties.

### list search conditions for property
- Use `ConditionFactory.getConditionController(propertyNameOrTranslatedObject)` to resolve the matching controller after mapping the English property name back to a controller key.
- Call `getConditionsForProperty(property)` for the condition list.
- Call `canSelectValues(...)`, `canEditValues(...)`, `getValuesForProperty(...)` to infer `valueInputMode`.
- Call `isCaseDependent(...)` and `supportsApproximateMatching(...)` to set flags.
- The ignore diacritics flag can be tied to `supportsApproximateMatching(...)`, matching current editor behavior.

### search nodes by condition
- Use `ConditionFactory.createCondition(...)` to build the `ASelectableCondition`.
- Evaluate the condition over the scoped nodes, similar to the existing search traversal.

### ai only filter state
- Store the active condition in a map scoped data structure that is independent from the user visible filter.
- Apply it only when ai tools are executing.

### list attribute names
- Use the map model from the supplied map identifier.
- Attribute data is managed by the attribute feature controller and attribute models; the exact entry point should be the existing attribute controller for the map.
- Return only names to avoid a large payload and to keep values private until requested.

### search attributes by name and value
- Use the attribute controller for the map to locate nodes where the named attribute exists and matches the value.
- Expose only the node identifiers to keep tool responses small, with optional node text or paths when useful for the ai response.

## Thinking model with non-thinking overview tool
The primary model should handle reasoning and final responses, while a cheaper non-thinking model can provide a fast, scoped overview to guide targeted search.

### Rationale
- The overview tool can scan large maps in chunks cheaply, building a compact index.
- The thinking model can then use that index to run precise searches and apply conditions without relying on guesswork.
- This separation keeps reasoning quality high while reducing cost for broad scanning.

### Proposed overview tool behavior
- Input includes an optional focus request so the overview is tailored to the current user question.
- Output is an index, not a narrative summary.
- The index should be small and deterministic to avoid flooding the main model.

### Proposed overview tool schema
Tool: `generateSearchOverview`

Input:
- `mapIdentifier`: string, required.
- `focusRequest`: string, optional. If provided, prioritize terms and sections relevant to this request.
- `modelIdentifier`: string, optional. Allows selecting a cheaper or faster model for overview generation.
- `maximumKeywordCount`: integer, optional. Upper bound for keyword entries.
- `maximumSectionCount`: integer, optional. Upper bound for section entries.

Output:
- `summary`: string, short abstract describing what the map is about.
- `themes`: array of strings, short high level topics.
- `sections`: array of objects with:
  - `nodeIdentifier`: string.
  - `nodeText`: string.
  - `keywords`: array of strings (small list).
- `keywords`: array of objects with:
  - `term`: string.
  - `nodeIdentifiers`: array of strings (small list).

### Usage pattern
- The thinking model calls `generateSearchOverview` with the user request as `focusRequest`.
- It uses returned themes, sections, and keywords to choose targeted `listSearchConditionsForProperty` and `searchNodesByCondition` calls.

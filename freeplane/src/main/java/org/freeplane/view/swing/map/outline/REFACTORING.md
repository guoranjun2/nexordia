Refactoring Plan for org.freeplane.view.swing.map.outline

Purpose: capture the refactoring roadmap, decisions, progress, and guidance so work can continue safely and coherently.

Guiding Principles
- Tell, do not ask: expose behavior, not internal data structures.
- SOLID alignment: reduce god objects, separate responsibilities, and depend on abstractions where helpful.
- Backward compatibility by default: avoid breaking public surface unnecessarily; when changes are required, keep them local and well documented.
- Minimal, focused changes: keep diffs scoped; avoid opportunistic refactors.
- Phase gates: no code until plan approved for each change phase.

Current Architecture Snapshot
- OutlinePane: container wiring BreadcrumbPanel and ScrollableTreePanel with a scroll pane.
- ScrollableTreePanel: renders the content outline (blocks), handles keyboard and mouse interaction, scrolling, selection, and focus.
- BreadcrumbPanel: renders the ancestor path and provides navigation hooks.
- VisibleOutlineState: keeps logical visible state (visible node list, hovered node, breadcrumb height, first visible identifier).
- NodePositioning and OutlineGeometry: compute positions and metrics.
- NavigationButtons and ExpansionControls: show per-node expansion controls and perform expansion changes.
- MapAwareOutlinePane, MapTreeNode, NodeTreeBuilder, OutlineViewState: connect outline to the map view and persist outline view state.

Completed Work (baseline for continuation)
No code until plan approved: approved and implemented for the following subphases.

1) Decouple view state from user interface components (tell, do not ask)
- VisibleOutlineState no longer stores or exposes BlockPanel instances or raw lists for external iteration.
- New behavior methods were added:
  - getVisibleNodeCount()
  - getNodeAtVisibleIndex(int)
 - getNodeIdAtVisibleIndex(int)
- BreadcrumbPath, MapAwareOutlinePane, and ScrollableTreePanel were adapted to use behavior methods instead of pulling lists.

2a) Replace FlatNode with TreeNode level
- Removed FlatNode entirely. VisibleOutlineState now stores `List<TreeNode>`.
- Added `level` to TreeNode and maintain it via `setParent(...)`, which recomputes levels for the whole subtree (refresh on attach).
- NodePositioning and layout use `node.getLevel()` as the single source of truth for X positioning.

2) Introduce a BlockPanel cache owned by the view
- Added OutlineBlockViewCache: a small cache mapping block index to BlockPanel, owned by ScrollableTreePanel.
- ScrollableTreePanel now manages creation, sizing, and removal of BlockPanel instances through this cache.
- Added helper method isNodeVisibleInBlocks(TreeNode) to encapsulate visibility checks without leaking component internals outward.

3) Tighten data transfer object encapsulation (limited)
- BreadcrumbState: fields are private and final; added getters and return an unmodifiable breadcrumb node list.
- OutlineViewport.VisibleBlockRange: fields are private and final; added getters and contains(int) method.
- OutlineViewState: fields are private with getters for first visible node identifier, root node identifier, and saved filter; applyTo(TreeNode) remains the canonical behavior.

Motivation and Effects
- Reduces coupling between logical state and user interface components, enabling safer future refactors.
- Improves intent expression at call sites and prevents misuse of internal lists and component maps.
- Maintains behavior while clarifying responsibility boundaries (state versus view).

Follow‑up Phases (proposed)
For each phase: no code until plan approved.

Phase 3: Replace client properties with typed node buttons
- Problem: frequent use of JButton clientProperty("treeNode") is weakly typed and spreads across multiple classes.
- Change: introduce a NodeButton extends JButton with a TreeNode field and getter, and use it in BlockPanel and BreadcrumbPanel. Update readers to rely on typed access.
- Success criteria: compile-time safety for button-to-node association; no behavior change.

Phase 4: Strengthen TreeNode encapsulation
- Problem: getChildren() returns a mutable list and encourages external mutation.
- Change: return an unmodifiable view from getChildren(); offer childCount(), childAt(int), and forEachChild(Consumer<TreeNode>) helpers. Keep existing mutation methods (addChild, add(MapTreeNode,int), remove) as the only mutators.
- Success criteria: no external direct list mutation; outline behavior unchanged.

Phase 5: Extract focus and selection responsibilities
- Problem: ScrollableTreePanel mixes rendering, focus, and selection orchestration.
- Change: extract OutlineFocusManager (restore focus, isWithinOutline, focus helpers) and OutlineSelectionManager (setSelectedNode, scroll selection into view, synchronization helpers). ScrollableTreePanel delegates to them.
- Success criteria: ScrollableTreePanel shrinks substantially; focus and selection logic isolated and testable.

Phase 6: Extract block layout operations
- Problem: block creation, removal, preferred size calculation, and width computation are embedded in ScrollableTreePanel.
- Change: extract OutlineBlockLayout to manage createBlock, clearBlocks, updatePreferredFromActualBlocks, calculateActualRequiredWidth, and removeBlocksFromBlockIndex. Wire it with NodePositioning, OutlineViewport, OutlineGeometry, block size, and the block cache.
- Success criteria: rendering logic is coherent and contained; no performance regressions.

Phase 7: Consolidate keyboard input handling
- Problem: BreadcrumbPanel currently hosts global outline key bindings.
- Change: move global navigation keymap setup into OutlinePane or a dedicated OutlineKeymap, keeping BreadcrumbPanel focused on breadcrumb rendering and hover logic.
- Success criteria: keyboard behavior preserved regardless of focus location; reduced cross‑panel coupling.

Phase 8: Navigation button abstraction
- Problem: NavigationButtons directly depends on ExpansionControls implementation and exposes button fields package‑wide.
- Change: introduce an ExpansionHandler interface with expand, collapse, expandMore, reduce methods. Make ExpansionControls implement it. Keep NavigationButtons internals private and expose only attachToNode(...) and hideNavigationButtons().
- Success criteria: easier to test and replace expansion logic; internal button details encapsulated.

Phase 9: Strategy injection and construction
- Problem: ScrollableTreePanel constructs many concrete collaborators, limiting testability and extension.
- Change: accept collaborators via constructor or setters (geometry, positioning, viewport factory, selection indicator, block layout, managers, expansion handler, breadcrumb path). Provide factory helpers for default wiring to minimize call site changes.
- Success criteria: improved dependency inversion and substitutability without major public surface changes.

Phase 10: Optional visual strategies
- Change: introduce SelectionIndicator strategy (default circle) and a BlockSizingStrategy (default fixed size). Make ScrollableTreePanel consume strategies rather than hardcoded choices.
- Success criteria: visual behavior becomes pluggable without touching core logic.

Risks and Mitigations
- Behavior drift: work in small phases; verify selection, navigation, scrolling, breadcrumb updates, and hover buttons after each phase.
- Performance regressions: keep block layout logic semantically identical; measure large maps after Phases 2 and 6.
- Focus management pitfalls: audit OutlineFocusManager thoroughly; rely on manual focus regression checks across windows.
- External consumers: confine breaking changes to package‑private or internal classes; document migration steps below.
- Structural level consistency: TreeNode.setParent(parent) recomputes `level` for the subtree when parent != null; all builders and live insert paths must attach via setParent to keep levels correct.

Migration Notes (post‑Phase 2 + 2a)
- VisibleOutlineState
  - Removed methods: getBlockPanels(), addBlockPanel(...), clearBlockPanels(), hasBlockPanel(...), getBlockPanel(...), removeBlockPanel(...), getBlockPanelIndices().
  - Removed method: getVisibleNodes(). Use behavior methods instead:
    - getVisibleNodeCount()
    - getNodeAtVisibleIndex(int)
    - getNodeIdAtVisibleIndex(int)
- FlatNode removed. Do not depend on snapshot depths for layout; use TreeNode.getLevel().
- OutlineViewport.VisibleBlockRange: use getters getFirstBlock(), getLastBlock(), getBreadcrumbAreaHeight().
- BreadcrumbState: use getters getBreadcrumbNodes(), getBreadcrumbHeight(), getFirstVisibleNodeIndex(). Returned breadcrumb list is unmodifiable.
- OutlineViewState: access properties via getters and call applyTo(TreeNode) to restore expansion state.

Verification
- Build compilation for the module:
  - `pwd` should be the repository root.
  - `gradle :freeplane:compileJava`
- Run tests (if applicable for the module):
  - `gradle :freeplane:test -PTestLoggingFull`
- Manual smoke checks in a running application:
  - Breadcrumb shows and updates while scrolling.
  - Selection via keyboard and mouse in both breadcrumb and content areas.
  - Expand, collapse, expand more, and reduce actions via buttons and keys.
  - Selection indicator appears in both areas; scrolling to selection works.
  - Switching maps with filters preserves and restores outline state.

Review Checklist per Phase
- Scope agreed and “no code until plan approved” observed.
- Public surface changes documented and migration paths provided.
- Unit and manual verification performed for navigation, selection, scrolling, and expansion.
- Performance characteristics remain acceptable on large maps.

Rollback Plan
- If a phase causes regressions, revert only the phase changes and keep prior phases intact. The decoupling performed in earlier phases should make rollback scoped and safe.

Appendix A: Files touched in completed baseline
- Added: OutlineBlockViewCache.java
- Updated: VisibleOutlineState.java (TreeNode list), ScrollableTreePanel.java, MapAwareOutlinePane.java, BreadcrumbPath.java, BreadcrumbPanel.java, BreadcrumbState.java, OutlineViewport.java, OutlineViewState.java, NodeTreeBuilder.java, NodePositioning.java, TreeNode.java, MapTreeNode.java
- Removed: FlatNode.java

Appendix B: Examples (tell, do not ask)
- Before: callers fetch a list and iterate to find a node.
  - visibleState.getVisibleNodes().get(index)
- After: callers request the outcome directly.
  - visibleState.getNodeAtVisibleIndex(index)
  - visibleState.getVisibleNodeCount()
  - visibleState.getNodeIdAtVisibleIndex(index)

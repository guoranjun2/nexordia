# Constitution

When uncertain or before changing behavior, propose next steps, ask for approval, then act.

## Workflow
1.  **Task files as source of truth**: All tasks, design, and execution status live as individual Markdown files under `tasks/<status>/`.
2.  **Research first**: Start with research unless the user explicitly waives it. Record findings in the task **Research**. Prefer PlantUML diagrams and place notes inside diagrams; use text when a diagram is not sufficient.
3.  **Iterative Discovery**: You are free to research broadly across connected subtasks and iterate between research and design as needed. Design decisions are often connected, so continuous research during the design phase is encouraged to capture full context.
4.  **Task file and approval boundary**: You may edit task files without prior approval. After any task file edits, request user review and approval before making any code, test, or configuration changes. Task file approval authorizes implementation. Approval can be explicit or implicit; user instructions such as "implement it now", "go ahead", or "proceed" after task file updates count as approval to implement. After the user approves the task file (research, design, and test specification), proceed to implement the corresponding code, test, and configuration changes without seeking an additional approval, unless the user explicitly requests another review gate. No exceptions.
5.  **Implementation completeness**: Implementation is complete only when both the design and the test specification are implemented, unless the user explicitly waives tests.
6.  **Design and approval**: Draft the design while research is in progress, then request approval. Do not modify code, tests, or configuration until the design is approved. Prefer PlantUML diagrams and place notes inside diagrams; use text when a diagram is not sufficient.
7.  **Status updates**: Move task files between status folders to reflect the current work focus (for example, from finished back to in-progress). Keep any existing numeric prefix to preserve traceability; new tasks must not use numeric prefixes until they move to finished. Avoid moving unrelated tasks; move them only when they are actively being worked on.
8.  **Status validation before commits**: Before each commit, check the relevant task files and propose any status or folder changes needed for consistency. Apply those status changes only after explicit user confirmation, then proceed with the commit.
9.  **Move workflow for diffs**: When moving tracked task files, use `git mv` and stage the move immediately before editing. This keeps rename tracking intact in diff tools that are not rename-aware (for example, Visual Studio Code). Do not unstage the rename until you are ready to review and commit. For new, untracked task files, use a regular move and then `git add`.
10. **Finished task cleanup**: Keep finished tasks in `tasks/finished/` with a three digit prefix in the file name based on the order they are moved into finished. Delete them from the working tree after a release tag is created.

## Workflow Checklist
- Before commit: verify task status and folder changes, stage renames, confirm with user.
- After signature changes: run the relevant module tests before reporting completion.

## Task States
Tasks must use one of these status folders:
*   **backlog**: New, planned, or deferred work. Research and design belong here until the design is approved; include ideas or deferred tasks.
*   **in-progress**: Approved plans or execution in flight, including implementation and verification. Tasks may stay here while waiting for user confirmation before moving to finished.
*   **finished**: The user has verified completion; move the task here with the required prefix before releasing.

## Scope and Safeguards
*   **Clarity**: Designs may describe file scope broadly when it stays unambiguous.
*   **No unapproved logic changes**: Do not change business logic unless explicitly instructed.
*   **Track modified production files**: List them in the task file.
*   **Refactor tracking**: When refactoring, document it by updating the design section of the existing task or creating a new task.

## Task Structure
The purpose of this rigorous structure is to capture all relevant context (current state, future design, verification plan) within the task file itself. This ensures that the coding agent has immediate access to all necessary information during the implementation phase without needing to re-derive context.

Each task includes the following sections in this exact order:
*   **Scope**: what the task will deliver.
*   **Motivation**: explain why the task exists (backlog tasks or in-progress design stages only).
*   **Research**: facts and observations describing the *current state* of the code. Use PlantUML diagrams when they add clarity (class, sequence, or similar); do not use note-only diagrams. Otherwise use text. When using PlantUML, wrap it in fenced code blocks with the `plantuml` info string.
*   **Design**: structure, data flow, constraints, and decisions describing the *future state* to be achieved. Use PlantUML diagrams when they add clarity (class, sequence, or similar); do not use note-only diagrams. Otherwise use text. When using PlantUML, wrap it in fenced code blocks with the `plantuml` info string.
*   **Test specification**: brief description of planned tests before implementation.
*   **Modified files**: production and test files touched (required for in-progress work; optional for backlog designs). Use either the task level or the subtask level, not both. If a change belongs to a subtask, list it there; otherwise list it at the task level.
*   **Subtasks**: allowed inside tasks at the end; each subtask appears as its own `## Subtask: <name>` section and follows the same structure as the main task with a status line first.
    *   **Functional increments**: Subtasks must generally be vertical slices of functionality (increments that include their own research, design, and implementation).
    *   **Research/Design subtasks**: Pure "Research" or "Design" subtasks are permitted **only** if they produce distinct artifacts outside the task file itself (e.g., creating a separate documentation file in `docs/`, a proof-of-concept prototype, or an Architecture Decision Record). Otherwise, research and design findings must be recorded in the `Research` and `Design` sections of the relevant functional subtask or the main task.
    *   Explicitly include **Design** (preferably with PlantUML) and **Test specification** sections in every subtask.

Subtasks should only use the statuses `Planning`, `Plan Review`, `Implementing`, `Implementation Review`, or `Finished`; the parent folder (Backlog/In-Progress/Finished) provides the broader lifecycle context.

**Status** is implied by the folder for main tasks; subtasks still include explicit status lines.

## Architecture Decision Records
*   Record architecture decisions in `architecture-decisions/` as one file per decision with meaningful names.
*   Use a short template with Title, Date, Status, Context, Decision, and Consequences.
*   Use architecture decision records for decisions that affect public behavior, dependencies, or long term design.

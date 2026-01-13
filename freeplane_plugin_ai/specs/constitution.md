# Constitution

When uncertain or before changing behavior, propose next steps, ask for approval, then act.

## Workflow
1.  **Task files as source of truth**: All tasks, design, and execution status live as individual Markdown files under `specs/tasks/<status>/`.
2.  **Research first**: Start with research unless the user explicitly waives it. Record findings in the task **Research**. Prefer PlantUML diagrams and place notes inside diagrams; use text when a diagram is not sufficient.
3.  **Design and approval**: Draft the design while research is in progress, then request approval. Do not modify code, tests, or configuration until the design is approved. Prefer PlantUML diagrams and place notes inside diagrams; use text when a diagram is not sufficient.
4.  **Status updates**: Move task files between status folders to reflect the current work focus (for example, from finished back to in-progress). Keep any existing numeric prefix to preserve traceability; new tasks must not use numeric prefixes until they move to finished. Avoid moving unrelated tasks; move them only when they are actively being worked on.
4.  **Status validation before commits**: Before each commit, check the relevant task files and propose any status or folder changes needed for consistency. Apply those status changes only after explicit user confirmation, then proceed with the commit.
5.  **Move workflow for diffs**: When moving tracked task files, use `git mv` and stage the move immediately before editing. This keeps rename tracking intact in diff tools that are not rename-aware (for example, Visual Studio Code). Do not unstage the rename until you are ready to review and commit. For new, untracked task files, use a regular move and then `git add`.
6.  **Finished task cleanup**: Keep finished tasks in `specs/tasks/finished/` with a three digit prefix in the file name based on the order they are moved into finished. Delete them from the working tree after a release tag is created.

## Workflow Checklist
- Before commit: verify task status and folder changes, stage renames, confirm with user.
- After signature changes: run the relevant module tests before reporting completion.

## Task States
Tasks must use one of these status folders:
*   **Backlog**: New, planned, or deferred work. Research and design belong here until the design is approved; include ideas or deferred tasks.
*   **In-Progress**: Approved plans or execution in flight, including implementation and verification. Tasks may stay here while waiting for user confirmation before moving to finished.
*   **Finished**: The user has verified completion; move the task here with the required prefix before releasing.

## Scope and Safeguards
*   **Clarity**: Designs may describe file scope broadly when it stays unambiguous.
*   **No unapproved logic changes**: Do not change business logic unless explicitly instructed.
*   **Track modified production files**: List them in the task file.
*   **Refactor tracking**: When refactoring, document it by updating the design section of the existing task or creating a new task.

## Task Structure
Each task includes the following sections in this exact order:
*   **Scope**: what the task will deliver.
*   **Motivation**: explain why the task exists (backlog tasks or in-progress design stages only).
*   **Research**: facts and observations, preferably captured as PlantUML diagrams with notes; use text when a diagram is not sufficient.
*   **Design**: structure, data flow, constraints, and decisions, preferably captured as PlantUML diagrams with notes; use text when a diagram is not sufficient.
*   **Test specification**: brief description of planned tests before implementation.
*   **Modified files**: production and test files touched (required for in-progress work; optional for backlog designs). Use either the task level or the subtask level, not both. If a change belongs to a subtask, list it there; otherwise list it at the task level.
*   **Subtasks**: allowed inside tasks; each subtask follows the same structure and includes its own status section as the first element within the parent task.

Subtasks should only use the statuses `Planning`, `Plan Review`, `Implementing`, `Implementation Review`, or `Finished`; the parent folder (Backlog/In-Progress/Finished) provides the broader lifecycle context.

**Status** is implied by the folder for main tasks; subtasks still include explicit status lines.

## Architecture Decision Records
*   Record architecture decisions in `specs/architecture-decisions/` as one file per decision with meaningful names.
*   Use a short template with Title, Date, Status, Context, Decision, and Consequences.
*   Use architecture decision records for decisions that affect public behavior, dependencies, or long term design.

# Constitution

## Purpose
Keep work verifiable with a light, repeatable workflow that avoids surprises.
Established abbreviations like "ai", "uuid", and "llm" are valid terms and are not treated as abbreviations.

## Workflow
1.  **Task files as source of truth**: All tasks, design, and execution status live as individual Markdown files under `specs/tasks/<status>/`. Backlog and ideas are historical artifacts.
2.  **Research first**: Start with research unless the user explicitly waives it. Record findings in the task **Research summary**. Prefer PlantUML diagrams and place notes inside diagrams; use text when a diagram is not sufficient.
3.  **Design and approval**: Draft the design while research is in progress, then request approval. Do not modify code, tests, or configuration until the design is approved. Prefer PlantUML diagrams and place notes inside diagrams; use text when a diagram is not sufficient.
4.  **Status updates**: Move task files between status folders to reflect progress.
5.  **Information gathering**: Reading commands do not require plan approval.
6.  **Finished task cleanup**: Keep finished tasks in `specs/tasks/finished/` with a three digit prefix in the file name based on the order they are moved into finished. Delete them from the working tree after a release tag is created.

## Task States
Tasks must use one of these status folders:
*   **Backlog**: The task is known but not yet detailed.
*   **Designing**: Research and design are underway.
*   **Design Review**: The design is written and waiting for user approval. Research must be documented before this state.
*   **Implementing**: The task is being executed.
*   **Implementation Review**: Execution is done and waiting for user verification. Only the user can mark **Finished**.
*   **Finished**: The user has verified completion.
*   **Postponed**: The task is deferred.

## Scope and Safeguards
*   **Clarity**: Designs may describe file scope broadly when it stays unambiguous.
*   **No unapproved logic changes**: Do not change business logic unless explicitly instructed.
*   **Track modified production files**: List them in the task file.
*   **Refactor tracking**: When refactoring, document it by updating the design section of the existing task or creating a new task.
*   **Task placement**: Add new tasks as individual files in `specs/tasks/backlog/`.

## Stop and Ask
If there is confusion, contradictory instructions, or an unexpected failure that would require unplanned changes to behavior or signatures, stop and ask for guidance.

## Reverse Pair Programming
1.  **Propose** the next step at a high level.
2.  **No code snippets** during proposal.
3.  **Align** and get approval.
4.  **Act** after approval.

## Task Structure
Each task includes:
*   **Research summary**: facts and observations, preferably captured as PlantUML diagrams with notes; use text when a diagram is not sufficient.
*   **Design**: structure, data flow, constraints, and decisions, preferably captured as PlantUML diagrams with notes; use text when a diagram is not sufficient.
*   **Test specification**: brief description of planned tests before implementation.
*   **Modified files**: production and test files touched.
*   **Status**: implied by the folder; do not duplicate status in the task file.

## Architecture Decision Records
*   Record architecture decisions in `specs/architecture-decisions/` as one file per decision with meaningful names.
*   Use a short template with Title, Date, Status, Context, Decision, and Consequences.
*   Use architecture decision records for decisions that affect public behavior, dependencies, or long term design.

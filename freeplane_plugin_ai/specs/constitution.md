# Constitution

## Purpose
Keep work verifiable with a light, repeatable workflow that avoids surprises.

## Workflow
1.  **Backlog as source of truth**: All tasks, plans, and execution status live in [backlog.md](backlog.md). The current sprint is tracked there and detailed in `specs/sprints/`.
2.  **Research first**: Start with research unless the user explicitly waives it. Record findings in the task **Research summary**.
3.  **Plan and approval**: Draft the plan while research is in progress, then request approval. Do not modify code, tests, or configuration until the plan is approved.
4.  **Status updates**: Update task status before implementation and after each phase.
5.  **Information gathering**: Reading commands do not require plan approval.

## Task States
Tasks in the sprints must use one of these states:
*   **Identified**: The task is known but not yet detailed.
*   **Planning**: Research and planning are underway.
*   **Plan Review**: The plan is written and waiting for user approval. Research must be documented before this state.
*   **Plan Approved**: The plan is approved and ready for execution.
*   **Implementing**: The task is being executed.
*   **Implementation Review**: Execution is done and waiting for user verification. Only the user can mark **Finished**.
*   **Finished**: The user has verified completion.
*   **Postponed**: The task is deferred.

## Scope and Safeguards
*   **Clarity**: Plans may describe file scope broadly when it stays unambiguous.
*   **No unapproved logic changes**: Do not change business logic unless explicitly instructed.
*   **Track modified production files**: List them in the task description in `backlog.md`.
*   **Task placement**: Add new tasks to the end of the task list.

## Stop and Ask
If there is confusion, contradictory instructions, or an unexpected failure that would require unplanned changes to behavior or signatures, stop and ask for guidance.

## Reverse Pair Programming
1.  **Propose** the next step at a high level.
2.  **No code snippets** during proposal.
3.  **Align** and get approval.
4.  **Act** after approval.

## Architecture Decision Records
*   Record architecture decisions in `specs/architecture-decisions/` as one file per decision with meaningful names.
*   Use a short template with Title, Date, Status, Context, Decision, and Consequences.
*   Use architecture decision records for decisions that affect public behavior, dependencies, or long term design.

# Constitution

## Core Philosophy: The Junior Engineer
Treat the AI as a junior engineer who must submit a design doc for review before coding. The goal is correctness and "no surprises."

## Ground Rules

1.  **Explicit Research, Planning & Approval**:
    *   **Task Management**: All tasks, plans, and execution status are tracked in [backlog.md](backlog.md).
    *   **Research & Status Quo**: All data required for planning and the current status quo must be added to the task description in [backlog.md](backlog.md) before planning starts.
    *   **Research Initiation**: Research is required for each task unless the user explicitly approves skipping it. The AI proposes a research plan and asks for user approval and input. If the user already provided relevant research inputs or scope in the request, the AI may start research using that information without an extra prompt.
    *   **Before any code, test, or configuration file is created, deleted or modified**, the plan must be added to the relevant section in [backlog.md](backlog.md).
    *   The plan must be **explicitly approved by the user** before execution.
    *   Information gathering commands do **not** require a formal plan entry or prior approval. They are considered part of the analysis process.

2.  **Standard Workflow: Research, Plan, Implement**:
    *   **Start with research**: Always begin with research unless the user explicitly waives it. Record research findings directly in the task’s **Research summary** section and update it as you go.
    *   **Plan alongside research**: Research and planning are iterative and can be developed together, but the plan cannot be marked ready for approval until the research summary is documented.
    *   **Plan approval**: Propose the implementation plan and obtain explicit user approval. Tests and acceptance criteria belong in the plan.
    *   **Implement**: Execute the approved plan and move the task to implementation review.

3.  **Scope Definition**:
    *   Plans do not need to list every single file if the scope can be accurately described using general terms (e.g., "all files in `src/main/java/com/example/`").
    *   The key requirement is **clarity**: The user must understand exactly what will happen.

4.  **Task States**:
    *   Tasks in [backlog.md](backlog.md) must have one of the following states:
        *   **Identified**: The task is identified but not yet detailed.
        *   **Planning**: The task is currently being planned.
        *   **Plan Review**: The plan is written and waiting for user approval. Research must be documented in the task’s **Research summary** before reaching this state.
        *   **Plan Approved**: The plan is approved and ready for execution.
        *   **Implementing**: The task is currently being executed.
        *   **Implementation Review**: The execution is done and waiting for user verification. The AI cannot transition a task to **Finished**; only the user can do that after review.
        *   **Finished**: The task is completed and verified.
        *   **Postponed**: The task is deferred.
    *   **Research And Plan Tracking**: Each task maintains **Research summary** and **Plan** sections from **Identified** onward. Research and planning can progress together, but **Plan Review** is allowed only after the research summary has initial entries. There is no separate research state; research updates stay within the task sections.
    *   **AI Responsibility**: The AI must update the task status in `backlog.md` before starting implementation, and after finishing each phase. Research and planning progress are tracked in the task’s **Research summary** and **Plan** sections.
    *   **New Tasks**: When creating a new task, assign it the status **Identified**.
    *   **Task Placement**: Always add new tasks to the **end** of the task list

5.  **Stop & Ask**:
    *   If you are puzzled, confused, or unsure about anything (e.g., missing files, contradictory instructions, unexpected errors), **immediately stop all activities**.
    *   **Unexpected Changes**: If a compilation error or test failure requires changing a method signature, interface, or behavior that was not explicitly planned, **STOP**. Do not "fix" it. Report it.
    *   Report the issue to the user and ask for clarification.
    *   Do not guess or proceed with assumptions.

6.  **Production Code Integrity**:
    *   **No Logic Changes**: Do not change business logic unless explicitly instructed.
    *   **Track Modifications**: Keep a list of all modified production files in the task description within `backlog.md`.
    *   **Verification**: After implementation, the state must be set to **Implementation Review**. Do not set it to **Finished** yourself. Wait for the user to verify and approve the completion.

7.  **Reverse Pair Programming**:
    *   **Roles**: User is the **Navigator** (Strategy/Approval). AI is the **Driver** (Implementation/Proposal).
    *   **The Process**:
        1.  **Propose**: The AI suggests the next step as a **high-level idea** (e.g., "Refactor the DAO to use Spring Data to remove the Guice dependency").
        2.  **No Snippets**: Do not show code snippets or diffs during the proposal. Code here means programming language or markup language. Focus on the *intent* and *reasoning*.
        3.  **Align**: The AI asks for approval.
        4.  **Act**: Only after approval does the AI write the code.

8.  **Architecture Decision Records**:
    *   Record architecture decisions in `specs/architecture-decisions/` as a separate file per decision.
    *   Use meaningful file names without numbers.
    *   Use a short template that includes Title, Date, Status, Context, Decision, and Consequences.
    *   Capture decisions that affect design, tooling, or public behavior before implementation begins.

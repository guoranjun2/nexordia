Freeplane 1.13 – configuration note

Freeplane 1.13 does not introduce changes to the core configuration format.
The only functional addition is the AI integration.

AI features are disabled by default and become active only after explicit user opt-in.
As long as AI is not enabled, Freeplane 1.13 behaves identically to 1.12.x and therefore
continues to use the existing configuration directory 1.12.x.

This directory (1.13.x) is created intentionally only to document this decision.

If future versions introduce configuration changes that require a separate profile,
this will be handled explicitly and transparently.

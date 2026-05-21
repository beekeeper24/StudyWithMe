# Harness Composition Policy

Future Codex sessions should use the project harness stack proactively.

The user does not need to ask for `OMX team`, sub-agents, or orchestration on substantial work. Decide from task shape and risk.

Planning rule:

- do not run OMX planning and Superpowers planning by default;
- pick one lead planning harness;
- use OMX-led planning when requirements, acceptance criteria, boundaries, or agent work-splitting are unclear;
- use Superpowers-led planning when the goal is clear but the engineering method, TDD shape, implementation order, or debugging discipline needs structure;
- combine both only when the second harness answers a different question, not when it repeats the same planning step.

Use main Codex flow for:

- trivial edits;
- one-file fixes;
- formatting-only changes;
- obvious local corrections.

Use OMX team/orchestration when at least two are true:

- there are 2+ independent workstreams;
- implementation, tests, docs, review, or research can proceed without file conflicts;
- code spans multiple modules or security boundaries;
- a reviewer can inspect risk while implementation continues;
- runtime/browser verification can run separately from code edits.

Use Superpowers for method:

- `brainstorming` or `writing-plans` for new features and behavior changes;
- `test-driven-development` for auth, authorization, token handling, data migration, concurrency, and other high-risk logic;
- `systematic-debugging` when root cause is unclear;
- `verification-before-completion` before claiming non-trivial work is complete.

Use gstack only in the scoped ways this project wants:

- `cso` / `/cso` for security-sensitive work;
- do not run broad gstack review, QA, product, or release workflows unless the user expands scope.

Use Compound Engineering after review or meaningful implementation to capture:

- repeated mistakes;
- missed assumptions;
- reusable project rules;
- review lessons;
- repetition-prevention notes.

For StudyWithMe, a good default for substantial backend work is:

1. choose one lead planning harness: OMX for unclear requirements/work split, Superpowers for engineering method;
2. split independent work with OMX team/orchestration when useful;
3. implement and integrate in main Codex;
4. run focused tests or full build;
5. run `cso` for auth/security-sensitive changes;
6. update `docs/learnings/` and the Notion work log when the work is meaningful.

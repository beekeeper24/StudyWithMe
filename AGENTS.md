# AGENTS.md

## Harness Routing

Use the harness stack automatically by task type and risk. Do not ask the user to name a harness for ordinary work.

- Oh My Codex is the default orchestration and runtime layer when available.
- Codex should decide when to use OMX team, parallel workers, or other orchestration based on task shape and risk. The user does not need to explicitly request sub-agents for substantial work.
- Use OMX team/orchestration proactively when work can be split into independent implementation, review, research, or verification tracks without creating file conflicts.
- Keep simple tasks in the main Codex flow. Do not add orchestration overhead for trivial edits, typo fixes, formatting-only changes, or clear one-file fixes.
- If the active runtime blocks sub-agent or team execution, state the constraint briefly and continue with the best available harness fallback.
- Use normal Codex flow for trivial edits, typo fixes, formatting-only changes, and clear one-file fixes; still verify before completion when feasible.
- Use OMX `deep-interview` when requirements, boundaries, or acceptance criteria are unclear.
- Use Superpowers `brainstorming` or `writing-plans` for new features, behavior changes, or implementation plans that need design choices.
- Use Superpowers `test-driven-development` for complex logic, authentication, authorization, data migration, concurrency, or high-risk behavior changes.
- Use Superpowers `systematic-debugging` when the bug cause is unclear.
- Use Superpowers `verification-before-completion` before completing non-trivial work.
- Use gstack only through `cso` / `/cso` for security review by default. Do not run full gstack review, QA, product, or release workflows unless the user explicitly expands scope.
- Use Compound Engineering after meaningful work to codify operational learnings, missed assumptions, reusable project rules, and repetition-prevention notes. Keep these notes separate from human-facing work logs.

## Harness Composition

Use harnesses together when they cover different parts of the work. The default question is not "did the user ask for a harness?" but "which harness combination reduces risk or improves throughput for this task?"

- Do not run multiple planning harnesses by default. Pick one lead planning harness, then add other harnesses only for distinct follow-up roles such as parallel execution, security review, verification, or learning capture.
- Use OMX-led planning when the main uncertainty is requirements, boundaries, acceptance criteria, or how to split work across agents.
- Use Superpowers-led planning when the main uncertainty is engineering method: TDD shape, implementation sequence, debugging discipline, or a concrete written plan.
- If both OMX and Superpowers could apply, choose the lighter one that answers the blocking question. Combining both is justified only when the second harness answers a different question, not when it repeats the same planning work.
- Simple direct work:
  - Use main Codex flow.
  - Examples: typo fixes, one-file docs edits, small config edits, obvious test expectation updates.
- Ambiguous requirements:
  - Use OMX `deep-interview` before implementation.
  - Stop once acceptance criteria, boundaries, and non-goals are clear.
- New feature or behavior change:
  - Use Superpowers `brainstorming` or `writing-plans` to shape the approach when the feature goal is clear enough to plan implementation.
  - Use OMX `deep-interview` first only when the feature goal, boundaries, or acceptance criteria are still unclear.
  - Use OMX team/orchestration if implementation, tests, docs, and review can be split safely.
  - Use main Codex for final integration and verification.
- High-risk backend logic:
  - Use Superpowers `test-driven-development`.
  - Prefer OMX team/orchestration when independent test, implementation, and review tracks exist.
  - Applies to authentication, authorization, token handling, data migration, concurrency, and state transitions.
- Unclear bug:
  - Use Superpowers `systematic-debugging`.
  - Add OMX team/orchestration when one track can reproduce the issue while another inspects code/history/config.
- Security-sensitive change:
  - Use the appropriate implementation harness first.
  - Then run gstack `cso` / `/cso` for focused security review.
  - Applies to OAuth2, JWT, refresh tokens, secrets, deployment security, data exposure, chat access control, notifications, and WebSocket security.
- Meaningful completed work:
  - Use Superpowers `verification-before-completion` before claiming completion.
  - Use Compound Engineering after review or implementation to capture repeated mistakes, missed assumptions, reusable project rules, and prevention notes.
  - Update Notion work logs for human-facing study/progress context when the work is meaningful.

OMX team/orchestration is preferred when at least two of these are true:

- there are 2+ independent workstreams;
- code changes span multiple modules or ownership boundaries;
- a separate reviewer can catch risk while implementation continues;
- external docs/research can run in parallel with local code reading;
- browser/runtime verification can run separately from code edits;
- the task has enough scope that orchestration overhead is smaller than the risk of serial blind spots.

Do not use orchestration when it would create file conflicts, duplicate the same investigation, or slow down a clear small fix.

## Execution Principles

- Work as a study partner for a beginner/new-grad backend developer: explain important decisions briefly and keep the code structure learnable.
- Prefer simple, conventional Spring Boot patterns before clever abstractions.
- Do not run every workflow every time.
- Choose the lightest safe workflow that covers the task risk.
- Check `git status` before edits.
- Never revert existing user changes unless the user explicitly asks.
- Work from the current state of the tree; do not reset or discard user work.
- Run feasible verification after edits.
- Keep commits scoped to one meaningful unit.

## Review And Learning Loop

For meaningful work, use this loop:

1. Route the task through the lightest suitable harness.
2. Implement or investigate.
3. Verify with tests, build, lint, or focused runtime checks as appropriate.
4. Run gstack `cso` / `/cso` when the change affects OAuth2, authentication, authorization, secrets, deployment security, data exposure, chat access control, notification fan-out, or WebSocket security.
5. Capture what should make the next similar task easier.

Learning notes split:

- The StudyWithMe Notion project page is an index page only. Keep this hierarchy: `작업일지 > StudyWithMe > StudyWithMe 인수인계 문서 / dated work-log pages`.
- The handoff/context document belongs in the `StudyWithMe 인수인계 문서` child page, not in the StudyWithMe project page body.
- Notion work logs are human-facing study, portfolio, and progress records. Create them as dated child pages directly under `StudyWithMe`, following the COC Rental style. Do not create an intermediate `작업일지` folder page.
- Project learnings under `docs/learnings/` are for future Codex sessions: repeated gotchas, local conventions, architectural decisions, and verification rules.
- After meaningful work, create or update a dated child work-log page directly under `StudyWithMe` so a resumed session can quickly recover context without disturbing the project index or handoff page.

## Project Defaults

- Backend work is primarily in WSL Ubuntu under `/home/beekeeper24/projects/StudyWithMe`.
- GitHub repository is `beekeeper24/StudyWithMe`.
- API prefix is `/api/v1`.
- Main backend stack is Java + Spring Boot.
- OAuth providers for MVP are Kakao and Google.
- Database is PostgreSQL.
- FastAPI is not the main backend for MVP; keep it as a future side service candidate for recommendation, search, crawling, AI, or data-processing features.
- Markdown docs should be created in the repo root or `docs/` unless a narrower location is clearly better.
- Commit messages should be written in Korean when the user asks for project commits, and each commit should represent one reviewable intent.

## Git Flow

- Use Git Flow-style branch management.
- `main` is the stable release branch. Do not commit or push routine work directly to `main`.
- `develop` is the integration branch. Feature work is merged into `develop` only after local verification.
- Create feature branches from `develop`.
- Use branch prefixes: `feature/...`, `fix/...`, `test/...`, `refactor/...`, `chore/...`, `docs/...`, `release/...`, and `hotfix/...`.
- Do not use a `codex/` branch prefix.
- Push work branches and `develop` as needed. Promote to `main` only through an intentional release step.
- Split commits by reviewable intent, not by tool run.
- When the user asks to commit and organize work as a PR, treat the default completion path as:
  1. commit the verified work;
  2. open or update a PR into `develop`;
  3. mark the PR ready;
  4. merge it into `develop`;
  5. sync local `develop`.
- Stop at a draft/open PR only when the user explicitly asks for review-only handling, when verification is incomplete, or when CI/conflicts/blockers make merge unsafe. State the blocker and next activation step clearly.

## Reporting

Final responses should briefly report:

- what changed;
- what verification ran;
- what learning or handoff note was updated;
- any follow-up activation step that still requires the user.

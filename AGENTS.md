# AGENTS.md

## Harness Routing

Use the harness stack automatically by task type and risk. Do not ask the user to name a harness for ordinary work.

- Oh My Codex is the default orchestration and runtime layer when available.
- Use normal Codex flow for trivial edits, typo fixes, formatting-only changes, and clear one-file fixes; still verify before completion when feasible.
- Use OMX `deep-interview` when requirements, boundaries, or acceptance criteria are unclear.
- Use Superpowers `brainstorming` or `writing-plans` for new features, behavior changes, or implementation plans that need design choices.
- Use Superpowers `test-driven-development` for complex logic, authentication, authorization, data migration, concurrency, or high-risk behavior changes.
- Use Superpowers `systematic-debugging` when the bug cause is unclear.
- Use Superpowers `verification-before-completion` before completing non-trivial work.
- Use gstack only through `cso` / `/cso` for security review by default. Do not run full gstack review, QA, product, or release workflows unless the user explicitly expands scope.
- Use Compound Engineering after meaningful work to codify operational learnings, missed assumptions, reusable project rules, and repetition-prevention notes. Keep these notes separate from human-facing work logs.

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

## Reporting

Final responses should briefly report:

- what changed;
- what verification ran;
- what learning or handoff note was updated;
- any follow-up activation step that still requires the user.

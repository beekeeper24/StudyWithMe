# 0030 GitHub Actions CI Baseline

Date: 2026-05-25

## Context

StudyWithMe had no GitHub Actions workflows. PRs were merged after local verification only, so regressions could slip through when local environments differed from GitHub runners.

## Decision

Add repository-level CI first, before CD:

- Backend repo: `.github/workflows/backend-ci.yml`
  - runs on PRs and pushes to `develop` and `main`;
  - uses Java 21;
  - runs `./gradlew test --no-daemon --console=plain`.
- Frontend repo: `.github/workflows/frontend-ci.yml`
  - runs on PRs and pushes to `develop` and `main`;
  - uses Node.js 24;
  - runs `npm ci`, `npm run lint`, and `npm run build`.

## GitHub Token Gotcha

Pushing workflow files requires a token with the `workflow` scope. Without it, GitHub rejects the push:

```text
refusing to allow a Personal Access Token to create or update workflow `.github/workflows/...` without `workflow` scope
```

Refresh auth with:

```bash
gh auth refresh -h github.com -s workflow
```

## Frontend Lockfile Gotcha

GitHub runner used Node `24.15.0` and npm `11.12.1`. The existing lockfile passed locally with npm `11.6.2`, but failed on the runner because optional peer package entries were missing.

Fix by regenerating the lockfile with the runner npm version:

```bash
npx npm@11.12.1 install --package-lock-only
npx npm@11.12.1 ci
```

## Remaining Manual Step

CI now runs, but branch protection is not confirmed from local code. To make checks mandatory, configure GitHub branch protection/rulesets for `develop`:

- require PR before merge;
- require `Gradle Test` on the backend repo;
- require `Lint and Build` on the frontend repo.

## Verification

- Backend PR #44 ran `Backend CI / Gradle Test` successfully.
- Frontend PR #12 ran `Frontend CI / Lint and Build` successfully after lockfile sync.
- Local verification:
  - `./gradlew test --no-daemon --console=plain`
  - `npx npm@11.12.1 ci && npm run lint && npm run build`

# 0033 Study Structured Fields

Date: 2026-05-25

## Context

The frontend study creation screen already asked for progress method, target members, and rules, but it serialized those fields into one `description` string. That made the backend contract weak and forced the frontend to parse display data back out of prose.

## Decision

Promote recruitment details into explicit study fields:

- `progressMethod`
- `targetAudience`
- `rules`
- `capacity`
- `schedule`

The database stores these as nullable columns for migration safety. New API creation can use structured fields directly. `description` remains in the response for compatibility, but new frontend code treats it only as a fallback for old rows.

Capacity is validated as at least 1 when supplied. The generated compatibility description for structured-only creates is intentionally short so it cannot exceed the existing `description` column length.

## Verification

- Backend PR: https://github.com/beekeeper24/StudyWithMe/pull/50
- Frontend PR: https://github.com/beekeeper24/StudyWithMe-Front/pull/15
- Backend CI: `Gradle Test`
- Frontend CI: `Lint and Build`
- Local backend checks:
  - `./gradlew test --tests com.studywithme.study.application.StudyServiceTest --tests com.studywithme.study.presentation.StudyControllerTest --no-daemon --console=plain`
  - `./gradlew test --no-daemon --console=plain`
- Local frontend checks:
  - `npm run lint`
  - `npm run build`
  - Playwright desktop/mobile smoke screenshots for the study create form.

## Follow-up

The next study-domain cleanup should decide whether studies need edit/update APIs, participant capacity enforcement, and richer schedule modeling. Do not keep adding more display-only fields to the frontend until the backend contract is explicit.

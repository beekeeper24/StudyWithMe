# 0034 Study Update API

Date: 2026-05-25

## Context

Study creation had moved to structured recruitment fields, but owners still had no way to correct title, rules, schedule, capacity, or target audience after creating a study.

## Decision

Add an owner-only full update endpoint:

- `PUT /api/v1/studies/{studyId}`
- request fields: `title`, `progressMethod`, `targetAudience`, `rules`, `capacity`, `schedule`
- non-owners receive `STUDY-004`
- capacity must be at least 1

The frontend reuses the existing study create form as edit mode. Owners can open edit mode from a study card or detail panel, and the form is prefilled from the selected study.

## Verification

- Backend PR: https://github.com/beekeeper24/StudyWithMe/pull/52
- Frontend PR: https://github.com/beekeeper24/StudyWithMe-Front/pull/16
- Backend CI: `Gradle Test`
- Frontend CI: `Lint and Build`
- Local backend checks:
  - RED: `StudyControllerTest` update API tests failed before implementation
  - `./gradlew test --tests com.studywithme.study.application.StudyServiceTest --tests com.studywithme.study.presentation.StudyControllerTest --no-daemon --console=plain`
  - `./gradlew test --no-daemon --console=plain`
- Local frontend checks:
  - `npm run lint`
  - `npm run build`
  - Playwright smoke check for edit-form heading, title, and capacity prefill.

## Follow-up

Study capacity is still display/contract data only. The next domain step should enforce capacity during join or explicitly defer it with a visible product decision.

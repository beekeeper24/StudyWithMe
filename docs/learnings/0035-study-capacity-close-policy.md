# 0035 Study Capacity Close Policy

Date: 2026-05-25

## Context

Study capacity had become part of the study contract, but joining did not enforce it. The product decision changed while implementing: do not build a reopen flow. A full study should disappear from the public recruitment list and stay closed.

## Decision

Capacity counts the owner plus every `JOINED` member.

- When a join fills the last seat, the study status becomes `CLOSED`.
- Further join attempts for a full study return `STUDY-007`.
- Closed studies are not shown in the public study list.
- If a participant leaves after the study is closed, the study remains closed.
- There is no reopen API and no reopen UI.
- Closed studies remain visible only as My Page history, where non-owner participants can still leave.

## Verification

- Backend PR: https://github.com/beekeeper24/StudyWithMe/pull/54
- Frontend PR: https://github.com/beekeeper24/StudyWithMe-Front/pull/17
- Backend CI: `Gradle Test`
- Frontend CI: `Lint and Build`
- Local backend checks:
  - `./gradlew test --tests com.studywithme.study.application.StudyServiceTest --tests com.studywithme.study.presentation.StudyControllerTest --no-daemon --console=plain`
  - `./gradlew test --no-daemon --console=plain`
- Local frontend checks:
  - `npm run lint`
  - `npm run build`
  - Playwright smoke check: no `재개시` action and one participant `탈퇴` action in past history.

## Follow-up

The next useful study-domain decision is whether closed studies should allow owners to edit metadata for history accuracy, or whether closed studies should become immutable except for participant leave/history actions.

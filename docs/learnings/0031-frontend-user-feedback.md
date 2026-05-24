# 0031 Frontend User Feedback

Date: 2026-05-25

## Context

The frontend had many user-action failures that only wrote to the developer activity log. Once the developer tools are hidden, normal users cannot tell whether a study join, post save, chat room load, or account action failed.

## Decision

Use a small global toast for action-level success and failure, while keeping field-level validation inline:

- API failures now throw `ApiClientError` with backend error code and HTTP status.
- Action failures call a shared request-error reporter and show a short Korean toast.
- `401` responses are treated as expired/invalid session state and clear the authenticated UI.
- Signup and nickname problems stay next to the input because the user needs to fix that exact field.
- Realtime and closed-study chat failures use the same user-visible feedback layer.

## Verification

- Frontend PR: https://github.com/beekeeper24/StudyWithMe-Front/pull/13
- CI: `Lint and Build`
- Local checks:
  - `npm run lint`
  - `npm run build`
  - Playwright screenshots for login, desktop auth-failure toast, and mobile auth-failure toast.

## Follow-up

The frontend still needs broader product polish for board/study/chat flows, but failure states should no longer depend on the hidden developer log.

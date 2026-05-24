# 0032 Community Author Contract

Date: 2026-05-25

## Context

Community screens should not expose raw member ids as the primary author label. The frontend also needs a server-backed ownership flag so edit/delete controls are rendered from authorization state, not from a guessed client-side comparison.

## Decision

Post and comment read APIs now return author display data and requester ownership:

- `authorNickname`
- `authorProfileImageUrl`
- `ownedByRequester`

Public community reads still allow unauthenticated access. When there is no requester, `ownedByRequester` is false. Authenticated reads pass the requester id into the service so the response can be personalized without changing the public API path.

The frontend now sends the access token on community reads when available, displays nicknames and avatars, and hides post edit/delete actions unless `ownedByRequester` is true.

## Verification

- Backend PR: https://github.com/beekeeper24/StudyWithMe/pull/48
- Frontend PR: https://github.com/beekeeper24/StudyWithMe-Front/pull/14
- Backend CI: `Gradle Test`
- Frontend CI: `Lint and Build`
- Local backend checks:
  - `./gradlew test --tests com.studywithme.post.presentation.PostControllerTest --tests com.studywithme.comment.presentation.CommentControllerTest --no-daemon --console=plain`
  - `./gradlew test --tests com.studywithme.websocket.WebSocketStompIntegrationTest --no-daemon --console=plain`
  - `./gradlew test --no-daemon --console=plain`
- Local frontend checks:
  - `npm run lint`
  - `npm run build`

## Follow-up

The next backend product work should clean up the study creation model. The current free-form study introduction needs structured fields such as progress method, target members, rules, capacity, schedule, and related display rules before the frontend study creation screen is polished further.

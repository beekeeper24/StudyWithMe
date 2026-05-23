# 0026 Member Nickname Onboarding

Date: 2026-05-24

## Context

OAuth provider nicknames are not the app nickname. New users should choose their StudyWithMe nickname during the first app entry, and after that point the service should not operate with a missing nickname.

## Decision

- Persist new OAuth members with `nickname = null`.
- Expose `nicknameRequired` from `GET /api/v1/auth/me`.
- Allow only onboarding auth APIs while `nicknameRequired` is true.
- Add `PUT /api/v1/auth/me/nickname` for first setup and later My Page edits.
- Keep nickname validation server-side: 2-20 chars, Korean/English letters, numbers, and underscore.

## Gotcha

After adding the server-side gate, the frontend cannot load `me`, notifications, chat rooms, and my studies in one initial `Promise.all`. For a new member, the non-`me` API calls will correctly fail with `MEMBER-004`, preventing the UI from reaching the nickname setup screen.

The correct sequence is:

1. Fetch `GET /api/v1/auth/me`.
2. If `nicknameRequired` is true, render the nickname setup screen and stop.
3. After nickname save, load the rest of the app data.

## Verification

- `./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest.rejectAuthenticatedApiBeforeNicknameSetup --no-daemon --console=plain`
- `./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain`
- `./gradlew test --no-daemon --console=plain`
- `npm run lint`
- `npm run build`
- Runtime curl check:
  - `GET /api/v1/auth/me` returns `nicknameRequired: true`;
  - app API before nickname returns `MEMBER-004`;
  - nickname save returns `nicknameRequired: false`;
  - app API after nickname succeeds.
- Browser check on `http://localhost:5173`:
  - OAuth callback token opens the nickname setup screen;
  - save button is disabled before input;
  - saving a valid nickname enters the Home screen.

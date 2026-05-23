# 0028 Member Withdrawal Rejoin

Date: 2026-05-24

## Context

OAuth sign-up testing became cumbersome because there was no way to reset a real Google/Kakao account from the UI. Deleting rows manually works locally, but the product also needs account withdrawal.

## Decision

Add authenticated member withdrawal:

- `DELETE /api/v1/auth/me`
- Soft delete the member with `WITHDRAWN`.
- Anonymize unique identity fields so the same OAuth account can sign up again:
  - email;
  - nickname;
  - OAuth subject;
  - profile image.
- Revoke all refresh tokens for the member.
- Clear the refresh cookie.
- Reject existing access tokens for withdrawn members in the auth/account gate.

## Why Not Hard Delete

Members are referenced by posts, comments, studies, notifications, chat rooms, and chat messages. Hard delete would either fail on foreign keys or require broad cascading policy before the product rules are clear.

Soft delete plus anonymization keeps history intact while freeing the OAuth unique key for rejoin/testing.

## Frontend Rule

Expose `회원 탈퇴` from My Page. After a successful withdrawal:

1. disconnect realtime;
2. clear access token/profile/app state;
3. return to the login screen.

## Verification

- TDD RED/GREEN:
  - withdrawal anonymizes the member and revokes refresh tokens;
  - same OAuth subject can sign up again;
  - old access token is rejected after withdrawal.
- `./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain`
- `./gradlew test --no-daemon --console=plain`
- `npm run lint`
- `npm run build`
- Runtime curl:
  - `DELETE /api/v1/auth/me` returns success and clears cookie;
  - old access token returns `AUTH-003`;
  - original OAuth subject can be inserted again after withdrawal.
- Browser:
  - My Page `회원 탈퇴` returns to login screen.

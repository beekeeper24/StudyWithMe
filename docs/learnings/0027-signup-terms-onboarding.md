# 0027 Signup Terms Onboarding

Date: 2026-05-24

## Context

The first nickname onboarding looked like a direct login because a new OAuth account could enter the app after only setting a nickname. A real sign-up step should save the app nickname and required terms agreement together.

## Decision

- Add member terms agreement fields:
  - `terms_agreed_at`
  - `terms_version`
  - `privacy_policy_version`
- Add `PUT /api/v1/auth/me/signup` for first sign-up completion.
- Require both `termsAgreed` and `privacyPolicyAgreed`.
- Return `signupRequired`, `nicknameRequired`, and `termsAgreementRequired` from `GET /api/v1/auth/me`.
- Continue blocking `/api/v1/**` for signup-required users except onboarding auth APIs.
- Keep `PUT /api/v1/auth/me/nickname` for My Page nickname changes, not as the primary first sign-up path.

## Migration Rule

V11 backfills existing members that already have a nickname with `LEGACY` terms versions. This prevents previously-created local/test users from being blocked by the new sign-up gate.

New OAuth members are still created with both nickname and terms fields empty.

## Frontend Rule

The app shell must be hidden while `signupRequired` is true. The first screen should be a sign-up screen with:

- nickname input;
- service terms required checkbox;
- privacy policy required checkbox;
- disabled completion button until all required inputs are present.

## Verification

- Backend RED/GREEN targeted tests:
  - sign-up completion succeeds only with nickname and both required agreements;
  - missing required agreement returns `MEMBER-005`;
  - nickname-only members are still blocked from app APIs.
- `./gradlew test --tests com.studywithme.auth.presentation.AuthControllerTest --no-daemon --console=plain`
- `./gradlew test --no-daemon --console=plain`
- `npm run lint`
- `npm run build`
- Runtime curl:
  - `GET /api/v1/auth/me` returns `signupRequired: true`;
  - app API before sign-up returns `MEMBER-004`;
  - missing privacy agreement returns `MEMBER-005`;
  - sign-up completion returns all required flags false;
  - app API after sign-up succeeds.
- Browser:
  - `회원가입` screen appears for a new OAuth member;
  - completion button stays disabled until nickname and both checkboxes are set;
  - completion enters Home.

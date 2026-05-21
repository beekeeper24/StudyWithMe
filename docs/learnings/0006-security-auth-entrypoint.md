# Security Auth Entrypoint Policy

When connecting Spring Security to the StudyWithMe auth baseline, keep normal API authentication JWT-only.

Rules:

- Read access tokens from `Authorization: Bearer <access-token>`.
- Do not use refresh-token DB lookups during normal API requests.
- Treat structurally incomplete JWTs as invalid credentials. For example, missing `roles` claims should return `AUTH-003`, not a server error.
- Do not persist OAuth login authentication in the HTTP session.
- If OAuth login needs a session for provider state, keep it temporary and store no post-login API identity in it.
- Return the existing error envelope with `AUTH-003` for missing or invalid credentials on protected endpoints.
- Deny routes by default. Add future public routes explicitly in `SecurityConfig`.

Token delivery:

- OAuth success and refresh responses return access-token-only JSON.
- Do not put access or refresh tokens in redirect query strings.
- Deliver refresh tokens through a cookie:
  - `HttpOnly` so frontend JavaScript cannot read it;
  - `Secure` in production so it only travels over HTTPS;
  - `SameSite=Lax` by default, or `SameSite=None; Secure` only when frontend/API are intentionally cross-site.
- Response bodies may include access-token data but must not include the raw refresh token.
- Logout should revoke the matching DB refresh token when present and clear the cookie.

Production activation rules:

- Set `JWT_SECRET` through the runtime environment or deployment secret manager.
- Keep Google/Kakao client id and client secret outside git.
- Register provider redirect URIs before browser-testing real OAuth login.

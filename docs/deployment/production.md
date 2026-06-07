# Production Deployment Checklist

StudyWithMe production deployment uses the Spring `prod` profile and explicit
frontend origin, OAuth callback, JWT secret, and refresh-token cookie settings.

## Backend Environment

Required for production:

```bash
SPRING_PROFILES_ACTIVE=prod,oauth
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<long-random-secret>
APP_CORS_ALLOWED_ORIGINS=https://<frontend-domain>
OAUTH_SUCCESS_FRONTEND_REDIRECT_URI=https://<frontend-domain>/auth/callback
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
KAKAO_CLIENT_ID=<kakao-client-id>
KAKAO_CLIENT_SECRET=<kakao-client-secret>
REFRESH_TOKEN_COOKIE_SECURE=true
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
```

Use `REFRESH_TOKEN_COOKIE_SAME_SITE=Lax` when the frontend and API are same-site,
for example subdomains under the same registrable domain.

Use `REFRESH_TOKEN_COOKIE_SAME_SITE=None` only when the browser must send the
refresh-token cookie on cross-site requests, for example a Vercel frontend on one
site and an API deployed on a different site. `SameSite=None` must be paired with
`REFRESH_TOKEN_COOKIE_SECURE=true`.

## Frontend Environment

Required for a deployed frontend:

```bash
VITE_API_BASE_URL=https://<api-domain>
VITE_WS_URL=wss://<api-domain>/ws
```

The frontend API client already sends requests with `credentials: 'include'`, so
the browser can include the HttpOnly refresh-token cookie when CORS and SameSite
settings allow it.

## CORS And Cookie Rules

- `APP_CORS_ALLOWED_ORIGINS` must contain the exact frontend origin. Do not use
  `*` with credentialed requests.
- `SecurityConfig` enables credentialed CORS with `allowCredentials=true`.
- Cross-site refresh requests need all three settings to agree:
  - frontend `fetch` credentials enabled;
  - backend CORS allows the exact frontend origin with credentials;
  - refresh cookie uses `SameSite=None; Secure`.

## OAuth Provider Redirect URIs

Register backend OAuth callback URLs with each OAuth provider:

```text
https://<api-domain>/login/oauth2/code/google
https://<api-domain>/login/oauth2/code/kakao
```

Set the backend success redirect target to the frontend callback:

```bash
OAUTH_SUCCESS_FRONTEND_REDIRECT_URI=https://<frontend-domain>/auth/callback
```

## Pre-Deploy Verification

Run before promoting a production deployment:

```bash
./gradlew test
```

After deploy, verify:

- OAuth login redirects back to `/auth/callback`.
- `POST /api/v1/auth/refresh` succeeds after a browser reload.
- Browser devtools show the refresh cookie as `HttpOnly` and `Secure`.
- WebSocket connects to `wss://<api-domain>/ws` after login.

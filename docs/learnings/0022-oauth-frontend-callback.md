# 0022 OAuth Frontend Callback

Date: 2026-05-23

## Context

The backend OAuth success handler originally returned access-token JSON directly. That works for API clients but leaves a browser user on the backend JSON response instead of returning them to the React app.

## Decisions

- On OAuth success, keep issuing the refresh token as an HttpOnly cookie.
- Redirect the browser to the frontend callback URL.
- Put access-token fields in the URL fragment, not the query string, so the callback request does not send the token to the frontend server.
- Keep the access token in frontend memory state. Do not store it in `localStorage` for the MVP.
- Use `POST /api/v1/auth/refresh` with `credentials: "include"` to recover an access token after reload.

## Local Configuration

Default callback:

```text
http://localhost:5173/auth/callback
```

If Vite falls back to port `5174`, start the backend with:

```bash
OAUTH_SUCCESS_FRONTEND_REDIRECT_URI=http://localhost:5174/auth/callback ./gradlew bootRun
```

## Verification Rule

When changing OAuth success handling, refresh cookies, or frontend callback parsing, run:

```bash
./gradlew test --tests OAuth2AuthenticationSuccessHandlerTest --tests AuthControllerTest
npm run lint
npm run build
```

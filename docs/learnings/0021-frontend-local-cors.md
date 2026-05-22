# 0021 Frontend Local CORS

Date: 2026-05-22

## Context

The React/Vite frontend runs from a different browser origin than the Spring Boot API during local development. Without explicit CORS and WebSocket allowed-origin configuration, browser REST calls and the `/ws` handshake can fail even when curl or backend tests pass.

## Decisions

- Keep local defaults narrow: `http://localhost:5173` and `http://127.0.0.1:5173`.
- Configure both HTTP CORS and STOMP endpoint allowed origins from `app.cors.allowed-origins`.
- Keep `allowCredentials=true` because refresh-token cookies are part of the auth design.
- Do not use wildcard origins with credentialed browser requests.

## Verification Rule

When changing frontend origin, security filter configuration, or WebSocket endpoint registration, run:

```bash
./gradlew test --tests CorsConfigurationTest --tests WebSocketStompIntegrationTest
```

This checks that browser preflight still works and that the STOMP integration path still connects.

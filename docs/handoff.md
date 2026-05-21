# StudyWithMe Handoff

Last updated: 2026-05-21

## Read This First

This document is the local handoff note for the next Codex/OMX session.

When resuming work:

1. Read `AGENTS.md`.
2. Check `git status --short --branch`.
3. Read this file.
4. Check the latest Notion page under `작업일지 > StudyWithMe`.
5. Work from `develop` using Git Flow feature branches.

## Project

- Repository: `beekeeper24/StudyWithMe`
- Local WSL path: `/home/beekeeper24/projects/StudyWithMe`
- Windows UNC path: `//wsl.localhost/Ubuntu/home/beekeeper24/projects/StudyWithMe`
- Integration branch: `develop`
- Stable release branch: `main`, intentionally not used yet
- API prefix: `/api/v1`
- Main backend: Java 21, Spring Boot 3.5.14
- Database: PostgreSQL
- OAuth providers for MVP: Google, Kakao

## Product Goal

StudyWithMe is a study community backend.

The core portfolio point is not simple CRUD. The service connects study recruitment, board communication, comments, replies, mentions, private chat, study chat, and notifications.

The important backend story is event-driven communication:

- user action occurs;
- domain policy decides who should be notified;
- duplicate/self notifications are prevented;
- notification/read state and later real-time delivery are handled consistently.

## Current Architecture Decisions

- Use a modular monolith first.
- Keep domain package boundaries simple and learnable.
- Use pragmatic clean architecture ideas, but do not over-abstract early.
- Add ports/adapters selectively later for external integrations such as OAuth providers, push, maps, or AI services.
- Use common response and error envelopes.
- Error codes follow `DOMAIN-XXX`, for example `AUTH-001` and `GLOBAL-400`.
- Use JWT for short-lived access tokens.
- Use opaque refresh tokens with DB-stored hashes and rotation.

## Current Implemented State

Completed and merged into `develop`:

1. Common API response and global exception shape.
2. PostgreSQL, Flyway, member schema, and member repository baseline.
3. OAuth login baseline for Google/Kakao profile normalization and member upsert.
4. JWT access token and DB hash based refresh token rotation baseline.
5. HTTP authentication entrypoint baseline:
   - Spring Security filter chain;
   - JWT `Authorization: Bearer ...` authentication filter;
   - OAuth success handler that issues a token pair;
   - `GET /api/v1/auth/me`.

Known merged PRs:

- PR #1: `feature/common-api-response`
- PR #2: `feature/postgres-flyway-setup`
- PR #3: `feature/oauth-login-baseline`
- PR #4: `feature/jwt-refresh-token-baseline`
- Current branch work: `feature/security-auth-entrypoint`

## Important Local State

At the time this handoff was written:

- branch should be `develop` tracking `origin/develop`;
- `gradlew` may appear modified only because its file mode changed from executable to non-executable;
- do not revert that user/environment change unless the user explicitly asks;
- Docker Postgres may already be running as `studywithme-postgres`.

Local defaults:

- app port: `8081`
- PostgreSQL host port: `15432`
- database: `studywithme`
- username: `studywithme`
- password: `studywithme`
- access token TTL: `30m`
- refresh token TTL: `14d`

## Current Security/Token Policy

Access token:

- JWT;
- HS256;
- short-lived;
- default TTL `30m`;
- contains issuer, member id subject, and roles;
- normal API calls should validate this without DB refresh-token lookup.

Refresh token:

- opaque random token;
- raw token is returned only to the client;
- SHA-256 hash is stored in DB;
- default TTL `14d`;
- refresh rotates the token;
- old refresh token row gets `rotated_at`;
- revoked, rotated, expired, or unknown tokens are rejected.

This design keeps normal API calls fast while preserving server-side control for reissue, logout, and token theft response.

## Current HTTP Auth Policy

Security filter chain:

- `GET /api/v1/auth/me` requires authentication.
- Access tokens are read from `Authorization: Bearer <access-token>`.
- Valid JWT claims become an `AuthenticatedMemberPrincipal` in the request `SecurityContext`.
- Invalid or missing credentials for protected endpoints return the existing error envelope with `AUTH-003`.
- API authentication is intentionally JWT-only. OAuth may use an HTTP session temporarily for provider state, but Spring Security does not persist the authenticated security context into the session.
- Routes not explicitly permitted are denied by default, so new endpoints must be intentionally added to the security rules.

OAuth success token delivery for the local MVP:

- OAuth login success writes HTTP `200` JSON with `accessToken`, `refreshToken`, expiry times, and `tokenType: Bearer`.
- Tokens are not placed in redirect query strings.
- This JSON refresh-token response is only for local/manual MVP inspection.
- Before connecting a real browser frontend, move refresh-token delivery to a cookie:
  - `HttpOnly` so frontend JavaScript cannot read the refresh token;
  - `Secure` in production so the cookie is sent only over HTTPS;
  - `SameSite=Lax` by default, or `SameSite=None; Secure` only if frontend and API must run cross-site.
- After that change, OAuth/login responses should expose the access token to the frontend but not the raw refresh token in JSON.

Production secrets policy:

- Production must set a strong `JWT_SECRET` through the runtime environment or secret manager.
- Do not rely on the local `studywithme-local-development-secret-key-change-me` fallback outside local development.
- Google/Kakao client id and client secret must be configured outside git through environment variables or the deployment secret store.

## Next Work

After `feature/security-auth-entrypoint` is reviewed/merged, the next implementation tasks are:

1. Add explicit refresh/reissue and logout HTTP endpoints.
2. Implement the production refresh-token delivery policy before any real frontend launch: refresh token in `HttpOnly; Secure; SameSite` cookie, not JSON.
3. Register Google/Kakao client id and secret outside git.
4. Register provider redirect URIs.
5. Browser-test actual OAuth login end-to-end.
6. Add future public API routes to `SecurityConfig` explicitly instead of relying on defaults.

Recommended verification:

```bash
./gradlew test --no-daemon --console=plain
docker compose up -d postgres
./gradlew bootRun --no-daemon --console=plain
```

Actual OAuth browser login test should happen after:

- Google/Kakao client id and secret are configured;
- provider redirect URIs are registered;
- success handler and token delivery are implemented and provider client credentials are configured.

Expected redirect URIs:

- `http://localhost:8081/login/oauth2/code/google`
- `http://localhost:8081/login/oauth2/code/kakao`

## CLI Resume Prompt

Paste this into a fresh Codex/OMX CLI session:

```text
StudyWithMe 프로젝트 이어서 작업하자.

먼저 AGENTS.md와 docs/handoff.md를 읽고, Notion의 작업일지 > StudyWithMe > StudyWithMe 인수인계 문서와 최신 작업일지를 확인해.

현재 기준은 develop 브랜치이고 Git Flow 방식으로 feature 브랜치를 만들어 작업해야 해. main은 릴리즈 전까지 건드리지 않는다.

최근 완료된 작업:
- PR #4에서 JWT access token + DB hash 기반 refresh token 회전 구조를 develop에 merge함.
- feature/security-auth-entrypoint에서 SecurityFilterChain, JWT 인증 필터, OAuth 성공 핸들러, /api/v1/auth/me를 구현함.
- access token은 stateless JWT, refresh token은 DB 저장 hash/rotation/revoke 정책.
- local MVP에서는 OAuth 성공 시 token pair JSON을 반환하지만, frontend 연결 전 refresh token은 HttpOnly Secure SameSite cookie로 전환하기로 결정함.
- 테스트는 ./gradlew test --no-daemon --console=plain 통과.

다음 작업:
- 현재 feature/security-auth-entrypoint 변경을 리뷰하고 PR로 develop에 병합
- refresh/reissue/logout HTTP endpoint 추가
- refresh token cookie delivery 구현
- Google/Kakao client id/secret 및 redirect URI 설정 후 실제 OAuth 브라우저 로그인 검증

작업 전에 git status와 현재 브랜치를 확인하고, gradlew 권한 변경이 있으면 사용자/환경 변경으로 보고 함부로 되돌리지 마.
커밋 메시지는 한국어 Lore 프로토콜을 지키고, PR은 develop 대상으로 만들어.
```

## Work Rules To Preserve

- User is a beginner/new-grad backend developer. Explain important decisions briefly while working.
- Prefer simple Spring Boot conventions over clever abstractions.
- Use TDD for authentication, authorization, token, migration, and other high-risk behavior.
- Run feasible verification before claiming completion.
- Keep Notion structure as:
  - `작업일지 > StudyWithMe`
    - `StudyWithMe 인수인계 문서`
    - dated worklog pages directly under `StudyWithMe`
- Do not create another nested `작업일지` page under `StudyWithMe`.
- Keep repo learnings under `docs/learnings/`.

# StudyWithMe Handoff

Last updated: 2026-05-22

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
6. Refresh/reissue/logout HTTP endpoint and refresh token cookie delivery.
7. OAuth client environment-variable configuration under the `oauth` Spring profile.
8. Local OAuth `.env` helper and actual Google/Kakao browser login verification.
9. Study recruitment baseline:
   - authenticated create/join/leave/close;
   - public list/detail;
   - pessimistic write lock for join/leave/close decisions;
   - owner/member role tracking.
10. Free-board post baseline:
   - authenticated create/update/delete;
   - public list/detail;
   - author-only update/delete;
   - soft delete with `DELETED` status.
11. Comment/reply baseline:
   - authenticated comment/reply create;
   - public comment list by post;
   - author-only update/delete;
   - one-level replies only;
   - soft delete with deleted-parent reply hiding.
12. Notification outbox baseline:
   - comment/reply creation stores outbox events in the same transaction;
   - processor creates in-app notifications idempotently;
   - self-notifications are suppressed;
   - outbox retry/dead states are modeled;
   - polling worker is available but disabled by default.
13. Kafka outbox relay baseline:
   - Spring Kafka dependency and local Kafka Docker Compose service;
   - V7 Kafka publish state columns on `outbox_events`;
   - relay publishes due outbox rows to Kafka with the outbox event id as key;
   - Kafka publish retry/dead state is independent from in-app notification processing;
   - Kafka relay worker is available but disabled by default.
14. Mention notification baseline:
   - extract `@nickname` from comment/reply content;
   - resolve exact ACTIVE member nicknames;
   - store `COMMENT_MENTIONED` outbox events;
   - create `MENTIONED_IN_COMMENT` notifications;
   - replace ordinary comment/reply notifications with mention notifications for the same receiver and comment.
15. Kafka notification consumer baseline:
   - shared `OutboxKafkaEvent` envelope between relay and consumer;
   - disabled-by-default Kafka notification consumer;
   - consumer delegates to existing notification processor policy;
   - Kafka replay/retry idempotency uses outbox event id as notification source event id.
16. Chat REST MVP baseline:
   - authenticated 1:1 private chat room creation;
   - authenticated study chat room creation for study members;
   - authenticated room list for rooms joined by the requester;
   - authenticated message create/list;
   - room membership is checked before every message write/read;
   - study chat room membership syncs current study members when the room is requested.

No active feature work is currently in progress after PR #22. Start the next branch from `develop`.

- Flyway V6 notification/outbox schema:
  - `outbox_events`;
  - `notifications`.
- Outbox events:
  - `COMMENT_CREATED`
  - `REPLY_CREATED`
- Notification types:
  - `COMMENT_ON_POST`
  - `REPLY_ON_COMMENT`
- In-app notification API:
  - authenticated `GET /api/v1/notifications`;
  - authenticated `POST /api/v1/notifications/{notificationId}/read`.
- `NotificationOutboxProcessor` handles at-least-once processing.
- `NotificationOutboxWorker` is disabled by default and enabled with `app.notification.outbox.worker-enabled=true`.
- Flyway V7 Kafka relay schema adds:
  - `kafka_publish_status`;
  - `kafka_retry_count`;
  - `kafka_next_attempt_at`;
  - `kafka_published_at`;
  - `kafka_last_error`.
- `OutboxKafkaRelay` sends a JSON envelope to Kafka topic `studywithme.outbox.events` by default.
- Kafka key is the outbox event id, so downstream consumers can deduplicate at-least-once delivery.
- `OutboxKafkaRelayWorker` is disabled by default and enabled with `OUTBOX_KAFKA_RELAY_ENABLED=true`.
- `NotificationKafkaConsumer` reads the same Kafka envelope and delegates to `NotificationOutboxProcessor`.
- `NotificationKafkaConsumer` is disabled by default and enabled with `NOTIFICATION_KAFKA_CONSUMER_ENABLED=true`.
- Kafka consumer group defaults to `studywithme-notification` and can be changed with `NOTIFICATION_KAFKA_GROUP_ID`.
- During MVP transition, DB outbox polling and Kafka consumer can coexist because notification idempotency is guarded by source event id.
- Mention extraction:
  - `@nickname` exact, case-sensitive matching;
  - ACTIVE members only;
  - self-mentions suppressed;
  - duplicate mentions inside one comment/reply collapsed into one notification.
- Outbox event:
  - `COMMENT_MENTIONED`
- Notification type:
  - `MENTIONED_IN_COMMENT`

Chat REST MVP details:

- Flyway V8 schema:
  - `chat_rooms`;
  - `chat_room_members`;
  - `chat_messages`.
- Chat room types:
  - `PRIVATE`;
  - `STUDY`.
- API:
  - authenticated `POST /api/v1/chat/private-rooms`;
  - authenticated `POST /api/v1/studies/{studyId}/chat-room`;
  - authenticated `GET /api/v1/chat/rooms`;
  - authenticated `POST /api/v1/chat/rooms/{roomId}/messages`;
  - authenticated `GET /api/v1/chat/rooms/{roomId}/messages`.
- Private rooms use deterministic room keys so the same two members reuse one room.
- Study rooms use deterministic room keys by study id and sync current study members into `chat_room_members`.
- WebSocket delivery, unread counts, read receipts, chat notifications, moderation, and retention policy are not implemented yet.

Comment baseline details:

- Flyway V5 schema: `comments`.
- API:
  - authenticated `POST /api/v1/posts/{postId}/comments`;
  - public `GET /api/v1/posts/{postId}/comments`;
  - authenticated `POST /api/v1/comments/{commentId}/replies`;
  - authenticated author-only `PUT /api/v1/comments/{commentId}`;
  - authenticated author-only `DELETE /api/v1/comments/{commentId}`.
- Comment status values: `PUBLISHED`, `DELETED`.
- Replies are one level deep. Nested replies return `COMMENT-003`.
- Public lists hide deleted comments and replies whose parent comment is hidden.

Post baseline details:

- Flyway V4 schema: `posts`.
- API:
  - authenticated `POST /api/v1/posts`;
  - public `GET /api/v1/posts`;
  - public `GET /api/v1/posts/{postId}`;
  - authenticated author-only `PUT /api/v1/posts/{postId}`;
  - authenticated author-only `DELETE /api/v1/posts/{postId}`.
- Post status values: `PUBLISHED`, `DELETED`.
- Delete is a soft delete so future comments/notifications can keep a stable post reference.

Known merged PRs:

- PR #1: `feature/common-api-response`
- PR #2: `feature/postgres-flyway-setup`
- PR #3: `feature/oauth-login-baseline`
- PR #4: `feature/jwt-refresh-token-baseline`
- PR #6: `feature/security-auth-entrypoint`
- PR #7: `feature/auth-refresh-endpoints`
- PR #8: `feature/oauth-client-env-config`
- PR #9: `feature/local-oauth-env-script`
- PR #10: `feature/study-recruitment-baseline`
- PR #11: `docs/pr-merge-workflow-rule`
- PR #12: `feature/post-baseline`
- PR #13: `docs/post-merge-handoff`
- PR #14: `feature/comment-baseline`
- PR #15: `feature/notification-outbox-baseline`
- PR #16: `feature/kafka-outbox-relay`
- PR #18: `feature/mention-notification-baseline`
- PR #20: `feature/kafka-notification-consumer`
- PR #22: `feature/chat-mvp-baseline`

## Important Local State

At the time this handoff was written:

- active branch should be `develop`;
- `gradlew` may appear modified only because its file mode changed from executable to non-executable;
- do not revert that user/environment change unless the user explicitly asks;
- Docker Postgres may already be running as `studywithme-postgres`.
- Docker Kafka may already be running as `studywithme-kafka`.
- Local `.env` may exist with real OAuth credentials. It is ignored by git and must stay untracked.

Local defaults:

- app port: `8081`
- PostgreSQL host port: `15432`
- Kafka host port: `9092`
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
- Study and post public reads are explicitly permitted; mutating routes require JWT authentication.
- Comment public list is explicitly permitted; comment/reply create/update/delete require JWT authentication.
- Notification list/read routes require JWT authentication and only expose the authenticated member's notifications.
- Chat room list/create and message list/create routes require JWT authentication; message list/create also require room membership inside `ChatService`.

Refresh/reissue/logout HTTP policy:

- OAuth login success and `POST /api/v1/auth/refresh` return access-token-only JSON.
- The raw refresh token is not included in JSON response bodies.
- Refresh tokens are delivered through a cookie:
  - name: `refreshToken`;
  - path: `/api/v1/auth`;
  - `HttpOnly` so frontend JavaScript cannot read the refresh token;
  - `SameSite=Lax` by default;
  - `Secure` is configurable and must be enabled in production HTTPS.
- `POST /api/v1/auth/refresh` reads the refresh-token cookie, rotates it, writes a new refresh-token cookie, and returns the new access token.
- `POST /api/v1/auth/logout` reads the refresh-token cookie when present, revokes the matching DB refresh token, and clears the cookie.
- Tokens are not placed in redirect query strings.

Production secrets policy:

- Production must set a strong `JWT_SECRET` through the runtime environment or secret manager.
- Do not rely on the local `studywithme-local-development-secret-key-change-me` fallback outside local development.
- Google/Kakao client id and client secret must be configured outside git through environment variables or the deployment secret store.

OAuth client config:

- Real provider registrations live in `src/main/resources/application-oauth.yml`.
- The registrations are active only when the `oauth` Spring profile is enabled.
- Start local browser testing with `scripts/run-oauth-local.sh` after creating a local `.env`.
- `.env` is ignored by git. Keep real client ids/secrets there, and keep `.env.example` as the committed template only.
- Required environment variables:
  - `GOOGLE_CLIENT_ID`
  - `GOOGLE_CLIENT_SECRET`
  - `KAKAO_CLIENT_ID`
  - `KAKAO_CLIENT_SECRET`

## Next Work

Recent study branch verification:

- `./gradlew test --no-daemon --console=plain` passes.
- `git diff --check` passes.
- `docker compose up -d postgres` plus `./gradlew bootRun --no-daemon --console=plain` starts successfully.
- PostgreSQL Flyway schema history reaches version `3 - create study schema`.
- `GET /actuator/health` returns `{"status":"UP"}`.
- `bootRun` may show exit `143` after manual verification shutdown; that is expected when the agent stops the running app.

Completed local OAuth verification on 2026-05-22:

- Google browser login callback succeeded.
- Kakao browser login callback succeeded.
- Both providers created/updated ACTIVE members with USER role.
- Refresh token rows were issued and remained active.
- `GET /api/v1/auth/me` without an access token still returns `AUTH-003`.
- gstack browse could not run in this WSL/Windows setup because its Windows ACL hardening failed on the WSL UNC `.gstack` path, so browser login was verified through the user's default browser and DB checks.

Next implementation tasks:

1. Start chat MVP or real-time notification delivery from `develop`.
2. If chat comes next, fix private/study chat membership authorization before message storage.
3. If real-time notification comes next, send already-created notifications over WebSocket/SSE without bypassing DB notification records.
4. Enable `REFRESH_TOKEN_COOKIE_SECURE=true` in production HTTPS.
5. Add future public API routes to `SecurityConfig` explicitly instead of relying on defaults.

Recommended verification:

```bash
./gradlew test --no-daemon --console=plain
docker compose up -d postgres
docker compose up -d kafka
./gradlew bootRun --no-daemon --console=plain
scripts/run-oauth-local.sh
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
- feature/auth-refresh-endpoints에서 refresh/reissue/logout HTTP endpoint와 refresh token cookie delivery를 구현함.
- OAuth 성공/refresh 응답 body에는 access token만 담고, refresh token은 HttpOnly SameSite cookie로 전달함.
- feature/local-oauth-env-script에서 `.env` 기반 로컬 OAuth 실행 스크립트를 추가하고 Google/Kakao 실제 브라우저 로그인을 검증함.
- feature/study-recruitment-baseline에서 스터디 생성/목록/상세/참여/탈퇴/마감 기본 API를 구현함.
- study join/leave/close는 같은 study row에 pessimistic write lock을 걸어 상태/멤버십 결정을 직렬화함.
- feature/post-baseline에서 자유게시판 글 생성/목록/상세/수정/삭제 기본 API를 구현하고 develop에 merge함.
- 게시글 삭제는 DELETED soft delete로 처리하고, 공개 조회에서는 삭제 글을 숨김.
- feature/comment-baseline에서 게시글 댓글/1단계 답글 기본 API를 구현하고 develop에 merge함.
- 댓글 삭제는 DELETED soft delete로 처리하고, 공개 목록에서는 삭제 댓글과 삭제 부모 아래 답글을 숨김.
- feature/notification-outbox-baseline에서 댓글/답글 이벤트 outbox와 in-app notification baseline을 구현하고 develop에 merge함.
- feature/kafka-outbox-relay에서 DB outbox를 Kafka topic으로 publish하는 relay baseline을 구현함.
- Kafka relay는 domain transaction을 건드리지 않고, 별도 `kafka_publish_status`로 publish/retry/dead 상태를 관리함.
- feature/mention-notification-baseline에서 댓글/답글 `@nickname` 멘션 outbox와 mention notification baseline을 구현함.
- feature/kafka-notification-consumer에서 Kafka outbox event를 읽어 기존 notification processor에 위임하는 consumer baseline을 구현함.

다음 작업:
- chat MVP 또는 실시간 알림 전달 시작
- production HTTPS에서는 REFRESH_TOKEN_COOKIE_SECURE=true 설정

작업 전에 git status와 현재 브랜치를 확인하고, gradlew 권한 변경이 있으면 사용자/환경 변경으로 보고 함부로 되돌리지 마.
커밋 메시지는 한국어로 쓰고, PR은 develop 대상으로 만든 뒤 명시적 보류가 없으면 검증 후 develop에 머지한다.
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

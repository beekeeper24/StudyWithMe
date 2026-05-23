# StudyWithMe Handoff

Last updated: 2026-05-24

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
- Frontend repository: `beekeeper24/StudyWithMe-Front`
- Local WSL path: `/home/beekeeper24/projects/StudyWithMe`
- Frontend local WSL path: `/home/beekeeper24/projects/StudyWithMe-Front`
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
17. Chat WebSocket delivery baseline:
   - STOMP endpoint `/ws`;
   - application destination prefix `/app`;
   - simple broker topic prefix `/topic`;
   - STOMP `CONNECT` authenticates `Authorization: Bearer <access-token>`;
   - STOMP `SUBSCRIBE` and `SEND` validate chat room membership;
   - WebSocket sends persist messages through `ChatService` before publishing to room topics.
18. Notification WebSocket delivery baseline:
   - authenticated members subscribe to `/user/queue/notifications`;
   - notification outbox processing still creates DB notification rows first;
   - realtime publish runs after transaction commit;
   - server publishes with `convertAndSendToUser`;
   - client `SEND` to the notification user queue is rejected.
19. WebSocket STOMP integration verification:
   - random-port Spring Boot integration test connects to `/ws`;
   - verifies chat STOMP send/subscribe delivery with real broker frames;
   - verifies notification user queue delivery from outbox processing;
   - no extra runtime dependency was needed for the test client.
20. Frontend OAuth flow baseline:
   - OAuth success writes the refresh token as an HttpOnly cookie;
   - OAuth success redirects to the frontend callback with the access token in the URL fragment;
   - default frontend callback is `http://localhost:5173/auth/callback`;
   - override with `OAUTH_SUCCESS_FRONTEND_REDIRECT_URI` when Vite runs on another port.
21. Frontend repository baseline:
   - Vite React TypeScript app exists at `/home/beekeeper24/projects/StudyWithMe-Front`;
   - Google/Kakao OAuth login buttons call the backend OAuth authorization endpoints;
   - `/auth/callback` parses the URL fragment access token and removes the fragment from browser history;
   - refresh token stays in the backend HttpOnly cookie and is used through `credentials: "include"`;
   - README documents the backend redirect override for a non-5173 Vite port.
22. Local work-rule update:
   - `AGENTS.md` includes the Superpowers TDD rule: no happy-path-only tests, include meaningful edge cases, avoid absurd cases, and split tests by behavior/unit boundary.
23. Frontend community screen expansion:
   - frontend PR #2 expands the React app from OAuth/realtime verification console into study/post/comment/chat/notification tabs;
   - user-facing screens hide access token, backend URL, manual sync, activity log, and roomId entry behind a developer-tools toggle;
   - startup attempts refresh-cookie session recovery and then loads profile, notifications, and chat rooms;
   - study detail can open a study chat room;
   - chat tab lists the authenticated member's chat rooms and loads message history before realtime participation.
24. Frontend support API contract:
   - study list/detail responses include owner nickname/profile image and requester-specific membership flags;
   - chat room responses include a display title;
   - authenticated chat room members can query `GET /api/v1/chat/rooms/{roomId}/members`;
   - chat member list access is still guarded by room membership validation.

Active feature work in progress:

- Backend branch: `feature/my-page-study-chat-history`
- Frontend branch: `feature/my-page-study-chat-history`
- Goal:
  - public study list shows recruiting studies only;
  - authenticated `GET /api/v1/studies/me` returns active and past study history;
  - study leave preserves `study_members` history with `LEFT` and `left_at`;
  - chat room delete hides the room per requester with `chat_room_members.hidden_at`;
  - study leavers are excluded from existing study chat room lists and message access even if old `chat_room_members` rows remain;
  - frontend profile menu opens a My Page with active/past study history;
  - frontend chat room list has a room delete action.
- Verification already run on the active branches:
  - backend `./gradlew test --no-daemon --console=plain`;
  - frontend `npm run build`;
  - frontend `npm run lint`;
  - Playwright browser render check for login, authenticated My Page, and chat.
- Local runtime after this work:
  - backend is running on `8081` with the `oauth` profile;
  - frontend is running on `5173`;
  - PostgreSQL schema has V9 applied locally.
- Learning note: `docs/learnings/0025-study-history-chat-room-hide.md`.

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

Notification WebSocket delivery details:

- Client subscribes to `/user/queue/notifications`.
- STOMP `CONNECT` still requires `Authorization: Bearer <access-token>`.
- `SUBSCRIBE /user/queue/notifications` requires an authenticated principal.
- Client `SEND /user/queue/notifications` is rejected.
- `NotificationOutboxProcessor` creates the DB notification first, then schedules realtime delivery after transaction commit.
- Server publishes `NotificationResponse` with `convertAndSendToUser(receiverMemberId.toString(), "/queue/notifications", response)`.
- Realtime delivery is best-effort. Polling `GET /api/v1/notifications` remains the durable catch-up path.

WebSocket integration test:

- `src/test/java/com/studywithme/websocket/WebSocketStompIntegrationTest.java`
- Run with:
  - `./gradlew test --tests com.studywithme.websocket.WebSocketStompIntegrationTest --no-daemon --console=plain`
- Covers:
  - real STOMP `CONNECT` to `/ws` with bearer token;
  - chat subscribe/send/receive through `/topic/chat.rooms.{roomId}` and `/app/chat.rooms.{roomId}.messages`;
  - notification receive through `/user/queue/notifications` after outbox processing.

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
  - authenticated `GET /api/v1/chat/rooms/{roomId}/members`;
  - authenticated `POST /api/v1/chat/rooms/{roomId}/messages`;
  - authenticated `GET /api/v1/chat/rooms/{roomId}/messages`.
- Private rooms use deterministic room keys so the same two members reuse one room.
- Study rooms use deterministic room keys by study id and sync current study members into `chat_room_members`.
- Chat room list responses include `title`; private room titles use the other member nickname, study room titles use the study title.
- Chat room member responses expose member id, nickname, profile image URL, and joined time only to room members.
- Unread counts, read receipts, chat notifications, moderation, and retention policy are not implemented yet.

Chat WebSocket delivery details:

- Endpoint:
  - WebSocket handshake: `/ws`
  - STOMP connect header: `Authorization: Bearer <access-token>`
- Publish:
  - client sends to `/app/chat.rooms.{roomId}.messages`
  - payload: `{ "content": "..." }`
  - server stores through `ChatService.sendMessage(...)`
  - server publishes `ChatMessageResponse` to `/topic/chat.rooms.{roomId}`
- Subscribe:
  - client subscribes to `/topic/chat.rooms.{roomId}`
  - server validates room membership before allowing the subscription frame.
- Current broker is Spring's in-memory simple broker. Multi-instance deployment will need broker relay or an external fan-out strategy.
- Unread counts, read receipts, chat notifications, moderation, and retention policy are still not implemented.

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
- PR #24: `feature/chat-websocket-delivery`
- PR #25: `docs/chat-websocket-post-merge-handoff`
- PR #26: `feature/notification-websocket-delivery`
- PR #27: `docs/notification-websocket-post-merge-handoff`
- PR #28: `test/websocket-stomp-integration`
- PR #29: `docs/websocket-integration-post-merge-handoff`
- PR #30: `fix/local-frontend-cors-origin`
- PR #31: `fix/websocket-local-frontend-origin`
- PR #32: `feature/oauth-frontend-callback`
- PR #33: `docs/tdd-test-design-rule`
- PR #35: `docs/frontend-playwright-font-learning`
- PR #36: `docs/frontend-community-post-merge-handoff`
- PR #37: `feature/frontend-support-api-contract`

Frontend merged PRs:

- PR #1: `feature/oauth-login-baseline`
- PR #2: `feature/frontend-community-screens`
- PR #3: `feature/frontend-api-contract-screens`

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
- frontend dev server: `5173`
- fallback frontend dev server when another Vite process owns 5173: `5174`
- default allowed browser origins: `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:5174`, `http://127.0.0.1:5174`
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
- `/ws` handshake is permitAll, but STOMP `CONNECT` requires a bearer access token and STOMP `SUBSCRIBE`/`SEND` require chat room membership.
- Notification user queue subscription also requires STOMP authentication; clients cannot publish to the notification user queue.
- Browser CORS is enabled only for configured origins. The local default allows Vite frontend origins on ports `5173` and `5174`; production must set `APP_CORS_ALLOWED_ORIGINS` to the deployed frontend origins.
- WebSocket `/ws` uses the same configured allowed origins, while STOMP authentication still happens through the `Authorization: Bearer <access-token>` `CONNECT` header.

Refresh/reissue/logout HTTP policy:

- OAuth login success redirects to the configured frontend callback with access-token data in the URL fragment.
- `POST /api/v1/auth/refresh` returns access-token-only JSON.
- The raw refresh token is not included in JSON response bodies.
- Refresh tokens are delivered through a cookie:
  - name: `refreshToken`;
  - path: `/api/v1/auth`;
  - `HttpOnly` so frontend JavaScript cannot read the refresh token;
  - `SameSite=Lax` by default;
  - `Secure` is configurable and must be enabled in production HTTPS.
- `POST /api/v1/auth/refresh` reads the refresh-token cookie, rotates it, writes a new refresh-token cookie, and returns the new access token.
- `POST /api/v1/auth/logout` reads the refresh-token cookie when present, revokes the matching DB refresh token, and clears the cookie.
- Tokens are not placed in redirect query strings. The OAuth access token uses the URL fragment so it is not sent back to the frontend server in the callback request.

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

Completed OAuth frontend callback work on 2026-05-23:

- Backend PR #32 redirects OAuth success to the frontend callback and keeps the refresh token in an HttpOnly cookie.
- Frontend PR #1 parses the callback fragment, keeps the access token in memory state, and verifies `/me`, refresh, and logout flows.
- Backend PR #33 updates `AGENTS.md` with the TDD test-design rule requested by the user.
- Backend `develop` and frontend `develop` were clean and synced with origin after those merges.

Next implementation tasks:

1. Add authenticated frontend route guards and friendlier error states for failed create/join/comment/chat actions.
2. Add notification reconnect/polling catch-up polish beyond the current login/connect-time sync.
3. Enable `REFRESH_TOKEN_COOKIE_SECURE=true` in production HTTPS.
4. Set `APP_CORS_ALLOWED_ORIGINS` and `OAUTH_SUCCESS_FRONTEND_REDIRECT_URI` to the real frontend origin in production.
5. Add future public API routes to `SecurityConfig` explicitly instead of relying on defaults.

Frontend community screen verification already completed:

- OAuth browser E2E for Google/Kakao against backend `8081` and frontend `5174`;
- `npm run lint`;
- `npm run build`;
- `git diff --check`;
- public API smoke for `GET /api/v1/studies` and `GET /api/v1/posts`;
- Playwright desktop/mobile screenshots against `http://localhost:5174/`.

WSL Playwright Korean screenshots require Korean fonts. See `docs/learnings/0023-frontend-playwright-korean-fonts.md`.

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
- feature/chat-mvp-baseline에서 1:1/private study chat REST MVP를 구현함.
- feature/chat-websocket-delivery에서 STOMP 기반 채팅 실시간 전달을 구현함.
- feature/notification-websocket-delivery에서 `/user/queue/notifications` 실시간 알림 전달을 구현함.
- test/websocket-stomp-integration에서 실제 STOMP 프레임 기반 채팅/알림 통합 검증을 추가함.
- feature/oauth-frontend-callback에서 OAuth 성공 시 frontend callback으로 redirect하고 access token은 URL fragment, refresh token은 HttpOnly cookie로 전달하도록 변경함.
- StudyWithMe-Front PR #1에서 Vite React TS 프론트 baseline과 Google/Kakao OAuth callback 처리를 구현함.
- docs/tdd-test-design-rule에서 Superpowers TDD 규칙을 AGENTS.md에 추가함.

다음 작업:
- OAuth 브라우저 E2E를 backend/frontend dev server로 재검증
- frontend study/post/community 화면 확장
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

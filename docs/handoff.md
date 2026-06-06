# StudyWithMe Handoff

Last updated: 2026-06-05

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
   - login-required list/detail;
   - pessimistic write lock for join/leave/close decisions;
   - owner/member role tracking.
10. Free-board post baseline:
   - authenticated create/update/delete;
   - login-required list/detail;
   - author-only update/delete;
   - soft delete with `DELETED` status.
11. Comment/reply baseline:
   - authenticated comment/reply create;
   - login-required comment list by post;
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
25. My Page study history and chat room hiding:
   - login-required study list shows recruiting studies only;
   - authenticated `GET /api/v1/studies/me` returns active and past study history;
   - study leave preserves `study_members` history with `LEFT` and `left_at`;
   - chat room delete hides the room per requester with `chat_room_members.hidden_at`;
   - study leavers are excluded from existing study chat room lists and message access even if old `chat_room_members` rows remain;
   - frontend profile menu opens a My Page with active/past study history;
   - frontend chat room list has a room delete action.
26. Member nickname onboarding:
   - new OAuth members are created with `nickname = null` instead of using provider nickname automatically;
   - `GET /api/v1/auth/me` returns `nicknameRequired`, `termsAgreementRequired`, and `signupRequired`;
   - authenticated `PUT /api/v1/auth/me/signup` completes first sign-up by saving nickname and required terms agreement together;
   - authenticated `PUT /api/v1/auth/me/nickname` changes the member nickname after sign-up;
   - nickname rules are 2-20 chars, Korean/English letters, numbers, and underscore only;
   - duplicate nicknames are rejected;
   - signup-required members can only call onboarding auth APIs until nickname and terms agreement are saved;
   - frontend gates the app shell behind a sign-up screen and supports nickname edits from My Page.
27. Member withdrawal:
   - authenticated `DELETE /api/v1/auth/me` withdraws the current member;
   - withdrawal soft-deletes the member with `WITHDRAWN` status;
   - member email, nickname, profile image, and OAuth subject are anonymized so the same OAuth account can sign up again;
   - all refresh tokens for the member are revoked and the refresh cookie is cleared;
   - existing access tokens for withdrawn members are rejected by the authentication/account gate;
   - frontend My Page exposes a `회원 탈퇴` button for local testing and normal user flow.
28. Closed study chat and operational UI cleanup:
   - backend rejects new messages to study chat rooms whose linked study is `CLOSED`;
   - REST and WebSocket message writes are both covered because both call `ChatService.sendMessage`;
   - closed study chat rooms can remain visible for history, but the frontend disables the composer when the closed study is known;
   - user-facing refresh controls were removed from profile menu, My Page, study list, chat room list, and notification popup;
   - account withdrawal now uses an in-app confirmation panel instead of browser `confirm` or OAuth/rejoin copy.
29. GitHub Actions CI baseline:
   - backend repo has `Backend CI` for PR/push to `develop` and `main`;
   - backend CI uses Java 21 and runs `./gradlew test --no-daemon --console=plain`;
   - frontend repo has `Frontend CI` for PR/push to `develop` and `main`;
   - frontend CI uses Node.js 24 and runs `npm ci`, `npm run lint`, and `npm run build`;
   - workflow file pushes require GitHub token `workflow` scope;
   - frontend lockfile was synced for npm `11.12.1`, matching the GitHub runner;
   - `develop` branch protection requires backend `Gradle Test` and frontend `Lint and Build` checks before merge.
30. Frontend user feedback baseline:
   - frontend API failures preserve HTTP status and backend error code via `ApiClientError`;
   - common user actions surface success/failure with a top-right toast instead of only the developer activity log;
   - nickname and signup validation remain inline in the form;
   - expired access-token failures guide the user to log in again and clear authenticated UI state;
   - closed study chat send attempts show a normal in-app message instead of only writing to the hidden dev log;
   - desktop and mobile toast placement was checked with Playwright screenshots.
31. Community author display contract:
   - post list/detail responses include author nickname, author profile image, and `ownedByRequester`;
   - comment/reply list responses include author nickname, author profile image, and `ownedByRequester`;
   - post list/detail/comment APIs require authentication; requester ownership is calculated from the authenticated member;
   - frontend passes the access token to community reads, shows author avatars/nicknames instead of raw member ids, and hides post edit/delete actions from non-owners;
   - `WebSocketStompIntegrationTest` was stabilized by waiting for the user queue subscription registration and cleaning notification/outbox data around each test.
32. Study structured recruitment fields:
   - Flyway V12 adds `progress_method`, `target_audience`, `rules`, `capacity`, and `schedule` to `studies`;
   - study create request/response now supports `progressMethod`, `targetAudience`, `rules`, `capacity`, and `schedule`;
   - `description` remains for compatibility, but new frontend study creation no longer builds a synthetic description string;
   - capacity is validated as at least 1 when provided;
   - frontend study cards/detail/history render the structured fields and keep legacy description parsing only as old-data fallback;
   - desktop and mobile study-create layouts were checked with Playwright screenshots.
33. Study update API and UI:
   - authenticated `PUT /api/v1/studies/{studyId}` updates title and structured recruitment fields;
   - only the study owner can update study recruitment info, non-owners receive `STUDY-004`;
   - update request validates required structured fields and capacity >= 1;
   - frontend owners can open study edit mode from study cards/detail and save through the update API;
   - Playwright smoke checked that the edit form opens with title and capacity prefilled.
34. Study capacity close policy:
   - study capacity counts the owner and all `JOINED` members;
   - joining into the final available seat automatically changes the study status to `CLOSED`;
   - additional joins for a full study return `STUDY-007`;
   - closed studies stay hidden from the recruiting study list even if a participant later leaves;
   - there is no reopen API or reopen UI; closed studies remain only as My Page history records.
35. Study join request and owner approval flow:
   - study participation is no longer immediate membership;
   - requester creates a pending join request and the owner approves, rejects, or sees pending requests from study detail;
   - requester can cancel a pending request before approval;
   - owner receives a participation-request notification and requester receives approval/rejection notifications;
   - pre-join users can open a 1:1 private chat with the study owner from recruiting study detail;
   - accepted members can use the study group chat, while the owner-chat shortcut is hidden after participation.
36. Study lifecycle and history behavior:
   - login-required study list shows recruiting studies only;
   - closed or ended studies remain visible to participants through My Page history instead of disappearing entirely;
   - owners can end a joined/closed study so it moves to past study history;
   - owner deletion is available for ended/deleted-history cleanup flows, but the UI should not show misleading delete actions for already deleted studies;
   - withdrawing an owner removes their active recruiting studies from the recruiting list.
37. Notification interaction baseline:
   - notification popup uses unread/read visual state;
   - notification items support per-item read, all-read, delete, and click-to-read behavior;
   - clicking actionable study notifications navigates to the relevant study context;
   - private chat request creates a notification for the other participant;
   - study group chat does not create separate notifications for normal group messages.
38. Community board categories and frontend board UI:
   - Flyway V14 adds `posts.board_type` with `FREE`, `QUESTION`, `REVIEW`, and `NOTICE`;
   - post create accepts `boardType`, defaulting to `FREE` for compatibility;
   - post list supports optional `GET /api/v1/posts?boardType=...` filtering;
   - post responses include `boardType`;
   - frontend community now has separate board tabs, a table-style list, independent write screen, independent detail screen, and comments below the post detail;
   - community list/detail hide raw publication status labels and use author/time oriented board presentation.
39. Community permissions and comment delete UI:
   - `NOTICE` post creation is restricted to members with `ADMIN` role;
   - non-admin notice creation returns `POST-003`;
   - frontend hides notice write entry for non-admin users and keeps a client-side guard;
   - frontend shows delete actions for requester-owned comments and replies;
   - comment delete calls `DELETE /api/v1/comments/{commentId}` and reloads the post detail comments.
40. Community notification routing:
   - comment/reply/mention notifications still keep `targetType = COMMENT` and `targetId = commentId`;
   - notification API and realtime payloads now include nullable `targetPostId` for COMMENT notifications;
   - frontend notification clicks use `targetPostId` to open the matching community post detail route;
   - study and chat notification click behavior remains unchanged.
41. Notice post management policy:
   - `NOTICE` post update/delete is restricted to members with `ADMIN` role;
   - any ADMIN can update/delete NOTICE posts, even if another ADMIN originally wrote the notice;
   - non-notice post update/delete remains author-only;
   - frontend shows notice edit/delete actions to ADMIN users and keeps regular post actions based on ownership;
   - authorization was checked in the service layer against DB member roles, not only client-side state or JWT UI flags.
42. Admin email bootstrap and community navigation polish:
   - backend supports `app.admin.emails` / `APP_ADMIN_EMAILS` as the configured ADMIN allow-list;
   - configured ACTIVE members receive `ADMIN` on OAuth login and existing matching members are bootstrapped at application startup;
   - local PostgreSQL granted ADMIN to the currently used `ahwnsk94@gmail.com` ACTIVE rows for immediate notice-admin testing;
   - `GET /api/v1/posts` accepts `page` and `size` query parameters for community list pagination;
   - frontend community detail routes use `/community/{board}/{postId}` and list routes use `/community/{board}`;
   - frontend community list requests 20 posts per page and exposes previous/next controls;
   - requester-owned comment/reply deletion now opens an in-app confirmation modal before calling delete.
43. My Page past study history cleanup:
   - Flyway V15 adds `study_members.history_hidden_at`;
   - authenticated `DELETE /api/v1/studies/me/history/{studyId}` hides a past study from the requester’s My Page history only;
   - authenticated `DELETE /api/v1/studies/me/history` hides all requester-visible past study history;
   - active/current studies cannot be hidden through this endpoint and return `STUDY-010`;
   - hidden history does not delete the study itself and does not affect other members’ history;
   - frontend past study rows show a small `X` history cleanup action, plus a compact `전체 삭제` action in the past-study header;
   - frontend history cleanup and destructive study delete confirmations use the in-app confirmation modal instead of browser `window.confirm`.
44. Post page response contract:
   - `GET /api/v1/posts` now returns a page object with `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`;
   - existing post item fields inside `content` remain unchanged;
   - frontend community list uses backend `hasNext` and `totalElements` instead of inferring pagination from returned item count.
45. Community post server-side search:
   - `GET /api/v1/posts` accepts optional `keyword` together with `boardType`, `page`, and `size`;
   - keyword search applies to published post title/content and preserves board filtering plus page metadata;
   - frontend community search sends the keyword to the backend instead of filtering only the currently loaded page.
46. Community search UX cleanup:
   - community search input shows a compact clear action when a keyword is present;
   - post list loading state is shown while server search/page requests are in flight;
   - frontend ignores stale post-list responses so slower previous searches do not overwrite the latest result.
47. Community post author search:
   - `GET /api/v1/posts?keyword=...` now searches published post title, content, and author nickname;
   - author nickname search keeps the existing board filter and page response metadata;
   - frontend search placeholder reflects title/content/author search scope.
48. Post search null keyword runtime fix:
   - when `keyword` is blank or omitted, `PostService` uses the existing published-list repository methods instead of the keyword JPQL query;
   - this avoids PostgreSQL treating nullable keyword expressions as `bytea` in `LOWER(...)` and returning `GLOBAL-500` for the default community list.
49. Chat report assignment notification sync:
   - assigning a pending chat message report marks unread `CHAT_REPORT` notifications for that report as read;
   - if the report notification outbox is processed after the report has already been assigned or handled, the processor skips creating fresh admin notifications;
   - frontend reloads notifications after an admin claims a report so the current admin's badge/popup catches up immediately;
   - moderation product policy documents this MVP all-admin notification cleanup behavior.

Active feature work in progress:

- None expected on `develop` after the latest verified merge.
- Latest completed learning notes:
  - `docs/learnings/0025-study-history-chat-room-hide.md`
  - `docs/learnings/0026-member-nickname-onboarding.md`
  - `docs/learnings/0027-signup-terms-onboarding.md`
  - `docs/learnings/0028-member-withdrawal-rejoin.md`
  - `docs/learnings/0029-closed-study-chat-and-operational-ui.md`
  - `docs/learnings/0030-github-actions-ci-baseline.md`
  - `docs/learnings/0031-frontend-user-feedback.md`
  - `docs/learnings/0032-community-author-contract.md`
  - `docs/learnings/0033-study-structured-fields.md`
  - `docs/learnings/0034-study-update-api.md`
  - `docs/learnings/0035-study-capacity-close-policy.md`
- Local runtime after the latest work:
  - backend is running on `8081`;
  - frontend is running on `5173`;
  - PostgreSQL schema has V14 applied locally after the post board type migration;
  - PostgreSQL schema has V15 applied locally after the study history hide migration;
  - local ADMIN role has been inserted for ACTIVE `ahwnsk94@gmail.com` rows to unblock notice admin testing.

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
  - authenticated `POST /api/v1/notifications/{notificationId}/read`;
  - authenticated `POST /api/v1/notifications/read-all`.
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
- Chat room list unread counts and last-message summary are implemented.
- Per-message read-count receipts are implemented for message list responses.
- Chat notifications, moderation, and retention policy are not implemented yet.

Member nickname onboarding details:

- Flyway V10 makes `members.nickname` nullable so OAuth sign-up can create a pending-onboarding member.
- Flyway V11 adds `members.terms_agreed_at`, `terms_version`, and `privacy_policy_version`.
- V11 backfills existing nickname-bearing members as `LEGACY` terms agreement so previously-created local members are not blocked by the new sign-up gate.
- OAuth sign-up intentionally ignores provider nickname for the app nickname.
- `AuthMeResponse.signupRequired` is the frontend gate signal. Keep `nicknameRequired` and `termsAgreementRequired` for more specific UI states.
- `SignupRequiredFilter` runs after JWT authentication and blocks `/api/v1/**` for signup-required members except:
  - `GET /api/v1/auth/me`;
  - `PUT /api/v1/auth/me/signup`;
  - `PUT /api/v1/auth/me/nickname`;
  - `POST /api/v1/auth/refresh`;
  - `POST /api/v1/auth/logout`.
- The frontend must fetch `GET /api/v1/auth/me` before loading app data after OAuth callback or refresh recovery. Loading notifications, chat rooms, or studies in the same first `Promise.all` will fail with `MEMBER-004` for new members.
- First sign-up must call `PUT /api/v1/auth/me/signup` with nickname, `termsAgreed: true`, and `privacyPolicyAgreed: true`. Calling nickname update alone does not complete sign-up because terms agreement remains missing.

Member withdrawal details:

- API: authenticated `DELETE /api/v1/auth/me`.
- The endpoint clears the refresh token cookie even when the request only has an access token.
- `MemberAccountService.withdraw(...)` anonymizes the current row instead of hard deleting it:
  - email becomes `withdrawn-{memberId}@studywithme.local`;
  - nickname becomes `null`;
  - OAuth subject becomes `withdrawn:{memberId}`;
  - profile image becomes `null`;
  - status becomes `WITHDRAWN`.
- This preserves foreign-key references from posts, comments, studies, notifications, and chats while freeing the original OAuth provider subject for a new sign-up.
- `SignupRequiredFilter` rejects withdrawn members with `AUTH-003` before controller handling, including `GET /api/v1/auth/me`.
- `TokenService.refresh(...)` rejects refresh tokens whose member is not `ACTIVE`.

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
- Chat room list unread counts and last-message summary are implemented.
- Per-message read-count receipts are implemented for message list responses.
- Chat notifications, moderation, and retention policy are still not implemented.

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
- Login is required for the website's functional API surface. Study list/detail, post list/detail, and comment list all require JWT authentication.
- Study/post/comment mutating routes also require JWT authentication and then enforce domain ownership or membership rules.
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
3. Keep backend route contracts aligned with the login-wall product policy when adding new API routes.

Frontend community screen verification already completed:

- OAuth browser E2E for Google/Kakao against backend `8081` and frontend `5174`;
- `npm run lint`;
- `npm run build`;
- `git diff --check`;
- authenticated API smoke for `GET /api/v1/studies` and `GET /api/v1/posts`;
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
커밋 메시지는 한국어로 쓰고, PR을 열기 전에 반드시 AGENTS.md의 PR readiness gate를 먼저 적용한다.
검증이 끝났다는 이유만으로 PR을 열지 말고, 같은 사용자 흐름이나 같은 reviewable deliverable에 속한 다음 작업이 남아 있으면 같은 feature 브랜치에 checkpoint commit만 쌓고 계속 진행한다.
PR은 hotfix/CI breakage/security immediate fix/사용자 명시 요청이 아닌 이상 develop 대상으로 하나의 coherent issue, feature, domain, infrastructure, MVP slice가 끝났을 때만 연다.
PR-ready slice가 완성되고 검증과 CI가 통과하면 명시적 보류가 없는 한 develop에 머지한다.
```

## Work Rules To Preserve

- User is a beginner/new-grad backend developer. Explain important decisions briefly while working.
- Prefer simple Spring Boot conventions over clever abstractions.
- Use TDD for authentication, authorization, token, migration, and other high-risk behavior.
- Choose the most practical efficient workflow for the task: enough planning, TDD, review, or subagent support to reduce rework and risk, without adding ceremony that does not improve the outcome.
- Use Superpowers `subagent-driven-development` when a reviewable deliverable has at least two independent checkbox tasks, file-conflict risk is low, and implementer/reviewer separation would add real value.
- Keep tightly coupled backend/frontend contract work in the main Codex flow with TDD when steps depend on each other, such as DB shape -> service contract -> API response -> frontend type/UI.
- Use Superpowers `executing-plans` when there is a written implementation plan to execute task-by-task but subagent coordination is unavailable or not worth the overhead.
- Run feasible verification before claiming completion.
- Keep Notion structure as:
  - `작업일지 > StudyWithMe`
    - `StudyWithMe 인수인계 문서`
    - dated worklog pages directly under `StudyWithMe`
- Do not create another nested `작업일지` page under `StudyWithMe`.
- Keep repo learnings under `docs/learnings/`.

## Recent Implementation Notes

### 49. Community search scope

- Backend post list search now accepts `searchScope` with `ALL`, `TITLE`, `TITLE_CONTENT`, and `AUTHOR`.
- `keyword` omitted or blank still uses the normal published-list query path instead of the search query.
- Frontend community search removes the page/count text beside the search box and uses a dropdown to choose the search scope.
- The frontend requests 20 posts per page (`postPageSize = 20`); pagination "next" is only testable after a board has at least 21 matching posts.

### 50. Community post sorting

- Backend post list search now accepts `sortOrder` with `LATEST` and `OLDEST`.
- Sorting uses `createdAt` plus `id` as a tie-breaker to keep pagination stable when timestamps are close.
- Frontend community list exposes the sort dropdown as `최신순` and `오래된순`.

### 51. Community post comment counts

- Post list/detail responses include `commentCount`.
- Comment counts are batch-loaded for list responses and exclude deleted comments plus replies hidden under deleted parent comments.
- Frontend community rows show the comment count beside the title only when the count is greater than zero.

### 52. Study list search

- `GET /api/v1/studies` accepts optional `keyword`.
- Recruiting study search matches title, description, progress method, target audience, and schedule.
- Frontend study list has a search field; recruiting search is server-backed and active-study search filters the already loaded active list locally.

### 53. Study list pagination

- `GET /api/v1/studies` now returns a page response with `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`.
- Public study list remains limited to recruiting studies owned by active members.
- Public study list accepts `keyword`, `page`, and `size`; size is capped at 50.
- Frontend study menu uses previous/next pagination; recruiting pages are server-backed and active-study pages are sliced from the loaded member history.

### 54. Study participant count

- Study responses include `joinedMemberCount`, counting current `JOINED` study members including the owner.
- Frontend study cards and detail modal show current participants against capacity.
- Study detail modal also shows remaining seats, using `마감` when the capacity has been reached.

### 55. My study history pagination

- Existing `GET /api/v1/studies/me` without `scope` still returns the full active/past history response for compatibility.
- `GET /api/v1/studies/me?scope=active|past&keyword=&page=&size=` returns a `StudyPageResponse`.
- Active history includes joined studies that are not `ENDED` or `DELETED`.
- Past history includes hidden-filtered records where the membership is `LEFT` or the study is `ENDED`/`DELETED`.
- Frontend My Page uses server-backed search and previous/next pagination for active and past study sections.

### 56. Study search fields

- Study search now matches title, owner nickname, and schedule only.
- Description, progress method, and target audience are no longer search targets.
- Frontend local active-study filtering uses the same title/owner/schedule criteria.

### 57. Production refresh cookie Secure default

- `application-prod.yml` sets `app.auth.refresh-token-cookie.secure` to `${REFRESH_TOKEN_COOKIE_SECURE:true}`.
- The local/default profile still keeps `${REFRESH_TOKEN_COOKIE_SECURE:false}` so local HTTP OAuth testing remains usable.
- Production HTTPS deployments should run with the `prod` profile so refresh-token cookies are Secure by default.
- Focused auth tests cover the local and prod refresh-token cookie Secure defaults.

### 58. Production frontend origin configuration

- `application-prod.yml` requires `APP_CORS_ALLOWED_ORIGINS` for production CORS allowed origins.
- `application-prod.yml` requires `OAUTH_SUCCESS_FRONTEND_REDIRECT_URI` for the OAuth success callback.
- The local/default profile still keeps localhost `5173` and `5174` defaults for local browser testing.
- Production profile property tests prevent localhost CORS/OAuth callback defaults from silently leaking into production.

### 59. PR granularity gate

- Recent production config work was split too narrowly: refresh cookie Secure default and production frontend origin config should have been one "production deployment config hardening" deliverable.
- The root cause was treating "verified small task" as "PR ready" instead of checking whether the broader reviewable slice was complete.
- Backend and frontend `AGENTS.md` now require a PR readiness gate before opening a PR.
- If the gate fails, keep the branch open and use checkpoint commits instead of opening and merging a tiny PR.
- When the user does not name a milestone, infer and state a reasonable milestone before coding instead of defaulting to the next tiny TODO.
- Ask one or two direct questions only when the milestone, priority, or acceptance criteria would be risky to infer.
- Use `deep-interview` or planning skills only when direct questions are not enough for broad or ambiguous work; do not make heavy planning the default.
- Use checkbox plans before broad implementation slices. If 2+ tasks are independent and reviewable, consider `subagent-driven-development`; if the tasks are strongly coupled, stay in main flow with TDD.
- Do not let old handoff wording such as "검증 후 PR 머지" override the readiness gate. Verification is necessary, but not sufficient for opening a PR.
- If several upcoming changes are all part of the same browser/user flow, keep them on one branch and use checkpoint commits until the full slice is ready.
- Example: frontend route restoration should be one reviewable deliverable when it covers home/workspace routes, community detail/write/edit, study detail/new/edit, chat room detail, post-login return, and back/cancel/delete URL cleanup. Splitting those into separate PRs is too narrow unless one part is an urgent fix or the user explicitly asks for that narrow PR.

### 63. Frontend route restoration PR granularity correction

- Frontend PRs #64, #65, #66, and #67 were merged as separate small route-restoration PRs:
  - #64 app workspace URL routing;
  - #65 chat room detail URL restoration;
  - #66 community write/edit URL routing;
  - #67 study new/edit URL routing.
- Those PRs were locally verified and CI-passing, but the granularity was too small under the current work rules.
- Correct future grouping: create one branch such as `feature/frontend-route-restoration` and keep related routing work as checkpoint commits until the whole route-restoration slice is complete.
- Before opening future PRs, explicitly answer: "Is the next likely task part of the same user-visible flow or reviewable deliverable?" If yes, do not open a PR yet.
- Apply this same correction to other frontend UX families: notification UX, chat UX, my-page UX, community board UX, and study recruitment UX should be grouped by user flow, not by each small screen or helper.

### 64. Notification all-read flow polish

- Backend `POST /api/v1/notifications/read-all` marks all notifications owned by the authenticated member as read and returns the refreshed notification list.
- The route is explicitly authenticated in `SecurityConfig` and covered by `SecurityConfigRouteContractTest`.
- `NotificationControllerTest` verifies that read-all changes only the requester’s notifications and does not mark another member’s notifications as read.
- Frontend notification popup now uses the read-all API instead of sending one request per unread notification.
- Failed individual read requests no longer fake a local read state; the UI keeps server state honest and surfaces the request error.

### 65. Chat room read-state list flow

- Flyway V16 adds `chat_room_members.last_read_message_id` for per-member room read position.
- `GET /api/v1/chat/rooms` responses now include nullable `lastMessageContent`, `lastMessageSenderMemberId`, `lastMessageCreatedAt`, and numeric `unreadCount`.
- `ChatService.findMyRooms` sorts rooms by latest message time, falling back to room creation time when a room has no messages.
- `ChatService.findMessages` marks the requester’s room read position up to the latest returned message after membership validation.
- Unread count excludes messages sent by the requester.
- Frontend chat room list shows recent-message preview plus a compact unread badge, and reloads rooms after opening a chat room so the badge clears.
- This added room-level unread state. Message-level read-count receipts were added later in the chat message read receipt flow.

### 66. Chat message read receipt flow

- `GET /api/v1/chat/rooms/{roomId}/messages` response items include `readMemberCount`.
- `readMemberCount` counts current usable room members, excluding the message sender, whose `last_read_message_id` is at least the message id.
- Message list lookup still validates room membership before reading messages or updating the requester read position.
- WebSocket send responses include `readMemberCount`, defaulting to `0` for newly sent messages.
- Frontend chat messages show a small read state only for messages sent by the current user: `읽지 않음` or `읽음 n`.
- This is a read-count receipt only; detailed per-member read lists are not implemented.

### 67. Chat room archive policy

- `DELETE /api/v1/chat/rooms/{roomId}` remains a per-member archive action, not a physical room/message delete.
- Archived chat rooms are hidden from the requester by `chat_room_members.hidden_at`.
- Hidden room members are no longer treated as active room members for direct message list reads, message sends, room member list reads, or WebSocket SUBSCRIBE/SEND validation because `validateRoomMembership` now checks `hidden_at`.
- Reopening an existing 1:1 chat room restores the requester, and if the target had hidden the room, restores the target and sends the private-chat request notification again.
- Frontend copy now says the room was removed from "my list" so users do not confuse the action with global deletion.
- Existing live WebSocket subscriptions are not forcibly disconnected server-side when a member archives a room; the frontend clears selection/disconnects on the user action. Server-side session eviction remains a separate future real-time lifecycle task.

### 68. Chat realtime member fan-out

- Chat WebSocket delivery no longer broadcasts room messages to public `/topic/chat.rooms.{roomId}`.
- Clients subscribe to `/user/queue/chat.rooms.{roomId}` for the active room.
- `ChatWebSocketDestination` accepts chat SUBSCRIBE destinations only in the user-queue form and rejects public room topic subscriptions.
- `ChatWebSocketController` stores the message, then loads current accessible room members with `chatService.findRoomMembers(roomId, senderMemberId)` and sends the response via `convertAndSendToUser(memberId, "/queue/chat.rooms.{roomId}", response)`.
- Because `findRoomMembers` uses the normal room membership policy, archived/hidden, left, deleted-study, or otherwise unusable members are excluded from realtime fan-out.
- Frontend `createRealtimeClient` subscribes to the matching user queue destination.
- The actual STOMP integration test now verifies chat receive through the user queue.

### 69. Chat message delete flow

- Chat messages use soft delete via `chat_messages.deleted_at`; physical message rows and original content remain in the database for future moderation/audit needs.
- `DELETE /api/v1/chat/rooms/{roomId}/messages/{messageId}` allows only the message sender to delete their own message.
- Message deletion still validates current room membership before author ownership, so hidden/left/deleted-study members cannot delete through stale ids.
- Deleted message responses include `deleted=true` and return the display content `삭제된 메시지입니다.` instead of the original content.
- Message list and last-message preview use the same display content mapping, so deleted messages stay in chronology without exposing original text.
- The delete API publishes the deleted message response to current accessible room members through `/user/queue/chat.rooms.{roomId}`.
- Frontend chat messages show a small delete icon only on the current user's non-deleted messages.
- Frontend realtime handling merges incoming chat messages by id, so a delete event updates the existing message instead of appending a duplicate.

### 70. Chat message report baseline

- Flyway V18 adds `chat_message_reports` for per-message moderation reports with reporter, reported member, reason, status, handler, and timestamps.
- A chat room member can report another member's non-deleted message through `POST /api/v1/chat/rooms/{roomId}/messages/{messageId}/reports`.
- Report creation validates current usable room membership before saving, rejects own-message reports, rejects deleted-message reports, and prevents duplicate reports by the same reporter/message pair.
- ADMIN users can list pending/all reports with `GET /api/v1/admin/chat-message-reports?status=PENDING` and handle a report with `POST /api/v1/admin/chat-message-reports/{reportId}/handle`.
- ADMIN authorization is enforced in `ChatService`, not only in the frontend or controller, so non-HTTP internal callers still go through the same role check.
- Report responses include the original message content for moderation review; this endpoint is authenticated and service-guarded to ADMIN for list/handle access.
- Frontend chat rows show a small report action only for another user's non-deleted persisted messages.
- Frontend My Page shows a lightweight ADMIN-only pending report panel where admins can resolve or reject reports; regular members never render this panel.

### 60. Frontend auth action guard polish

- Frontend user actions that require authentication now use a shared `requireAuthenticated` guard instead of silently returning on missing access token.
- Study chat/private chat, study mutations, post create/update/delete, comment/reply create/update/delete, chat room load/delete/connect, and message send surface a normal login-required toast when authentication is missing.
- Post save, comment submit, comment edit save, and chat composer controls include `canConnect` in their disabled state.
- This is a frontend UX guard only; backend authorization remains the source of truth for protected API access.

### 61. Security route contract test

- `SecurityConfigRouteContractTest` documents the backend route security contract.
- Site functional APIs such as `GET /api/v1/studies`, study detail, post list/detail, comment list, `GET /api/v1/studies/me`, join request lists, notifications, chat rooms, and write mutations must return `401 AUTH-003` without authentication.
- Only OAuth entrypoints, auth refresh/logout, health/info, `/ws` handshake, and `/error` are explicitly permitAll at the HTTP route level.
- Test-only unlisted API endpoints prove that newly added API routes are not accidentally exposed by default.

### 62. Login-wall security policy alignment

- The product policy is that users must log in before accessing StudyWithMe website features.
- Backend route security now matches the frontend login wall: study list/detail, community post list/detail, and comment list require JWT authentication instead of being public reads.
- Controller tests were updated so successful study/community/comment read flows use a bearer access token, while unauthenticated access is covered by `SecurityConfigRouteContractTest`.

### 71. Moderation product docs and report admin notification policy

- Moderation policy now has a product document: `docs/product/moderation.md`.
- Frontend moderation UI policy is tracked separately in `StudyWithMe-Front/docs/product/moderation.md`.
- Handoff should keep only recent/session-critical moderation context; long-lived report policy belongs in the product docs.
- Chat report admin notification MVP policy: when a report is created, active ADMIN members receive a `CHAT_REPORT` notification; existing self-notification suppression skips the reporter if they are also an admin.
- This all-admin notification policy is temporary; future assignment should add `assignedAdminId` and notify only the assigned admin for follow-up work.
- Chat report handling uses a `PENDING` status check plus JPA optimistic lock on `chat_message_reports.version` so two admins cannot successfully handle the same report at the same time.

### 72. Study history page test stability

- `StudyControllerTest.findMyActiveStudiesByPageAndKeyword` previously assumed a specific first-page id while testing search and pagination.
- Full-suite verification showed this could fail when two matching studies were created close together.
- The controller test now verifies that page 0 and page 1 contain exactly the two matching active studies, without depending on which one appears first.
- The past-history page test also avoids coupling the controller-level search/pagination assertion to exact row order.

### 73. Admin chat report history filters

- Backend report list contract is now covered for omitted status, `PENDING`, and `RESOLVED` queries.
- `GET /api/v1/admin/chat-message-reports` without `status` returns all report states for admin history review.
- Frontend My Page admin report panel has `대기`, `처리 완료`, `기각`, and `전체` filters.
- Only `PENDING` reports show handling actions; handled reports are displayed as read-only history with status and handled time.
- Clicking a `CHAT_REPORT` notification still switches admins to the pending report filter because new notifications represent actionable reports.

### 74. Admin chat report context panel

- Admin chat message report list/handle responses now include `reporterNickname`, `reportedNickname`, and `handlerNickname` when those members still have nicknames.
- Normal report creation responses keep those nickname fields empty because the member context is only needed for admin operation.
- The backend enriches report responses from member ids, while keeping report list and handle endpoints service-guarded to ADMIN.
- Frontend My Page admin report rows now show 신고자, 피신고자, 처리자, original message, and report reason as separate operator-facing blocks.
- Pending reports still show handling actions; resolved/rejected reports remain read-only history.
- Missing nicknames are rendered as `탈퇴한 회원` on the frontend so numeric ids are not the primary admin label.

### 75. Admin chat report handling note flow

- Backend controller tests now cover the existing 500-character limit for chat report handling notes.
- Frontend admin report handling actions open a modal instead of immediately resolving or rejecting the report.
- The modal shows reporter, reported member, original message, and report reason before the admin submits the decision.
- Admin handling notes are optional and are sent to the existing `handlingNote` request field.
- Handled report history displays the handling note when one exists.

### 76. Admin chat report assignment MVP

- Flyway V20 adds `assigned_admin_member_id` and `assigned_at` to `chat_message_reports`.
- `POST /api/v1/admin/chat-message-reports/{reportId}/assign` lets an ADMIN claim a pending unassigned report.
- Pending reports assigned to another admin cannot be claimed or handled by the current admin.
- `handleMessageReport` now requires the requester to be the assigned admin before resolving or rejecting.
- Admin report responses include `assignedAdminMemberId`, `assignedAdminNickname`, and `assignedAt`.
- Frontend admin report rows show 담당자 separately from 처리자.
- Pending unassigned reports show `담당하기`; only reports assigned to the current admin show `처리 완료` and `기각`.
- MVP all-admin `CHAT_REPORT` notification fan-out remains for newly created unassigned reports; assigned reports need notification cleanup so they do not remain as fresh unclaimed alerts.

### 77. Admin chat report assignment notification sync

- `ChatService.assignMessageReport` now marks unread `CHAT_REPORT` notifications for the assigned report as read after the assignment update succeeds.
- `NotificationOutboxProcessor` checks that a `CHAT_MESSAGE_REPORTED` target report is still `PENDING` and unassigned before creating admin notifications.
- This prevents stale notifications both when notifications already exist at assignment time and when outbox processing runs late after assignment.
- Frontend `assignChatReport` reloads notifications after the claim request succeeds so the current admin's local notification state catches up.
- `docs/product/moderation.md` records the MVP policy: all active admins may receive the initial report alert, but claimed or handled reports should not continue to appear as new unclaimed alerts.

### 78. Community content report moderation branch

- Active branch in both repos: `feature/content-report-moderation`.
- Backend adds `content_reports` with Flyway V21 and supports post/comment report creation plus ADMIN list, assign, and handle APIs.
- Community report notifications use `CONTENT_REPORT`; assignment marks unread report notifications read and late outbox processing skips already assigned/handled reports.
- Frontend adds report actions on another member's community post/comment/reply, a community report modal, and a My Page `커뮤니티 신고` admin panel with the same status and assignment filters as chat reports.
- Notification popup target type `CONTENT_REPORT` opens My Page and reloads the pending content report queue for admins.
- Focused backend verification passed for `ContentReportServiceTest`, `ContentReportControllerTest`, `SecurityConfigRouteContractTest`, and `NotificationOutboxProcessorTest`.
- Frontend `npm run lint` and `npm run build` passed after the content report UI wiring.

### 79. Moderation content action branch

- Active branch in both repos: `feature/moderation-content-action`.
- Backend adds `ContentReportModerationAction` with `NONE` and `DELETE_TARGET`.
- `RESOLVED + DELETE_TARGET` soft-deletes the reported post/comment/reply through the existing `PUBLISHED`/`DELETED` visibility policy.
- `REJECTED + DELETE_TARGET` is rejected as an invalid moderation action.
- Frontend adds a `신고 대상 삭제` checkbox in the `커뮤니티 신고` resolve modal and shows `대상 삭제` in handled report history.
- This is content takedown only; member-level warnings/suspensions/bans remain future sanctions work.

### 80. Chat report moderation action branch

- Active branch in both repos: `feature/chat-report-moderation-action`.
- Backend adds `ChatMessageReportModerationAction` with `NONE` and `DELETE_TARGET`.
- Flyway V23 adds `chat_message_reports.moderation_action`, defaulting existing reports to `NONE`.
- `RESOLVED + DELETE_TARGET` soft-deletes the reported chat message through the existing deleted-message placeholder policy.
- `REJECTED + DELETE_TARGET` is rejected as an invalid moderation action.
- Existing 4-argument `handleMessageReport` calls remain transactional so older internal callers still persist handled status changes.
- Frontend adds a `신고 대상 메시지 삭제` checkbox in the `채팅 신고` resolve modal and shows `대상 삭제` in handled chat report history.
- This is message takedown only; member-level warnings/suspensions/bans and room restrictions remain future sanctions work.

### 81. Member sanction baseline branch

- Active backend branch: `feature/member-sanction-baseline`.
- Backend adds `member_sanctions` with Flyway V24 for ADMIN-only member-level sanction history.
- `POST /api/v1/admin/member-sanctions` records a sanction for an active target member.
- `GET /api/v1/admin/member-sanctions?targetMemberId={memberId}` returns one member's sanction history newest first.
- Current baseline supports `WARNING` only and is record-only: it does not block login, invalidate tokens, suspend, ban, or restrict chat access.
- Optional `sourceType` and `sourceId` can link a sanction to manual admin action, chat message report, or community content report.
- Frontend UI is intentionally not part of this backend API baseline branch.

# Notification Outbox Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 댓글/답글 작성 이벤트를 outbox에 안전하게 기록하고, worker가 in-app 알림으로 처리하는 신뢰성 baseline을 만든다.

**Architecture:** 도메인 transaction 안에서 댓글/답글과 `outbox_events`를 함께 저장한다. 별도 application service가 pending outbox를 처리해 `notifications`를 idempotent하게 생성한다. Kafka는 아직 붙이지 않고, 다음 slice에서 outbox relay가 Kafka로 publish할 수 있게 event 모델과 상태 전이를 먼저 고정한다.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, Jackson JSON, JUnit 5, MockMvc.

---

## File Structure

- Create `src/main/resources/db/migration/V6__create_notification_outbox_schema.sql`: `outbox_events`, `notifications`.
- Create `src/main/java/com/studywithme/outbox/domain/*`: outbox event/status.
- Create `src/main/java/com/studywithme/outbox/repository/OutboxEventRepository.java`.
- Create `src/main/java/com/studywithme/outbox/application/OutboxEventPublisher.java`.
- Modify `src/main/java/com/studywithme/comment/application/CommentService.java`: publish `COMMENT_CREATED` and `REPLY_CREATED` outbox records after saving comments.
- Create `src/main/java/com/studywithme/notification/domain/*`: notification/type/target.
- Create `src/main/java/com/studywithme/notification/repository/NotificationRepository.java`.
- Create `src/main/java/com/studywithme/notification/application/*`: processor, service, results.
- Create `src/main/java/com/studywithme/notification/presentation/*`: API for my notifications and read.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`: authenticated notification routes.
- Create tests under `src/test/java/com/studywithme/outbox/` and `src/test/java/com/studywithme/notification/`.

## Tasks

### Task 1: Outbox Schema And Recording

- [ ] Write failing tests that comment/reply creation stores outbox events in the same transaction.
- [ ] Add V6 outbox schema and outbox domain/repository/publisher.
- [ ] Wire `CommentService` to publish comment/reply events.
- [ ] Run focused outbox tests.

### Task 2: Notification Processing

- [ ] Write failing processor tests for comment-on-post notification, reply-on-comment notification, self-notification suppression, and idempotent reprocessing.
- [ ] Add notification schema/domain/repository and `NotificationOutboxProcessor`.
- [ ] Run focused notification processor tests.

### Task 3: Notification API

- [ ] Write failing MockMvc tests for authenticated notification list/read and unauthenticated rejection.
- [ ] Add `NotificationService`, response DTOs, and controller.
- [ ] Add explicit security matchers.
- [ ] Run focused notification API tests.

### Task 4: Verification And Handoff

- [ ] Run `./gradlew test --no-daemon --console=plain`.
- [ ] Run `git diff --check`.
- [ ] Run focused cso diff review for auth/data exposure/idempotency risks.
- [ ] Update `docs/handoff.md`, add learning note, update Notion.
- [ ] Commit, open PR to `develop`, merge PR, sync local `develop`.

## Self-Review

- Kafka producer/consumer is intentionally excluded. This slice creates the reliable DB boundary Kafka will later read from.
- Notification delivery is in-app DB only. WebSocket/SSE delivery is a later slice.
- Event processing is at-least-once. Idempotency is enforced by notification unique constraints and source event ids.

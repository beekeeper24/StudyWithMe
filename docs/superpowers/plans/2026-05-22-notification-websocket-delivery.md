# Notification WebSocket Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DB에 생성된 in-app notification을 수신자에게 STOMP WebSocket 개인 큐로 실시간 전달한다.

**Architecture:** 알림 생성 정책은 `NotificationOutboxProcessor`가 계속 담당한다. 알림 row가 저장된 뒤 commit 이후에만 `NotificationRealtimePublisher`가 `/user/queue/notifications`로 `NotificationResponse`를 전달해서, 실시간 전달이 DB 기록을 앞서가지 않게 한다.

**Tech Stack:** Java 21, Spring Boot WebSocket/STOMP, Spring TransactionSynchronization, Spring Data JPA, JUnit/AssertJ/Mockito

---

## File Structure

- Create `src/main/java/com/studywithme/notification/application/NotificationRealtimePublisher.java`: 알림 실시간 전달 port.
- Create `src/main/java/com/studywithme/notification/presentation/NotificationWebSocketPublisher.java`: `SimpMessagingTemplate.convertAndSendToUser(...)` adapter.
- Modify `src/main/java/com/studywithme/notification/application/NotificationOutboxProcessor.java`: 알림 저장 후 commit 이후 publisher 호출.
- Modify `src/main/java/com/studywithme/chat/presentation/ChatWebSocketAuthChannelInterceptor.java`: `/user/queue/notifications` 구독은 인증된 사용자면 허용하고, chat room destination만 room membership 검사.
- Create `src/test/java/com/studywithme/notification/presentation/NotificationWebSocketPublisherTest.java`: user queue destination 검증.
- Create `src/test/java/com/studywithme/notification/application/NotificationRealtimeDeliveryTest.java`: outbox 처리 commit 이후 publisher 호출 검증.
- Modify `src/test/java/com/studywithme/chat/presentation/ChatWebSocketAuthChannelInterceptorTest.java`: notification subscribe 허용 테스트 추가.

## Tasks

### Task 1: RED Tests

- [ ] Publisher test:
  - receiver id `1`에게 `/queue/notifications`로 `NotificationResponse`를 보낸다.
- [ ] Processor integration test:
  - outbox 처리로 notification row가 만들어진 뒤 transaction commit 이후 publisher가 호출된다.
- [ ] Interceptor test:
  - authenticated `SUBSCRIBE /user/queue/notifications`는 통과한다.
  - unauthenticated subscribe는 `AUTH-003`으로 거부한다.

### Task 2: GREEN Implementation

- [ ] Add `NotificationRealtimePublisher`.
- [ ] Implement WebSocket adapter with `SimpMessagingTemplate.convertAndSendToUser(receiverMemberId.toString(), "/queue/notifications", response)`.
- [ ] In `NotificationOutboxProcessor`, after successful notification save, schedule publish after commit using `TransactionSynchronizationManager`.
- [ ] If no transaction is active, publish immediately for non-transactional callers/tests.
- [ ] Make STOMP interceptor distinguish:
  - chat destinations: room id parse and membership check;
  - notification user queue: authenticated principal required, no room check.

### Task 3: Verification And Handoff

- [ ] Run focused tests:
  - `./gradlew test --tests com.studywithme.notification.presentation.NotificationWebSocketPublisherTest --tests com.studywithme.notification.application.NotificationRealtimeDeliveryTest --tests com.studywithme.chat.presentation.ChatWebSocketAuthChannelInterceptorTest --no-daemon --console=plain`
- [ ] Run full verification:
  - `./gradlew test --no-daemon --console=plain`
  - `git diff --check`
- [ ] Run focused `cso` review for WebSocket notification user-queue data exposure.
- [ ] Update:
  - `docs/handoff.md`
  - `docs/learnings/0019-notification-websocket-delivery.md`
  - Notion dated work log and handoff page.

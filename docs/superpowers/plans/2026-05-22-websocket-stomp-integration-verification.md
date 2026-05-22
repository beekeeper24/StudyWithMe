# WebSocket STOMP Integration Verification Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 실제 STOMP client가 랜덤 포트 Spring Boot 서버에 연결해서 채팅 메시지와 알림을 수신하는 흐름을 통합 테스트로 검증한다.

**Architecture:** `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 서버를 띄우고 `WebSocketStompClient`로 `/ws`에 접속한다. 테스트는 REST/mock 없이 STOMP `CONNECT`, `SUBSCRIBE`, `SEND`를 실제 frame으로 수행한다.

**Tech Stack:** Java 21, Spring Boot WebSocket, WebSocketStompClient, JUnit, AssertJ

---

## File Structure

- Create `src/test/java/com/studywithme/websocket/WebSocketStompIntegrationTest.java`: 실제 STOMP 연결 통합 테스트.
- Modify `build.gradle` only if a standard WebSocket client runtime is missing.
- Update `docs/handoff.md`, `docs/learnings/0020-websocket-stomp-integration.md`, and Notion after verification.

## Tasks

### Task 1: RED Integration Tests

- [ ] Start app on random port.
- [ ] Save two members and create a private chat room.
- [ ] Connect as one member using STOMP `Authorization: Bearer <access-token>`.
- [ ] Subscribe to `/topic/chat.rooms.{roomId}`.
- [ ] Send to `/app/chat.rooms.{roomId}.messages`.
- [ ] Assert a payload with the expected `roomId`, `senderMemberId`, and `content` is received.
- [ ] Connect a notification receiver.
- [ ] Subscribe to `/user/queue/notifications`.
- [ ] Create comment notification through existing outbox processor.
- [ ] Assert the receiver gets the expected notification payload.

### Task 2: GREEN Runtime Support

- [ ] If the client cannot start because there is no Jakarta WebSocket client implementation, add the smallest test runtime dependency.
- [ ] Keep production dependencies unchanged unless the app itself needs them.

### Task 3: Verification And Handoff

- [ ] Run focused integration test:
  - `./gradlew test --tests com.studywithme.websocket.WebSocketStompIntegrationTest --no-daemon --console=plain`
- [ ] Run full verification:
  - `./gradlew test --no-daemon --console=plain`
  - `git diff --check`
- [ ] Run focused security review for actual STOMP connection auth/data exposure.
- [ ] Update learning and handoff docs plus Notion.

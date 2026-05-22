# Chat WebSocket Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** REST 채팅 MVP 위에 STOMP WebSocket 실시간 메시지 전달을 추가한다.

**Architecture:** 클라이언트는 `/ws`로 WebSocket을 열고 STOMP `CONNECT` 헤더에 `Authorization: Bearer <access-token>`을 보낸다. 서버는 CONNECT에서 JWT를 인증하고, `SUBSCRIBE`와 `SEND`에서 채팅방 참여자 여부를 검사한 뒤, `SEND` 메시지를 DB에 저장하고 `/topic/chat.rooms.{roomId}`로 fan-out한다.

**Tech Stack:** Java 21, Spring Boot WebSocket, STOMP simple broker, Spring Security JWT, Spring Data JPA, JUnit/AssertJ

---

## File Structure

- Create `src/main/java/com/studywithme/chat/presentation/ChatWebSocketConfig.java`: STOMP endpoint, app destination, simple broker 설정.
- Create `src/main/java/com/studywithme/chat/presentation/ChatWebSocketAuthChannelInterceptor.java`: CONNECT JWT 인증, SUBSCRIBE/SEND 방 참여자 인가.
- Create `src/main/java/com/studywithme/chat/presentation/ChatWebSocketController.java`: `/app/chat.rooms.{roomId}.messages` 메시지 저장 및 broker publish.
- Create `src/main/java/com/studywithme/chat/presentation/ChatWebSocketMessageRequest.java`: WebSocket 메시지 작성 요청.
- Create `src/main/java/com/studywithme/chat/presentation/ChatWebSocketDestination.java`: destination room id parsing.
- Modify `src/main/java/com/studywithme/chat/application/ChatService.java`: room membership 검사 메서드를 WebSocket interceptor가 재사용할 수 있게 공개.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`: `/ws/**` handshake path를 열고 STOMP frame에서 인증한다.
- Create `src/test/java/com/studywithme/chat/presentation/ChatWebSocketDestinationTest.java`: destination parser 테스트.
- Create `src/test/java/com/studywithme/chat/presentation/ChatWebSocketAuthChannelInterceptorTest.java`: CONNECT/SUBSCRIBE/SEND 인증과 인가 테스트.
- Create `src/test/java/com/studywithme/chat/presentation/ChatWebSocketControllerTest.java`: 메시지 저장 후 broker publish 테스트.

## Tasks

### Task 1: RED Tests

- [ ] Destination parser test:
  - `/topic/chat.rooms.1` -> `1`
  - `/app/chat.rooms.1.messages` -> `1`
  - malformed destinations throw `CHAT-001`
- [ ] Auth interceptor test:
  - CONNECT without bearer token throws `AUTH-003`
  - CONNECT with valid bearer token sets `AuthenticatedMemberPrincipal`
  - SUBSCRIBE to a room where user is not a member throws `CHAT-002`
  - SEND to a room where user is a member passes
- [ ] Controller test:
  - WebSocket SEND stores a message through `ChatService`
  - returned payload has sender id, room id, and content

### Task 2: GREEN Implementation

- [ ] Add STOMP config:
  - endpoint: `/ws`
  - app prefix: `/app`
  - broker prefix: `/topic`
- [ ] Add channel interceptor:
  - read bearer token from STOMP CONNECT native header
  - convert token claims to `AuthenticatedMemberPrincipal`
  - reject invalid/missing token with `AUTH-003`
  - parse room id from SUBSCRIBE/SEND destinations
  - call `ChatService.validateRoomMembership(roomId, memberId)`
- [ ] Add WebSocket controller:
  - `@MessageMapping("/chat.rooms.{roomId}.messages")`
  - `@SendTo("/topic/chat.rooms.{roomId}")`
  - validate request with `@NotBlank` and `@Size(max = 1000)`
  - save via `ChatService.sendMessage`

### Task 3: Verification And Handoff

- [ ] Run focused tests:
  - `./gradlew test --tests com.studywithme.chat.presentation.ChatWebSocketDestinationTest --no-daemon --console=plain`
  - `./gradlew test --tests com.studywithme.chat.presentation.ChatWebSocketAuthChannelInterceptorTest --no-daemon --console=plain`
  - `./gradlew test --tests com.studywithme.chat.presentation.ChatWebSocketControllerTest --no-daemon --console=plain`
- [ ] Run full verification:
  - `./gradlew test --no-daemon --console=plain`
  - `git diff --check`
- [ ] Run focused `cso` review for WebSocket auth and room subscription data exposure.
- [ ] Update:
  - `docs/handoff.md`
  - `docs/learnings/0018-chat-websocket-delivery.md`
  - Notion dated work log and handoff page.

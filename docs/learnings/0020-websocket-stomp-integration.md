# 0020 WebSocket STOMP Integration Verification

Date: 2026-05-22

## Context

Unit tests covered STOMP interceptors and controllers, but they did not prove that a real client can connect to the running Spring Boot server and receive brokered messages.

## Decisions

- Add `WebSocketStompIntegrationTest` with `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
- Use `WebSocketStompClient` and `StandardWebSocketClient` to connect to `/ws`.
- Send the same STOMP `Authorization: Bearer <access-token>` header the frontend will use.
- Verify chat delivery by:
  - creating a private room;
  - subscribing to `/topic/chat.rooms.{roomId}`;
  - sending to `/app/chat.rooms.{roomId}.messages`;
  - asserting the received payload has the expected room, sender, and content.
- Verify notification delivery by:
  - subscribing to `/user/queue/notifications`;
  - creating a comment outbox notification;
  - processing pending outbox events;
  - asserting the receiver gets the expected notification payload.

## Verification Rule

When changing WebSocket config, STOMP destinations, auth headers, or broker prefixes, run:

```bash
./gradlew test --tests com.studywithme.websocket.WebSocketStompIntegrationTest --no-daemon --console=plain
```

This test catches drift that unit tests can miss, such as broken handshake paths, missing broker prefixes, or user queue routing changes.

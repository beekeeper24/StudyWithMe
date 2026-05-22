# 0018 Chat WebSocket Delivery

Date: 2026-05-22

## Context

WebSocket delivery adds a second access path to private chat messages. The HTTP security filter can protect the handshake path, but STOMP frames also need their own authentication and authorization checks.

## Decisions

- Use STOMP over `/ws` with:
  - application prefix `/app`;
  - broker prefix `/topic`.
- Keep `/ws` handshake public in `SecurityConfig` so the WebSocket upgrade can happen.
- Authenticate actual WebSocket sessions in the STOMP `CONNECT` frame using `Authorization: Bearer <access-token>`.
- Authorize both:
  - `SUBSCRIBE /topic/chat.rooms.{roomId}`;
  - `SEND /app/chat.rooms.{roomId}.messages`.
- Reuse `ChatService.validateRoomMembership(roomId, memberId)` for STOMP authorization.
- Save WebSocket messages through `ChatService.sendMessage(...)` before publishing to `/topic/chat.rooms.{roomId}`.

## Verification Rule

Future WebSocket work should keep tests that prove:

- missing STOMP bearer token returns `AUTH-003`;
- valid STOMP bearer token sets `AuthenticatedMemberPrincipal`;
- non-members cannot subscribe to a room topic;
- non-members cannot send to a room app destination;
- WebSocket send stores the message before fan-out.

## Follow-up

This slice uses the in-memory simple broker. It is suitable for MVP/local development, but multi-instance deployment should switch to a broker relay or an external fan-out strategy.

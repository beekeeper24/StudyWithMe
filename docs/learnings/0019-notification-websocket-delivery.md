# 0019 Notification WebSocket Delivery

Date: 2026-05-22

## Context

In-app notifications are already persisted through the outbox processor. Realtime delivery should not bypass that durable record, otherwise the frontend could show a notification that never committed to the database.

## Decisions

- Deliver realtime notifications through STOMP user destinations:
  - client subscribes to `/user/queue/notifications`;
  - server sends with `convertAndSendToUser(receiverMemberId.toString(), "/queue/notifications", response)`.
- Keep the notification creation policy inside `NotificationOutboxProcessor`.
- Publish only after transaction commit using `TransactionSynchronizationManager`.
- If no transaction is active, publish immediately so non-transactional callers still work.
- Allow authenticated `SUBSCRIBE /user/queue/notifications`.
- Reject client `SEND /user/queue/notifications`; only the server publishes notifications.
- Add `/queue` to the simple broker prefixes so user queue messages can be delivered.

## Verification Rule

For future notification realtime work, keep or extend tests that prove:

- processor publishes only after commit;
- duplicate outbox processing does not duplicate notification rows;
- notification user queue subscription requires an authenticated STOMP principal;
- client SEND to the notification user queue is rejected;
- WebSocket payload is the same `NotificationResponse` shape as the polling API.

## Follow-up

The current realtime path is best-effort after DB commit. If delivery reliability becomes a hard requirement, add a dedicated delivery outbox or reconnect catch-up policy rather than treating WebSocket publish as durable.

# 0012 Notification Outbox Baseline

## Context

The first notification slice needs reliable event capture before Kafka is introduced.

## Decision

- Store domain-side events in `outbox_events` inside the same transaction as comment/reply writes.
- Process outbox rows into in-app `notifications` with an at-least-once processor.
- Use `source_event_id + receiver_member_id + type` as a notification uniqueness guard.
- Suppress self-notifications in the processor.
- Retry both `PENDING` and `FAILED` outbox events whose `next_attempt_at` is due.
- Mark repeated failures as `DEAD` after five attempts.
- Keep the scheduled worker disabled by default and enable it with `app.notification.outbox.worker-enabled=true`.

## Why

Kafka does not remove the need for idempotency, retry, or durable event capture. The DB outbox is the source of truth; Kafka can later become a relay target without changing the domain transaction boundary.

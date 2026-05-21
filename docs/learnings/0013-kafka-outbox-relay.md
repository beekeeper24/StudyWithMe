# 0013 Kafka Outbox Relay Baseline

## Context

Notification outbox capture is already reliable inside the domain transaction. The next step is relaying those durable DB events to Kafka without making comment/reply writes wait for Kafka availability.

## Decision

- Keep `outbox_events.status` for in-app notification processing.
- Add separate Kafka publish state columns:
  - `kafka_publish_status`
  - `kafka_retry_count`
  - `kafka_next_attempt_at`
  - `kafka_published_at`
  - `kafka_last_error`
- Publish to Kafka with `event.id` as the key.
- Send a JSON envelope that contains the outbox metadata and original payload.
- Keep the scheduled Kafka relay disabled by default and enable it with `OUTBOX_KAFKA_RELAY_ENABLED=true`.

## Why

One outbox row may feed more than one downstream process. Reusing the existing `status` for Kafka would make in-app notification processing and Kafka publishing race each other. Separate status columns keep the learning model simple while preserving independent retry and dead-letter behavior.

Kafka publishing remains at-least-once. If Kafka accepts a message but the DB status update fails, the relay may publish the same event again. Downstream consumers must deduplicate by `eventId`.

## Verification

- `./gradlew test --tests "*OutboxKafkaRelayTest" --no-daemon --console=plain`
- `./gradlew test --tests "*StudyWithMeApplicationTests" --no-daemon --console=plain`
- `./gradlew test --no-daemon --console=plain`
- `git diff --check`
- `docker compose config --quiet`

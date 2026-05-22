# 0015 Kafka Notification Consumer Baseline

## Context

The outbox relay can publish durable DB events to Kafka. The next step is consuming those events without duplicating notification policy or breaking the existing DB outbox processor.

## Decision

- Use a shared `OutboxKafkaEvent` envelope for relay and consumer.
- Keep `NotificationKafkaConsumer` disabled by default.
- Enable it with `NOTIFICATION_KAFKA_CONSUMER_ENABLED=true`.
- Use `NOTIFICATION_KAFKA_GROUP_ID` for the consumer group, defaulting to `studywithme-notification`.
- Delegate Kafka messages to `NotificationOutboxProcessor.processKafkaEvent(...)`.
- Keep the DB polling processor in place during the MVP transition.

## Why

Notification policy should not fork between DB polling and Kafka consumption. The consumer is only a transport adapter; the processor owns notification creation, self-suppression, mention replacement, and idempotency.

Kafka remains at-least-once. The notification unique guard uses the outbox `eventId` as `sourceEventId`, so a message can be retried or replayed without creating duplicate notifications for the same receiver/type.

## Verification

- `./gradlew test --tests "*NotificationKafkaConsumerTest" --tests "*NotificationOutboxProcessorTest" --no-daemon --console=plain`
- `./gradlew test --tests "*OutboxKafkaRelayTest" --tests "*NotificationKafkaConsumerTest" --tests "*NotificationOutboxProcessorTest" --no-daemon --console=plain`
- `./gradlew test --no-daemon --console=plain`
- `git diff --check`
- `docker compose config --quiet`

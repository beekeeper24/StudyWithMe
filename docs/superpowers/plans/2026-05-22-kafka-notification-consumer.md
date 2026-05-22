# Kafka Notification Consumer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kafka topic `studywithme.outbox.events`를 읽어 기존 in-app notification 정책을 실행하는 consumer baseline을 만든다.

**Architecture:** DB outbox remains the source of truth. `OutboxKafkaRelay` publishes a shared JSON envelope, and a disabled-by-default `NotificationKafkaConsumer` reads that envelope and delegates to `NotificationOutboxProcessor`. The processor keeps idempotency by source event id, so DB processor and Kafka consumer can temporarily coexist during the MVP transition.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Kafka, Jackson JSON, JUnit 5, Mockito.

---

## File Structure

- Create `src/main/java/com/studywithme/outbox/application/OutboxKafkaEvent.java`
  - Shared Kafka envelope record used by relay and consumer.
- Modify `src/main/java/com/studywithme/outbox/application/OutboxKafkaRelay.java`
  - Use shared envelope record.
- Modify `src/main/resources/application.yml`
  - Add Kafka consumer string deserializers.
  - Add `app.notification.kafka.consumer-enabled` and `group-id`.
- Create `src/main/java/com/studywithme/notification/application/NotificationKafkaConsumer.java`
  - Disabled by default.
  - `@KafkaListener` reads outbox event envelopes.
- Modify `src/main/java/com/studywithme/notification/application/NotificationOutboxProcessor.java`
  - Expose `processKafkaEvent(...)` that reuses the same notification policy as DB outbox processing.
- Add tests:
  - `src/test/java/com/studywithme/notification/application/NotificationKafkaConsumerTest.java`
  - Update `NotificationOutboxProcessorTest`.

## Tasks

### Task 1: Consumer Contract Tests

- [x] Write failing consumer test for valid Kafka envelope delegation.
- [x] Write failing consumer test for malformed message rejection.
- [x] Run focused consumer test and verify RED.

### Task 2: Processor Reuse Tests

- [x] Write failing processor test that `processKafkaEvent` creates a mention notification idempotently.
- [x] Run focused processor test and verify RED.

### Task 3: Implementation

- [x] Add shared Kafka envelope.
- [x] Refactor relay to use shared envelope.
- [x] Add consumer config and `NotificationKafkaConsumer`.
- [x] Add `processKafkaEvent` processor entrypoint.
- [x] Run focused tests.

### Task 4: Verification And Handoff

- [x] Run full tests.
- [x] Run `git diff --check`.
- [x] Run focused cso review for consumer enablement/idempotency/data exposure.
- [x] Update `docs/handoff.md`, add learning note, update Notion.
- [x] Commit, open PR to `develop`, merge PR, sync local `develop`.

## Self-Review

- Consumer is disabled by default with `NOTIFICATION_KAFKA_CONSUMER_ENABLED=false`.
- This slice does not remove the DB polling processor.
- Kafka delivery remains at-least-once; notification unique guards remain required.
- Downstream idempotency key is the outbox `eventId`.

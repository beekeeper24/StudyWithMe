# Kafka Outbox Relay Implementation Plan

**Goal:** `outbox_events`를 Kafka topic으로 내보내는 relay를 추가한다.

**Architecture:** 댓글/답글 domain transaction은 그대로 DB outbox까지만 책임진다. Kafka relay는 별도 scheduled worker가 due outbox row를 읽고 Kafka에 publish한 뒤 Kafka publish 상태만 갱신한다. 기존 `status`는 in-app notification processor가 쓰고 있으므로 Kafka 전송 상태는 별도 컬럼으로 분리한다.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Kafka, Spring Data JPA, Flyway, Jackson JSON, JUnit 5, Mockito.

## File Structure

- Modify `build.gradle`: add Spring Kafka.
- Modify `docker-compose.yml`: add local Kafka service.
- Modify `src/main/resources/application.yml`: add `spring.kafka.*` and `app.outbox.kafka.*`.
- Add `src/main/resources/db/migration/V7__add_outbox_kafka_publish_state.sql`.
- Add `src/main/java/com/studywithme/outbox/domain/OutboxKafkaPublishStatus.java`.
- Modify `OutboxEvent`: add Kafka publish state fields and state transition methods.
- Modify `OutboxEventRepository`: add Kafka relay query.
- Add `src/main/java/com/studywithme/outbox/application/OutboxKafkaProperties.java`.
- Add `src/main/java/com/studywithme/outbox/application/OutboxKafkaRelay.java`.
- Add `src/main/java/com/studywithme/outbox/application/OutboxKafkaRelayWorker.java`.
- Add tests under `src/test/java/com/studywithme/outbox/application/`.

## Tasks

### Task 1: Relay State Model

- [x] Write failing tests for Kafka publish success, retry failure, dead after repeated failures, and already-published idempotency.
- [x] Add V7 schema and entity/repository support for Kafka publish state.

### Task 2: Kafka Relay

- [x] Add Spring Kafka dependency and app properties.
- [x] Implement relay JSON envelope and publish by `KafkaTemplate<String, String>`.
- [x] Add scheduled worker disabled by default.
- [x] Add local Kafka docker compose service.

### Task 3: Verification And Handoff

- [x] Run focused relay tests.
- [x] Run full tests.
- [x] Run `git diff --check`.
- [x] Run focused cso review for data exposure/retry/idempotency.
- [ ] Update `docs/handoff.md`, add learning note, update Notion.
- [ ] Commit, open PR to `develop`, merge PR, sync local `develop`.

## Self-Review

- Kafka publish is at-least-once, not exactly-once.
- The event id is the Kafka key so downstream consumers can deduplicate.
- If Kafka publish succeeds but DB status update fails, the relay may publish the same event again. That is acceptable for this slice because downstream idempotency is part of the architecture.
- Domain writes never wait for Kafka.

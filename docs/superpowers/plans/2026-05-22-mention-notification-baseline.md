# Mention Notification Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 댓글/답글 본문에서 `@nickname` 멘션을 추출하고, 멘션 대상에게 in-app notification을 생성한다.

**Architecture:** 댓글/답글 생성 transaction 안에서 기존 comment/reply outbox event와 별도로 `COMMENT_MENTIONED` outbox event를 저장한다. 멘션 추출은 `mention` 패키지의 작은 service가 담당하고, 알림 생성은 기존 `NotificationOutboxProcessor`가 처리한다. 같은 댓글 안 중복 멘션은 1명당 1개로 합치고, 멘션 알림은 같은 댓글/답글의 일반 댓글/답글 알림보다 우선한다.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, Jackson JSON, JUnit 5, AssertJ.

---

## File Structure

- Create `src/main/java/com/studywithme/mention/application/MentionExtractor.java`
  - Extract unique nickname tokens from comment content.
- Create `src/main/java/com/studywithme/mention/application/MentionTargetResolver.java`
  - Resolve extracted nicknames to ACTIVE members.
- Modify `src/main/java/com/studywithme/member/repository/MemberRepository.java`
  - Add nickname/status lookup for mention resolution.
- Modify `src/main/java/com/studywithme/outbox/application/OutboxEventPublisher.java`
  - Add `publishCommentMentioned(...)`.
- Modify `src/main/java/com/studywithme/comment/application/CommentService.java`
  - Resolve mentions after comment/reply save and publish `COMMENT_MENTIONED` when targets exist.
- Modify `src/main/java/com/studywithme/notification/domain/NotificationType.java`
  - Add `MENTIONED_IN_COMMENT`.
- Modify `src/main/java/com/studywithme/notification/application/NotificationOutboxProcessor.java`
  - Process `COMMENT_MENTIONED`.
  - Suppress ordinary comment/reply notification for receivers explicitly mentioned in the same source event.
- Add tests:
  - `src/test/java/com/studywithme/mention/application/MentionExtractorTest.java`
  - `src/test/java/com/studywithme/mention/application/MentionTargetResolverTest.java`
  - Update `CommentOutboxEventTest`
  - Update `NotificationOutboxProcessorTest`

## Tasks

### Task 1: Mention Extraction

- [x] Write failing `MentionExtractorTest` for unique `@nickname` extraction.
- [x] Implement minimal extractor.
- [x] Run focused extractor test.

### Task 2: Mention Target Resolution

- [x] Write failing resolver test for active existing members only.
- [x] Add repository query and resolver.
- [x] Run focused resolver test.

### Task 3: Mention Outbox Recording

- [x] Write failing comment/reply tests that mention content stores `COMMENT_MENTIONED` outbox event in the same transaction.
- [x] Wire `CommentService` to resolver and publisher.
- [x] Run focused outbox tests.

### Task 4: Mention Notification Processing

- [x] Write failing processor tests:
  - mentioned member receives `MENTIONED_IN_COMMENT`;
  - self mention is suppressed;
  - same comment duplicate mentions create one notification;
  - mention notification replaces ordinary comment/reply notification for the same receiver and comment.
- [x] Add notification type and processor branch.
- [x] Run focused processor tests.

### Task 5: Verification And Handoff

- [x] Run full tests.
- [x] Run `git diff --check`.
- [x] Run focused cso review for data exposure/idempotency/notification spam risks.
- [x] Update `docs/handoff.md`, add learning note, update Notion.
- [x] Commit, open PR to `develop`, merge PR, sync local `develop`.

## Self-Review

- Scope excludes mention parsing on comment update.
- Scope excludes mention records/table; outbox payload and notification records are enough for MVP.
- Scope keeps nickname matching exact and case-sensitive for now.
- Mention notifications are per comment/reply source event. The same member can receive new mention notifications from later comments.

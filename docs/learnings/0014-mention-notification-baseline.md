# 0014 Mention Notification Baseline

## Context

StudyWithMe notifications now come from durable outbox events. Mention support needs to preserve that model while avoiding noisy duplicate notifications when a member is both the ordinary comment/reply notification receiver and the explicit mention receiver.

## Decision

- Extract mentions from comment/reply content with `@nickname`.
- Resolve exact, case-sensitive nicknames to ACTIVE members only.
- Remove duplicate mentions inside the same comment/reply.
- Suppress self-mentions.
- Store a separate `COMMENT_MENTIONED` outbox event when at least one target remains.
- Add `MENTIONED_IN_COMMENT` notification type.
- If the ordinary comment/reply notification receiver is mentioned in the same comment/reply, create only the mention notification for that receiver.

## Why

A mention is a stronger signal than a generic comment/reply notification. Replacing the generic notification with the mention notification keeps the reason clear without losing the event.

The duplicate boundary is one comment/reply source event. If a member is mentioned again in a later comment, they should receive another mention notification.

## Verification

- `./gradlew test --tests "*MentionExtractorTest" --tests "*MentionTargetResolverTest" --no-daemon --console=plain`
- `./gradlew test --tests "*CommentOutboxEventTest" --no-daemon --console=plain`
- `./gradlew test --tests "*NotificationOutboxProcessorTest" --no-daemon --console=plain`
- `./gradlew test --tests "*CommentServiceTest" --tests "*CommentControllerTest" --tests "*CommentOutboxEventTest" --tests "*MentionExtractorTest" --tests "*MentionTargetResolverTest" --tests "*NotificationOutboxProcessorTest" --no-daemon --console=plain`
- `./gradlew test --no-daemon --console=plain`
- `git diff --check`

# 0017 Chat MVP Baseline

Date: 2026-05-22

## Context

Chat access control is sensitive because messages are private user data. The first chat slice should stay REST-only and prove the authorization boundary before adding WebSocket delivery or Kafka-backed fan-out.

## Decisions

- Model chat with three tables first:
  - `chat_rooms`
  - `chat_room_members`
  - `chat_messages`
- Use deterministic room keys:
  - `PRIVATE:{smallerMemberId}:{largerMemberId}`
  - `STUDY:{studyId}`
- Keep every chat HTTP endpoint authenticated in `SecurityConfig`.
- Enforce message read/write authorization in `ChatService`, not only in the controller.
- For study chat, synchronize current `study_members` into `chat_room_members` whenever the study chat room is requested. This prevents a member who joins after room creation from being locked out.

## Verification Rule

For future chat work, keep or extend tests that prove:

- unauthenticated chat requests return `AUTH-003`;
- non-members cannot read or send room messages;
- private room creation is idempotent per member pair;
- study chat rooms include both existing and newly joined study members.

## Follow-up

WebSocket delivery, unread counters, read receipts, chat notifications, moderation, and retention policy are intentionally outside the REST MVP and should be introduced as separate reviewable slices.

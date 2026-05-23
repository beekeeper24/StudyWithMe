# 0025 Study History And Chat Room Hide State

Date: 2026-05-24

## Context

The public study list should stop showing closed studies, but members still need a place to see past participation. Chat rooms also need a user-facing "delete" action without destroying shared room/message history.

## Decision

Model these as member-level states instead of hard deletes:

- `study_members.status`: `JOINED` or `LEFT`
- `study_members.left_at`: when the member left
- `chat_room_members.hidden_at`: when the room is hidden from that member's room list

The public study list returns only recruiting studies. Authenticated history comes from `GET /api/v1/studies/me`, split into active and past studies. Chat room "delete" maps to `DELETE /api/v1/chat/rooms/{roomId}` and hides the room only for the requester.

## Guardrails

- Do not delete `study_members` rows on leave. Leaving must preserve participation history.
- Closed studies should not appear in the public recruitment list.
- Study chat can remain available after a study closes for members who still have joined membership.
- Chat room hiding should not delete shared chat messages or remove the other participants' room access.
- Reopening an existing private or study chat should clear `hidden_at` for the requester.

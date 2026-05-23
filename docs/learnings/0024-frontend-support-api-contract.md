# 0024 Frontend Support API Contract

Date: 2026-05-23

## Context

Frontend UX work exposed places where the app had to infer backend state locally:

- study owner display used only `ownerMemberId`;
- current user's study membership was guessed from current-session actions;
- chat room titles were derived from `studyId`;
- chat room member display had no dedicated API.

These guesses make the UI inconsistent after refresh and force test-only labels into user-facing screens.

## Decision

When frontend screens need user-facing state, prefer extending the backend response contract instead of keeping long-lived local inference.

Current contract additions:

- `StudyResponse.ownerNickname`
- `StudyResponse.ownerProfileImageUrl`
- `StudyResponse.joinedByRequester`
- `StudyResponse.ownedByRequester`
- `ChatRoomResponse.title`
- `GET /api/v1/chat/rooms/{roomId}/members`

## Guardrails

- Public study list/detail may expose owner nickname/profile image, but not owner email.
- Requester-specific flags must be computed from the authenticated principal when a bearer token is present.
- Chat room member lists must stay authenticated and must validate room membership before returning member data.
- Keep existing response fields for frontend compatibility when adding fields.

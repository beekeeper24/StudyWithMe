# 0029 Closed Study Chat And Operational UI

Date: 2026-05-24

## Context

The frontend had several manual refresh controls that made normal screens feel like a test console. Also, study chat messages were still accepted after the study was closed.

## Decisions

- User-facing refresh buttons should not be shown on normal screens unless there is a clear product reason.
- Keep developer-only refresh/sync tools behind the existing developer tools area.
- Account withdrawal should use an in-app confirmation panel, not a browser `confirm` dialog or implementation-specific OAuth copy.
- Closed study chat rooms may remain visible for history, but they must not accept new messages.

## Backend Rule

All chat message writes must go through `ChatService.sendMessage`.

For study chat rooms:

1. validate room membership;
2. load the linked study;
3. reject writes when the study status is `CLOSED`.

This covers both REST message writes and STOMP message writes because both controllers delegate to `ChatService.sendMessage`.

## Frontend Rule

When the active chat room is linked to a known closed study, disable the composer and show a neutral placeholder:

- `마감된 스터디 채팅방입니다`

The backend remains the source of truth. The frontend disabled state is only a usability hint.

## Verification

- TDD RED:
  - `ChatServiceTest.rejectMessageToClosedStudyRoom` failed before implementation because `STUDY_CHAT_ROOM_CLOSED` did not exist.
- `./gradlew test --tests com.studywithme.chat.application.ChatServiceTest`
- `./gradlew test`
- `npm run lint`
- `npm run build`
- Browser check:
  - profile menu has no refresh action;
  - My Page has no refresh action;
  - withdrawal confirmation has no OAuth/rejoin copy;
  - closed study chat composer is disabled;
  - chat room list has no refresh action.

# Moderation Product Policy

This document records user-facing and operator-facing moderation policy.
Update it when moderation behavior changes. Keep `docs/handoff.md` for recent session context only.

## Chat Message Reports

- A signed-in chat room member can report another member's non-deleted chat message.
- A member cannot report their own message.
- A member cannot report the same message more than once.
- Deleted chat messages cannot be newly reported from the UI or API.
- Reports start as `PENDING` and can be handled as `RESOLVED` or `REJECTED`.

## MVP Admin Notification Policy

- For MVP, when a chat message report is created, active `ADMIN` members receive an admin notification.
- Withdrawn admin accounts do not receive report notifications.
- If the reporter is also an admin, the existing self-notification suppression keeps that reporter from receiving their own report notification.
- This is a temporary MVP policy to reduce the chance that a report is missed while there is no assignment workflow.
- The notification target is `CHAT_REPORT`, and the target id is the chat message report id.

## Concurrent Handling Policy

- Only one admin should be able to handle a report.
- The service rejects any report handling request when the report is no longer `PENDING`.
- `chat_message_reports.version` is used as a JPA optimistic lock.
- If two admins submit handling requests at nearly the same time, the first successful commit wins and the later stale update fails with the already-handled report error.

## Admin Report History Policy

- Admins can query all chat message reports by omitting the status filter.
- Admins can query a specific report state with `PENDING`, `RESOLVED`, or `REJECTED`.
- Pending reports are the actionable queue.
- Resolved and rejected reports are read-only history for operational review.

## Future Assignment Policy

When admin volume grows, replace the all-admin notification policy with assignment.

- Add `assignedAdminId`.
- Add an `ASSIGNED` state or equivalent assignment timestamp.
- Notify only the assigned admin for follow-up work.
- Keep `handlerMemberId` as the final admin who resolved or rejected the report.

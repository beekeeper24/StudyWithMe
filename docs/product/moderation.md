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
- This is a temporary MVP policy to reduce the chance that a report is missed before an admin claims the report.
- The notification target is `CHAT_REPORT`, and the target id is the chat message report id.

## Admin Assignment Policy

- Reports start without an assigned admin.
- An admin can claim a pending unassigned report.
- A pending report already assigned to another admin cannot be claimed by a different admin.
- Only the assigned admin can resolve or reject an assigned report.
- Assignment records `assignedAdminMemberId` and `assignedAt`.

## Concurrent Handling Policy

- Only one admin should be able to handle a report.
- The service rejects any report handling request when the report is no longer `PENDING`.
- The service rejects handling by an admin who is not assigned to the report.
- Admin handling notes are optional and limited to 500 characters.
- `chat_message_reports.version` is used as a JPA optimistic lock.
- If two admins claim or handle a report at nearly the same time, the first successful commit wins and the later stale update fails with the already-assigned or already-handled report error.

## Admin Report History Policy

- Admins can query all chat message reports by omitting the status filter.
- Admins can query a specific report state with `PENDING`, `RESOLVED`, or `REJECTED`.
- Pending reports are the actionable queue.
- Resolved and rejected reports are read-only history for operational review.
- Admin report responses include reporter, reported member, assigned admin, and handler nicknames when available.
- Normal report creation responses do not include those nicknames because the context is only needed for admin operation.
- Missing nicknames can happen for withdrawn or incomplete accounts; clients should render a safe fallback instead of relying on numeric ids as the primary operator label.

## Future Assignment Policy

When admin volume grows, replace the all-admin notification policy with automatic or manual assignment notification.

- Add an `ASSIGNED` state or equivalent assignment timestamp.
- Notify only the assigned admin for follow-up work.
- Keep `handlerMemberId` as the final admin who resolved or rejected the report.

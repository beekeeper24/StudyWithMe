# Moderation Product Policy

This document records user-facing and operator-facing moderation policy.
Update it when moderation behavior changes. Keep `docs/handoff.md` for recent session context only.

## Chat Message Reports

- A signed-in chat room member can report another member's non-deleted chat message.
- A member cannot report their own message.
- A member cannot report the same message more than once.
- Deleted chat messages cannot be newly reported from the UI or API.
- Reports start as `PENDING` and can be handled as `RESOLVED` or `REJECTED`.

## Community Content Reports

- A signed-in member can report another member's published community post or published comment/reply.
- A member cannot report their own post, comment, or reply.
- A member cannot report the same target more than once.
- Reports store a snapshot of the reported content so admins can review the original context even if the content changes later.
- Post reports store the post id as both `targetId` and `postId`.
- Comment/reply reports store the comment id as `targetId` and the parent post id as `postId`.
- Reports start as `PENDING` and can be handled as `RESOLVED` or `REJECTED`.

## MVP Admin Notification Policy

- For MVP, when a chat message report or community content report is created, active `ADMIN` members receive an admin notification.
- Withdrawn admin accounts do not receive report notifications.
- If the reporter is also an admin, the existing self-notification suppression keeps that reporter from receiving their own report notification.
- This is a temporary MVP policy to reduce the chance that a report is missed before an admin claims the report.
- The notification target is `CHAT_REPORT`, and the target id is the chat message report id.
- For community content reports, the notification target is `CONTENT_REPORT`, and the target id is the content report id.
- When an admin claims a report, unread `CHAT_REPORT` notifications for that report are marked read so it no longer remains as a fresh unclaimed alert for admins.
- When an admin claims a community content report, unread `CONTENT_REPORT` notifications for that report are marked read.
- If the report notification outbox is processed after the report has already been assigned or handled, the processor skips creating new admin notifications for that report.
- Realtime read-state push is not part of the MVP; clients refresh through the existing notification list loading and polling path.

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
- `chat_message_reports.version` and `content_reports.version` are used as JPA optimistic locks.
- If two admins claim or handle a report at nearly the same time, the first successful commit wins and the later stale update fails with the already-assigned or already-handled report error.

## Community Moderation Action Policy

- Community content reports can be handled with moderation action `NONE` or `DELETE_TARGET`.
- `NONE` only records the report decision and leaves the reported content unchanged.
- `DELETE_TARGET` soft-deletes the reported post, comment, or reply while preserving the original row and report snapshot for audit history.
- `DELETE_TARGET` is allowed only with `RESOLVED`; rejected reports cannot delete the target content.
- Deleted posts and comments are hidden through the existing published-content list/detail policies.
- This is content takedown only. Member-level sanctions such as warnings, suspensions, or bans are a later feature.

## Admin Report History Policy

- Admins can query all chat message reports or community content reports by omitting the status filter.
- Admins can query a specific report state with `PENDING`, `RESOLVED`, or `REJECTED`.
- Pending reports are the actionable queue.
- Resolved and rejected reports are read-only history for operational review.
- Admin report responses include reporter, reported member, assigned admin, and handler nicknames when available.
- Community content report responses include the selected moderation action.
- Normal report creation responses do not include those nicknames because the context is only needed for admin operation.
- Missing nicknames can happen for withdrawn or incomplete accounts; clients should render a safe fallback instead of relying on numeric ids as the primary operator label.

## Future Assignment Policy

When admin volume grows, replace the all-admin notification policy with automatic or manual assignment notification.

- Route new report notifications to a responsible admin or admin group instead of every active admin.
- Keep `assignedAdminMemberId` as the admin who owns the pending report.
- Keep `handlerMemberId` as the final admin who resolved or rejected the report.

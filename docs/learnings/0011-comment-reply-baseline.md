# 0011 Comment Reply Baseline

## Context

The comment MVP connects comments and one-level replies to free-board posts.

## Decision

- Store comments and replies in one `comments` table.
- Use nullable `parent_comment_id` for one-level replies.
- Reject nested replies in `CommentService` with `COMMENT-003`.
- Use `PUBLISHED` and `DELETED` statuses instead of physical deletion.
- Public comment lists query only `PUBLISHED` rows and hide replies whose parent comment is not visible.
- Keep public access limited to `GET /api/v1/posts/{postId}/comments`; all mutations require JWT.
- Enforce update/delete ownership in the `Comment` domain object, not only in the controller.

## Test Note

Because `comments.parent_comment_id` is a self-referencing foreign key, integration-test cleanup must delete replies before root comments. Use test-local cleanup logic rather than adding cleanup-only methods to the production repository.

## Why

One table keeps the model simple for the MVP while still preserving a stable comment id for later notifications, mentions, reports, and moderation. One-level replies are enough for portfolio/community flow without adding recursive tree complexity early.

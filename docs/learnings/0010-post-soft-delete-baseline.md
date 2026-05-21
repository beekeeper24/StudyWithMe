# 0010 Post Soft Delete Baseline

## Context

The free-board post MVP adds public read routes and authenticated author-only write routes.

## Decision

- Store posts in `posts` with `author_member_id`, title, content, status, and timestamps.
- Use `PUBLISHED` and `DELETED` statuses instead of physical deletion.
- Public list/detail queries must filter by `PostStatus.PUBLISHED`.
- Update/delete must load only published posts, then enforce author ownership in the domain object.
- New post routes must be explicitly listed in `SecurityConfig`; keep `anyRequest().denyAll()`.

## Why

Soft delete keeps a stable post id for future comments, mentions, notifications, reports, and audit-style portfolio stories. Filtering deleted posts at repository/service lookup prevents deleted content from leaking through public APIs.

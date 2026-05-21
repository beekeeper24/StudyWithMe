# 0009. Study domain baseline

## Context

The first study domain slice adds study creation, public list/detail lookup, join, leave, and owner-only close.

## Decisions

- Keep study recruitment in the `study` package as simple Spring MVC + service + JPA code.
- Store study membership in a separate `study_members` table with unique `(study_id, member_id)`.
- Use `RECRUITING` and `CLOSED` as the first study lifecycle states.
- Use `OWNER` and `MEMBER` as the first study membership roles.
- Keep capacity, filtering, pagination API, posts, comments, chat, and notification events out of this slice.
- Reject leave attempts from non-members with `STUDY-006`; do not report success for a no-op state transition.

## Concurrency Rule

For mutating study membership/status operations, load the study through a pessimistic write lock before applying domain rules.

This matters because `join` and `close` both depend on the current study status. Without serializing those decisions, a join could pass the `RECRUITING` check while a concurrent close commits before the membership insert.

## Security Rule

- Public routes are limited to `GET /api/v1/studies` and `GET /api/v1/studies/{studyId}`.
- Mutating routes require Bearer JWT:
  - `POST /api/v1/studies`
  - `POST /api/v1/studies/{studyId}/join`
  - `POST /api/v1/studies/{studyId}/leave`
  - `POST /api/v1/studies/{studyId}/close`
- Keep `SecurityConfig.anyRequest().denyAll()` active so new routes are explicit.

## Verification

- Run focused repository, service, and controller tests after changes.
- Run full `./gradlew test --no-daemon --console=plain`.
- Boot with Docker PostgreSQL and confirm Flyway reaches version 3.

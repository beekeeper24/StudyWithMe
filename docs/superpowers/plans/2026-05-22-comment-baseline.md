# Comment Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 게시글에 댓글과 1단계 답글을 작성, 조회, 수정, 삭제할 수 있는 MVP API를 구현한다.

**Architecture:** 댓글은 `posts`에 연결되는 별도 도메인으로 둔다. 조회는 공개, 생성/수정/삭제는 JWT 인증을 요구하며 수정/삭제는 작성자만 가능하다. 삭제는 future notification/comment history를 위해 `DELETED` soft delete로 처리하고, 공개 목록에는 삭제 댓글과 삭제 부모 아래 답글을 노출하지 않는다.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, Spring Security, JUnit 5, MockMvc.

---

## File Structure

- Create `src/main/resources/db/migration/V5__create_comment_schema.sql`: `comments` table with `post_id`, `author_member_id`, nullable `parent_comment_id`, content, status, timestamps.
- Create `src/main/java/com/studywithme/comment/domain/Comment.java`: comment aggregate with author ownership, update, soft delete, reply flag.
- Create `src/main/java/com/studywithme/comment/domain/CommentStatus.java`: `PUBLISHED`, `DELETED`.
- Create `src/main/java/com/studywithme/comment/exception/CommentErrorCode.java`: comment-specific errors.
- Create `src/main/java/com/studywithme/comment/repository/CommentRepository.java`: post list, reply lookup, published lookup.
- Create `src/main/java/com/studywithme/comment/application/*`: commands, result, service.
- Create `src/main/java/com/studywithme/comment/presentation/*`: request/response/controller.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`: explicit comment route rules.
- Create repository, service, and controller tests under `src/test/java/com/studywithme/comment/`.

## Tasks

### Task 1: Schema, Domain, Repository

- [ ] Write failing repository tests for top-level comments, replies, and published lookup ordering.
- [ ] Add Flyway V5 `comments` table and indexes.
- [ ] Add `Comment`, `CommentStatus`, `CommentRepository`, and `CommentErrorCode`.
- [ ] Run `./gradlew test --tests "*CommentRepositoryTest" --no-daemon --console=plain`.

### Task 2: Service Rules

- [ ] Write failing service tests for create, reply, reject nested reply, list, author update/delete, non-author rejection, deleted parent hiding replies.
- [ ] Add command/result records and `CommentService`.
- [ ] Run `./gradlew test --tests "*CommentServiceTest" --no-daemon --console=plain`.

### Task 3: HTTP API And Security

- [ ] Write failing MockMvc tests for unauthenticated write rejection, public list, authenticated create/reply/update/delete, and non-author rejection.
- [ ] Add request/response records and `CommentController`.
- [ ] Update `SecurityConfig` with explicit comment route rules.
- [ ] Run `./gradlew test --tests "*CommentControllerTest" --no-daemon --console=plain`.

### Task 4: Verification And Handoff

- [ ] Run `./gradlew test --no-daemon --console=plain`.
- [ ] Run `git diff --check`.
- [ ] Run focused cso diff review for auth/data exposure risks.
- [ ] Update `docs/handoff.md`, add `docs/learnings/0011-comment-reply-baseline.md`, update Notion.
- [ ] Commit, open PR to `develop`, merge PR, and sync local `develop`.

## Self-Review

- Scope excludes notification fan-out, mentions, rich text, attachment, pagination cursor, and moderation.
- Replies are one level deep only; nested replies are rejected.
- Public route exposure is limited to `GET /api/v1/posts/*/comments`.
- Mutating comment routes require JWT and still enforce author ownership in domain logic.

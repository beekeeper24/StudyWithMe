# Post Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 자유게시판 글 생성, 목록/상세 조회, 수정, 삭제 MVP API를 구현한다.

**Architecture:** 기존 스터디 도메인과 같은 단순 Spring Boot 계층 구조를 따른다. 게시글은 회원이 작성하며, 조회는 공개, 변경은 JWT 인증 및 작성자 권한을 요구한다. 삭제는 이후 댓글/알림 연결을 고려해 물리 삭제가 아니라 `DELETED` 상태로 숨긴다.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Data JPA, Flyway, Spring Security, JUnit 5, MockMvc.

---

## File Structure

- Create `src/main/resources/db/migration/V4__create_post_schema.sql`: `posts` table and lookup indexes.
- Create `src/main/java/com/studywithme/post/domain/Post.java`: post aggregate with author ownership, update, soft delete.
- Create `src/main/java/com/studywithme/post/domain/PostStatus.java`: `PUBLISHED`, `DELETED`.
- Create `src/main/java/com/studywithme/post/exception/PostErrorCode.java`: `POST-001`, `POST-002`.
- Create `src/main/java/com/studywithme/post/repository/PostRepository.java`: published list and detail lookup.
- Create `src/main/java/com/studywithme/post/application/PostCreateCommand.java`: service create input.
- Create `src/main/java/com/studywithme/post/application/PostUpdateCommand.java`: service update input.
- Create `src/main/java/com/studywithme/post/application/PostResult.java`: service output.
- Create `src/main/java/com/studywithme/post/application/PostService.java`: transaction and ownership rules.
- Create `src/main/java/com/studywithme/post/presentation/PostCreateRequest.java`: create request validation.
- Create `src/main/java/com/studywithme/post/presentation/PostUpdateRequest.java`: update request validation.
- Create `src/main/java/com/studywithme/post/presentation/PostResponse.java`: API response projection.
- Create `src/main/java/com/studywithme/post/presentation/PostController.java`: `/api/v1/posts` routes.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`: explicitly permit public GET routes and require auth for write routes.
- Create `src/test/java/com/studywithme/post/repository/PostRepositoryTest.java`: repository behavior.
- Create `src/test/java/com/studywithme/post/application/PostServiceTest.java`: service rules.
- Create `src/test/java/com/studywithme/post/presentation/PostControllerTest.java`: HTTP/security behavior.

## Tasks

### Task 1: Schema, Domain, Repository

- [ ] Write failing repository tests for saving a post and excluding deleted posts from public lookups.
- [ ] Add Flyway V4 `posts` table with `author_member_id`, `title`, `content`, `status`, timestamps, FK to `members`.
- [ ] Add `Post`, `PostStatus`, `PostRepository`, and `PostErrorCode`.
- [ ] Run `./gradlew test --tests "*PostRepositoryTest" --no-daemon --console=plain` and verify pass.

### Task 2: Service Rules

- [ ] Write failing service tests for create, public list/detail, author update/delete, non-author rejection, and deleted-post hidden lookup.
- [ ] Add command/result records and `PostService`.
- [ ] Run `./gradlew test --tests "*PostServiceTest" --no-daemon --console=plain` and verify pass.

### Task 3: HTTP API And Security

- [ ] Write failing MockMvc tests for unauthenticated write rejection, public list/detail, authenticated create/update/delete, and non-author update/delete rejection.
- [ ] Add request/response records and `PostController`.
- [ ] Update `SecurityConfig` with explicit `/api/v1/posts` matchers.
- [ ] Run `./gradlew test --tests "*PostControllerTest" --no-daemon --console=plain` and verify pass.

### Task 4: Verification And Handoff

- [ ] Run `./gradlew test --no-daemon --console=plain`.
- [ ] Run `git diff --check`.
- [ ] Update `docs/handoff.md` and add a learning note under `docs/learnings/`.
- [ ] Commit, open PR to `develop`, merge PR, and sync local `develop`.

## Self-Review

- Scope is limited to free-board post CRUD. Comments, categories, search, attachments, likes, and notification fan-out are intentionally excluded.
- API paths stay under `/api/v1`.
- New routes are explicitly listed in `SecurityConfig`; default deny remains in place.
- Migration uses `V4` because `V3` is already used by the study recruitment schema.

# Study Recruitment Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Build the first study domain slice: authenticated study creation, public list/detail lookup, authenticated join/leave, and owner-only study closing.

**Architecture:** Keep the domain as a simple Spring Boot modular-monolith package under `com.studywithme.study`. Use JPA entities and Spring Data repositories directly, with a small service layer enforcing membership and owner rules. Keep notification, chat, comments, and board integration out of this PR.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring MVC, Spring Security principal injection, Spring Data JPA, Flyway, PostgreSQL/H2 tests, JUnit5, AssertJ, MockMvc.

---

## File Structure

- Create `src/main/resources/db/migration/V3__create_study_schema.sql`
  - Owns `studies` and `study_members` schema.
- Create `src/main/java/com/studywithme/study/domain/StudyStatus.java`
  - Study lifecycle values: `RECRUITING`, `CLOSED`.
- Create `src/main/java/com/studywithme/study/domain/StudyMemberRole.java`
  - Study membership roles: `OWNER`, `MEMBER`.
- Create `src/main/java/com/studywithme/study/domain/Study.java`
  - Study aggregate root and owner-controlled state transitions.
- Create `src/main/java/com/studywithme/study/domain/StudyMember.java`
  - Join table entity for study membership.
- Create `src/main/java/com/studywithme/study/repository/StudyRepository.java`
  - JPA repository for list/detail lookup.
- Create `src/main/java/com/studywithme/study/repository/StudyMemberRepository.java`
  - JPA repository for membership checks.
- Create `src/main/java/com/studywithme/study/exception/StudyErrorCode.java`
  - Domain error codes `STUDY-001` through `STUDY-005`.
- Create `src/main/java/com/studywithme/study/application/StudyCreateCommand.java`
  - Application-layer create command so the service does not depend on web DTOs.
- Create `src/main/java/com/studywithme/study/application/StudyResult.java`
  - Application-layer result record used by service and mapped by presentation responses.
- Create `src/main/java/com/studywithme/study/application/StudyService.java`
  - Use cases: create, list, detail, join, leave, close.
- Create `src/main/java/com/studywithme/study/presentation/StudyController.java`
  - REST endpoints under `/api/v1/studies`.
- Create `src/main/java/com/studywithme/study/presentation/StudyCreateRequest.java`
  - Request DTO with validation.
- Create `src/main/java/com/studywithme/study/presentation/StudyResponse.java`
  - Response DTO for detail and list items.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`
  - Permit public study list/detail, require JWT for create/join/leave/close.
- Create `src/test/java/com/studywithme/study/repository/StudyRepositoryTest.java`
  - Entity and repository behavior.
- Create `src/test/java/com/studywithme/study/application/StudyServiceTest.java`
  - Domain use-case rules.
- Create `src/test/java/com/studywithme/study/presentation/StudyControllerTest.java`
  - API/security behavior.

## API Shape

- `POST /api/v1/studies`
  - Authenticated.
  - Creates a recruiting study owned by the current member.
- `GET /api/v1/studies`
  - Public.
  - Returns newest studies first.
- `GET /api/v1/studies/{studyId}`
  - Public.
  - Returns one study.
- `POST /api/v1/studies/{studyId}/join`
  - Authenticated.
  - Adds the current member as a participant.
- `POST /api/v1/studies/{studyId}/leave`
  - Authenticated.
  - Removes the current member if they are not the owner.
- `POST /api/v1/studies/{studyId}/close`
  - Authenticated owner only.
  - Marks the study as closed.

## Scope Boundaries

- No search/filtering beyond newest-first list.
- No pagination in the first slice; keep the list limited to 50 in service code.
- No max-member capacity logic yet.
- No study board, comments, chat, notification events, or mention parsing yet.
- No update/delete endpoint yet; closing is enough to introduce owner authorization.
- Leaving a study requires existing membership; non-members receive `STUDY-006`.

## Task 1: Schema

**Files:**
- Create: `src/main/resources/db/migration/V3__create_study_schema.sql`

- [x] **Step 1: Add failing repository test that expects study tables**

Create `src/test/java/com/studywithme/study/repository/StudyRepositoryTest.java` with:

```java
package com.studywithme.study.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyMemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class StudyRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private StudyRepository studyRepository;

	@Autowired
	private StudyMemberRepository studyMemberRepository;

	@Test
	@DisplayName("스터디와 모집장 멤버십을 저장한다")
	void saveStudyWithOwnerMembership() {
		Member owner = memberRepository.saveAndFlush(Member.createOAuthMember(
			"owner@example.com",
			"owner",
			OAuthProvider.GOOGLE,
			"google-owner",
			null
		));
		Study study = studyRepository.saveAndFlush(Study.create(
			"알고리즘 스터디",
			"매주 알고리즘 문제를 풀고 리뷰합니다.",
			owner.getId()
		));
		StudyMember membership = studyMemberRepository.saveAndFlush(StudyMember.owner(study.getId(), owner.getId()));

		assertThat(study.getId()).isNotNull();
		assertThat(study.getOwnerMemberId()).isEqualTo(owner.getId());
		assertThat(membership.getRole()).isEqualTo(StudyMemberRole.OWNER);
	}
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests StudyRepositoryTest --no-daemon --console=plain
```

Expected: compile failure because `Study`, `StudyRepository`, and related classes do not exist.

- [x] **Step 3: Add migration**

Create `src/main/resources/db/migration/V3__create_study_schema.sql`:

```sql
create table studies (
    id bigint generated by default as identity,
    owner_member_id bigint not null,
    title varchar(100) not null,
    description varchar(2000) not null,
    status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint fk_studies_owner_member foreign key (owner_member_id) references members (id)
);

create index idx_studies_status_created_at on studies (status, created_at desc);
create index idx_studies_owner_member_id on studies (owner_member_id);

create table study_members (
    id bigint generated by default as identity,
    study_id bigint not null,
    member_id bigint not null,
    role varchar(20) not null,
    joined_at timestamp not null,
    primary key (id),
    constraint fk_study_members_study foreign key (study_id) references studies (id),
    constraint fk_study_members_member foreign key (member_id) references members (id),
    constraint uk_study_members_study_member unique (study_id, member_id)
);

create index idx_study_members_member_id on study_members (member_id);
```

## Task 2: Domain And Repositories

**Files:**
- Create: `src/main/java/com/studywithme/study/domain/StudyStatus.java`
- Create: `src/main/java/com/studywithme/study/domain/StudyMemberRole.java`
- Create: `src/main/java/com/studywithme/study/domain/Study.java`
- Create: `src/main/java/com/studywithme/study/domain/StudyMember.java`
- Create: `src/main/java/com/studywithme/study/exception/StudyErrorCode.java`
- Create: `src/main/java/com/studywithme/study/repository/StudyRepository.java`
- Create: `src/main/java/com/studywithme/study/repository/StudyMemberRepository.java`
- Test: `src/test/java/com/studywithme/study/repository/StudyRepositoryTest.java`

- [x] **Step 1: Add domain enums**

Create `StudyStatus`:

```java
package com.studywithme.study.domain;

public enum StudyStatus {
	RECRUITING,
	CLOSED
}
```

Create `StudyMemberRole`:

```java
package com.studywithme.study.domain;

public enum StudyMemberRole {
	OWNER,
	MEMBER
}
```

- [x] **Step 2: Add study error codes**

Create `StudyErrorCode`:

```java
package com.studywithme.study.exception;

import com.studywithme.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StudyErrorCode implements ErrorCode {
	STUDY_NOT_FOUND("STUDY-001", "스터디를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	STUDY_ALREADY_CLOSED("STUDY-002", "이미 모집이 종료된 스터디입니다.", HttpStatus.CONFLICT),
	ALREADY_JOINED("STUDY-003", "이미 참여한 스터디입니다.", HttpStatus.CONFLICT),
	NOT_STUDY_OWNER("STUDY-004", "스터디 모집장만 수행할 수 있습니다.", HttpStatus.FORBIDDEN),
	OWNER_CANNOT_LEAVE("STUDY-005", "스터디 모집장은 탈퇴할 수 없습니다.", HttpStatus.CONFLICT),
	NOT_STUDY_MEMBER("STUDY-006", "참여하지 않은 스터디입니다.", HttpStatus.CONFLICT);

	private final String code;
	private final String message;
	private final HttpStatus status;

	StudyErrorCode(String code, String message, HttpStatus status) {
		this.code = code;
		this.message = message;
		this.status = status;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}
}
```

- [x] **Step 3: Add Study entity**

Create `Study.java` with fields `id`, `ownerMemberId`, `title`, `description`, `status`, `createdAt`, `updatedAt`, a `create` factory, `close(Long requesterMemberId)`, and getters. `close` should throw `new BusinessException(StudyErrorCode.NOT_STUDY_OWNER)` if requester is not owner.

- [x] **Step 4: Add StudyMember entity**

Create `StudyMember.java` with fields `id`, `studyId`, `memberId`, `role`, `joinedAt`, factories `owner(studyId, memberId)` and `member(studyId, memberId)`, and getters.

- [x] **Step 5: Add repositories**

Create `StudyRepository`:

```java
package com.studywithme.study.repository;

import com.studywithme.study.domain.Study;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRepository extends JpaRepository<Study, Long> {

	List<Study> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

Create `StudyMemberRepository`:

```java
package com.studywithme.study.repository;

import com.studywithme.study.domain.StudyMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

	boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

	Optional<StudyMember> findByStudyIdAndMemberId(Long studyId, Long memberId);

	void deleteByStudyIdAndMemberId(Long studyId, Long memberId);
}
```

- [x] **Step 6: Run repository test**

Run:

```bash
./gradlew test --tests StudyRepositoryTest --no-daemon --console=plain
```

Expected: pass.

## Task 3: Study Service Rules

**Files:**
- Create: `src/main/java/com/studywithme/study/application/StudyCreateCommand.java`
- Create: `src/main/java/com/studywithme/study/application/StudyResult.java`
- Create: `src/main/java/com/studywithme/study/application/StudyService.java`
- Test: `src/test/java/com/studywithme/study/application/StudyServiceTest.java`

- [x] **Step 1: Write service tests**

Cover these rules in `StudyServiceTest`:

- Creating a study creates OWNER membership.
- Joining a study creates MEMBER membership.
- Joining twice returns `STUDY-003`.
- Owner cannot leave their own study and receives `STUDY-005`.
- Non-member cannot leave a study and receives `STUDY-006`.
- Non-owner cannot close a study and receives `STUDY-004`.
- Unknown study id returns `STUDY-001`.

- [x] **Step 2: Add application records**

Create `StudyCreateCommand`:

```java
package com.studywithme.study.application;

public record StudyCreateCommand(
	String title,
	String description
) {
}
```

Create `StudyResult`:

```java
package com.studywithme.study.application;

import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyStatus;
import java.time.LocalDateTime;

public record StudyResult(
	Long id,
	Long ownerMemberId,
	String title,
	String description,
	StudyStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static StudyResult from(Study study) {
		return new StudyResult(
			study.getId(),
			study.getOwnerMemberId(),
			study.getTitle(),
			study.getDescription(),
			study.getStatus(),
			study.getCreatedAt(),
			study.getUpdatedAt()
		);
	}
}
```

- [x] **Step 3: Implement StudyService**

Create methods:

```java
public StudyResult create(Long requesterMemberId, StudyCreateCommand command)
public List<StudyResult> findAll()
public StudyResult findById(Long studyId)
public StudyResult join(Long studyId, Long requesterMemberId)
public StudyResult leave(Long studyId, Long requesterMemberId)
public StudyResult close(Long studyId, Long requesterMemberId)
```

Use `@Transactional` for mutations and `@Transactional(readOnly = true)` for reads.

- [x] **Step 4: Run service tests**

Run:

```bash
./gradlew test --tests StudyServiceTest --no-daemon --console=plain
```

Expected: pass.

## Task 4: HTTP API And Security

**Files:**
- Create: `src/main/java/com/studywithme/study/presentation/StudyCreateRequest.java`
- Create: `src/main/java/com/studywithme/study/presentation/StudyResponse.java`
- Create: `src/main/java/com/studywithme/study/presentation/StudyController.java`
- Modify: `src/main/java/com/studywithme/global/security/SecurityConfig.java`
- Test: `src/test/java/com/studywithme/study/presentation/StudyControllerTest.java`

- [x] **Step 1: Write controller tests**

Cover these API behaviors:

- Unauthenticated `POST /api/v1/studies` returns `AUTH-003`.
- Authenticated `POST /api/v1/studies` returns `200` with title/status/owner id.
- Public `GET /api/v1/studies` returns `200`.
- Public `GET /api/v1/studies/{studyId}` returns `200`.
- Authenticated `POST /api/v1/studies/{studyId}/join` returns `200`.
- Authenticated owner `POST /api/v1/studies/{studyId}/close` returns `200` and status `CLOSED`.

- [x] **Step 2: Add request and response DTOs**

`StudyCreateRequest`:

```java
package com.studywithme.study.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank @Size(max = 2000) String description
) {
}
```

`StudyResponse` should include `id`, `ownerMemberId`, `title`, `description`, `status`, `createdAt`, and `updatedAt`, and expose `static StudyResponse from(StudyResult result)`.

- [x] **Step 3: Add controller**

Use `@RestController`, `@RequestMapping("/api/v1/studies")`, `@Valid`, and `@AuthenticationPrincipal AuthenticatedMemberPrincipal`. Convert `StudyCreateRequest` to `new StudyCreateCommand(request.title(), request.description())` before calling the service.

- [x] **Step 4: Update SecurityConfig**

Add:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/studies", "/api/v1/studies/*").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/studies", "/api/v1/studies/*/join", "/api/v1/studies/*/leave", "/api/v1/studies/*/close").authenticated()
```

Keep `.anyRequest().denyAll()`.

- [x] **Step 5: Run controller tests**

Run:

```bash
./gradlew test --tests StudyControllerTest --no-daemon --console=plain
```

Expected: pass.

## Task 5: Full Verification And Handoff

**Files:**
- Modify: `docs/handoff.md`
- Create or update: `docs/learnings/0009-study-domain-baseline.md`
- Update Notion latest StudyWithMe work log.

- [x] **Step 1: Run full test suite**

Run:

```bash
./gradlew test --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: Run migration sanity with PostgreSQL**

Run:

```bash
docker compose up -d postgres
./gradlew bootRun --no-daemon --console=plain
```

Expected: app starts on `8081`, Flyway validates migrations, schema version reaches `3`.

- [x] **Step 3: Security review scope**

Because this feature adds authenticated create/join/leave/close and public list/detail routes, run a focused security review for:

- public routes are only `GET /api/v1/studies` and `GET /api/v1/studies/{studyId}`;
- mutating routes require JWT;
- owner-only close cannot be performed by a different authenticated member;
- non-members cannot receive a successful leave response;
- `.anyRequest().denyAll()` remains active.

- [x] **Step 4: Commit**

Commit with a Korean message:

```bash
git add src/main src/test docs
git commit -m "스터디 모집 기본 기능을 추가한다"
```

## Self-Review

- Spec coverage: The plan covers study create/list/detail/join/leave/close, owner authorization, and non-member leave rejection. It intentionally excludes notification, chat, board, comments, capacity, search, and pagination.
- Placeholder scan: No unresolved placeholder markers or unconstrained "add tests" steps remain.
- Type consistency: `StudyStatus`, `StudyMemberRole`, `StudyService`, `StudyCreateCommand`, `StudyResult`, `StudyCreateRequest`, and `StudyResponse` names are used consistently across tasks.

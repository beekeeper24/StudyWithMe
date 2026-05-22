# Chat MVP Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 인증한 회원이 1:1 채팅방과 스터디 채팅방을 만들고, 참여자만 메시지를 쓰고 읽을 수 있는 REST 기반 채팅 MVP를 만든다.

**Architecture:** 채팅은 `chat` 모듈 안에 `ChatRoom`, `ChatRoomMember`, `ChatMessage`를 둔 단순 JPA 모델로 시작한다. WebSocket, 읽음 처리, 알림 연동은 이번 범위에서 제외하고, 접근제어와 메시지 저장/조회 흐름을 먼저 고정한다.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL, Spring Security, MockMvc, AssertJ

---

## File Structure

- Create `src/main/resources/db/migration/V8__create_chat_schema.sql`: 채팅방, 채팅방 참여자, 메시지 테이블 생성.
- Create `src/main/java/com/studywithme/chat/domain/ChatRoomType.java`: `PRIVATE`, `STUDY` 타입.
- Create `src/main/java/com/studywithme/chat/domain/ChatRoom.java`: 방 타입, 고유 키, 스터디 id, 생성시각 보관.
- Create `src/main/java/com/studywithme/chat/domain/ChatRoomMember.java`: 방 참여자와 참여시각 보관.
- Create `src/main/java/com/studywithme/chat/domain/ChatMessage.java`: 방 id, 발신자 id, 내용, 생성시각 보관.
- Create `src/main/java/com/studywithme/chat/repository/ChatRoomRepository.java`: 방 키 조회와 내 방 목록 조회.
- Create `src/main/java/com/studywithme/chat/repository/ChatRoomMemberRepository.java`: 참여 여부 확인과 방 참여자 저장.
- Create `src/main/java/com/studywithme/chat/repository/ChatMessageRepository.java`: 방 메시지 생성순 조회.
- Create `src/main/java/com/studywithme/chat/exception/ChatErrorCode.java`: 채팅 전용 오류 코드.
- Create `src/main/java/com/studywithme/chat/application/ChatService.java`: 방 생성, 목록, 메시지 저장/조회 정책.
- Create `src/main/java/com/studywithme/chat/application/ChatMessageCreateCommand.java`: 메시지 작성 명령.
- Create `src/main/java/com/studywithme/chat/application/ChatRoomResult.java`: 방 응답용 결과.
- Create `src/main/java/com/studywithme/chat/application/ChatMessageResult.java`: 메시지 응답용 결과.
- Create `src/main/java/com/studywithme/chat/presentation/ChatController.java`: REST API 엔드포인트.
- Create `src/main/java/com/studywithme/chat/presentation/PrivateChatRoomCreateRequest.java`: 1:1 방 생성 요청.
- Create `src/main/java/com/studywithme/chat/presentation/ChatMessageCreateRequest.java`: 메시지 작성 요청.
- Create `src/main/java/com/studywithme/chat/presentation/ChatRoomResponse.java`: 방 응답.
- Create `src/main/java/com/studywithme/chat/presentation/ChatMessageResponse.java`: 메시지 응답.
- Modify `src/main/java/com/studywithme/study/repository/StudyMemberRepository.java`: 스터디 참여자 전체 조회 추가.
- Modify `src/main/java/com/studywithme/member/repository/MemberRepository.java`: ACTIVE 회원 존재 확인 추가.
- Modify `src/main/java/com/studywithme/global/security/SecurityConfig.java`: 채팅 API 인증 허용.
- Create `src/test/java/com/studywithme/chat/application/ChatServiceTest.java`: 핵심 정책 단위 테스트.
- Create `src/test/java/com/studywithme/chat/presentation/ChatControllerTest.java`: HTTP 인증/인가 테스트.

## Tasks

### Task 1: Service RED Tests

**Files:**
- Create: `src/test/java/com/studywithme/chat/application/ChatServiceTest.java`

- [ ] **Step 1: Write failing service tests**

```java
@Test
@DisplayName("1:1 채팅방은 두 회원 조합당 하나만 생성된다")
void createPrivateRoomReturnsExistingRoomForSamePair() {
	Member requester = saveMember("requester");
	Member target = saveMember("target");

	ChatRoomResult first = chatService.createPrivateRoom(requester.getId(), target.getId());
	ChatRoomResult second = chatService.createPrivateRoom(target.getId(), requester.getId());

	assertThat(second.id()).isEqualTo(first.id());
	assertThat(second.type()).isEqualTo(ChatRoomType.PRIVATE);
}

@Test
@DisplayName("채팅방 참여자만 메시지를 작성할 수 있다")
void rejectMessageFromNonRoomMember() {
	Member requester = saveMember("requester");
	Member target = saveMember("target");
	Member outsider = saveMember("outsider");
	ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

	assertThatThrownBy(() -> chatService.sendMessage(
		room.id(),
		outsider.getId(),
		new ChatMessageCreateCommand("안녕하세요")
	))
		.isInstanceOf(BusinessException.class)
		.extracting("errorCode")
		.isEqualTo(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
}
```

- [ ] **Step 2: Run service tests to verify RED**

Run: `./gradlew test --tests com.studywithme.chat.application.ChatServiceTest --no-daemon --console=plain`

Expected: compile failure because `ChatService`, result records, and domain types do not exist yet.

### Task 2: Schema And Service GREEN

**Files:**
- Create: all `chat/domain`, `chat/repository`, `chat/application`, `chat/exception` files listed above.
- Modify: `src/main/java/com/studywithme/study/repository/StudyMemberRepository.java`
- Modify: `src/main/java/com/studywithme/member/repository/MemberRepository.java`
- Create: `src/main/resources/db/migration/V8__create_chat_schema.sql`

- [ ] **Step 1: Implement minimal schema and service**

Use these policies:

- Private room key is `PRIVATE:{smallerMemberId}:{largerMemberId}`.
- Study room key is `STUDY:{studyId}`.
- A private room cannot target the requester.
- A private room target must be an ACTIVE member.
- A study room requester must already be in `study_members`.
- Message sender and reader must be in `chat_room_members`.
- Message content is stored as provided after request validation; max length is enforced at request and DB levels.

- [ ] **Step 2: Run service tests to verify GREEN**

Run: `./gradlew test --tests com.studywithme.chat.application.ChatServiceTest --no-daemon --console=plain`

Expected: PASS.

### Task 3: Controller RED Tests

**Files:**
- Create: `src/test/java/com/studywithme/chat/presentation/ChatControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

```java
@Test
@DisplayName("인증한 회원은 1:1 채팅방을 만들 수 있다")
void createPrivateRoom() throws Exception {
	Member requester = saveMember("requester");
	Member target = saveMember("target");

	mockMvc.perform(post("/api/v1/chat/private-rooms")
			.header("Authorization", "Bearer " + accessToken(requester))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(new PrivateChatRoomCreateRequest(target.getId()))))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.success").value(true))
		.andExpect(jsonPath("$.data.type").value("PRIVATE"));
}

@Test
@DisplayName("채팅방 참여자가 아니면 메시지 목록을 조회할 수 없다")
void rejectReadMessagesByNonRoomMember() throws Exception {
	Member requester = saveMember("requester");
	Member target = saveMember("target");
	Member outsider = saveMember("outsider");
	ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

	mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/messages", room.id())
			.header("Authorization", "Bearer " + accessToken(outsider)))
		.andExpect(status().isForbidden())
		.andExpect(jsonPath("$.success").value(false))
		.andExpect(jsonPath("$.error.code").value("CHAT-002"));
}
```

- [ ] **Step 2: Run controller tests to verify RED**

Run: `./gradlew test --tests com.studywithme.chat.presentation.ChatControllerTest --no-daemon --console=plain`

Expected: compile failure or 403/404 because controller and security matchers are not implemented yet.

### Task 4: Controller And Security GREEN

**Files:**
- Create: all `chat/presentation` files listed above.
- Modify: `src/main/java/com/studywithme/global/security/SecurityConfig.java`

- [ ] **Step 1: Implement REST endpoints**

Expose:

- `POST /api/v1/chat/private-rooms`
- `POST /api/v1/studies/{studyId}/chat-room`
- `GET /api/v1/chat/rooms`
- `POST /api/v1/chat/rooms/{roomId}/messages`
- `GET /api/v1/chat/rooms/{roomId}/messages`

- [ ] **Step 2: Run controller tests to verify GREEN**

Run: `./gradlew test --tests com.studywithme.chat.presentation.ChatControllerTest --no-daemon --console=plain`

Expected: PASS.

### Task 5: Verification And Handoff

**Files:**
- Modify: `docs/handoff.md`
- Create: `docs/learnings/0017-chat-mvp-baseline.md`
- Update: Notion dated work log under `작업일지 > StudyWithMe`

- [ ] **Step 1: Run full verification**

Run:

```bash
./gradlew test --no-daemon --console=plain
git diff --check
```

Expected: tests pass and whitespace check has no output.

- [ ] **Step 2: Run focused security review**

Run gstack `cso` focused on chat access control, chat message data exposure, and authorization boundaries.

- [ ] **Step 3: Update docs and Notion**

Record that WebSocket delivery, unread counts, read receipts, moderation, and Kafka-backed chat events remain future work.

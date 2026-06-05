package com.studywithme.chat.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.chat.application.ChatMessageCreateCommand;
import com.studywithme.chat.application.ChatRoomResult;
import com.studywithme.chat.application.ChatService;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import com.studywithme.chat.repository.ChatMessageReportRepository;
import com.studywithme.chat.repository.ChatMessageRepository;
import com.studywithme.chat.repository.ChatRoomMemberRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.application.StudyCreateCommand;
import com.studywithme.study.application.StudyResult;
import com.studywithme.study.application.StudyService;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatRoomMemberRepository chatRoomMemberRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatMessageReportRepository chatMessageReportRepository;

	@Autowired
	private StudyRepository studyRepository;

	@Autowired
	private StudyMemberRepository studyMemberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private ChatService chatService;

	@Autowired
	private StudyService studyService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private SimpMessagingTemplate messagingTemplate;

	@AfterEach
	void tearDown() {
		chatMessageReportRepository.deleteAll();
		chatMessageRepository.deleteAll();
		chatRoomMemberRepository.deleteAll();
		chatRoomRepository.deleteAll();
		studyMemberRepository.deleteAll();
		studyRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증하지 않고 1:1 채팅방을 만들면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedCreatePrivateRoom() throws Exception {
		mockMvc.perform(post("/api/v1/chat/private-rooms")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new PrivateChatRoomCreateRequest(1L))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

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
	@DisplayName("인증한 스터디 참여자는 스터디 채팅방을 만들 수 있다")
	void createStudyRoom() throws Exception {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		mockMvc.perform(post("/api/v1/studies/{studyId}/chat-room", study.id())
				.header("Authorization", "Bearer " + accessToken(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.type").value("STUDY"))
			.andExpect(jsonPath("$.data.studyId").value(study.id()))
			.andExpect(jsonPath("$.data.title").value("알고리즘 스터디"));
	}

	@Test
	@DisplayName("인증한 회원은 자신이 참여한 채팅방 목록만 조회한다")
	void findMyRooms() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		Member outsider = saveMember("outsider");
		ChatRoomResult myRoom = chatService.createPrivateRoom(requester.getId(), target.getId());
		chatService.createPrivateRoom(target.getId(), outsider.getId());
		chatService.sendMessage(
			myRoom.id(),
			target.getId(),
			new ChatMessageCreateCommand("새 메시지입니다")
		);

		mockMvc.perform(get("/api/v1/chat/rooms")
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].id").value(myRoom.id()))
			.andExpect(jsonPath("$.data[0].title").value("target"))
			.andExpect(jsonPath("$.data[0].lastMessageContent").value("새 메시지입니다"))
			.andExpect(jsonPath("$.data[0].lastMessageSenderMemberId").value(target.getId()))
			.andExpect(jsonPath("$.data[0].lastMessageCreatedAt").exists())
			.andExpect(jsonPath("$.data[0].unreadCount").value(1));
	}

	@Test
	@DisplayName("채팅방 참여자는 채팅방 멤버 목록을 조회할 수 있다")
	void findRoomMembers() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/members", room.id())
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].memberId").exists())
			.andExpect(jsonPath("$.data[0].nickname").exists())
			.andExpect(jsonPath("$.data[0].joinedAt").exists());
	}

	@Test
	@DisplayName("인증한 채팅방 참여자는 채팅방을 내 목록에서 삭제할 수 있다")
	void hideRoom() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}", room.id())
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/api/v1/chat/rooms")
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("채팅방 참여자가 아니면 채팅방 멤버 목록을 조회할 수 없다")
	void rejectFindRoomMembersByNonRoomMember() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		Member outsider = saveMember("outsider");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/members", room.id())
				.header("Authorization", "Bearer " + accessToken(outsider)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("CHAT-002"));
	}

	@Test
	@DisplayName("채팅방 참여자는 메시지를 작성할 수 있다")
	void sendMessage() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/messages", room.id())
				.header("Authorization", "Bearer " + accessToken(requester))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ChatMessageCreateRequest("안녕하세요"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.senderMemberId").value(requester.getId()))
			.andExpect(jsonPath("$.data.content").value("안녕하세요"));
	}

	@Test
	@DisplayName("채팅방 참여자는 메시지 목록을 조회할 수 있다")
	void findMessages() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());
		chatService.sendMessage(room.id(), requester.getId(), new ChatMessageCreateCommand("안녕하세요"));

		mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/messages", room.id())
				.header("Authorization", "Bearer " + accessToken(target)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].content").value("안녕하세요"))
			.andExpect(jsonPath("$.data[0].readMemberCount").value(1));
	}

	@Test
	@DisplayName("메시지 작성자는 자신이 보낸 메시지를 삭제할 수 있다")
	void deleteMessageBySender() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());
		var message = chatService.sendMessage(room.id(), requester.getId(), new ChatMessageCreateCommand("삭제할 메시지"));

		mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}/messages/{messageId}", room.id(), message.id())
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(message.id()))
			.andExpect(jsonPath("$.data.content").value("삭제된 메시지입니다."))
			.andExpect(jsonPath("$.data.deleted").value(true));

		verify(messagingTemplate).convertAndSendToUser(
			eq(requester.getId().toString()),
			eq("/queue/chat.rooms." + room.id()),
			org.mockito.ArgumentMatchers.any(ChatMessageResponse.class)
		);
		verify(messagingTemplate).convertAndSendToUser(
			eq(target.getId().toString()),
			eq("/queue/chat.rooms." + room.id()),
			org.mockito.ArgumentMatchers.any(ChatMessageResponse.class)
		);
	}

	@Test
	@DisplayName("메시지 작성자가 아니면 메시지를 삭제할 수 없다")
	void rejectDeleteMessageByNonSender() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());
		var message = chatService.sendMessage(room.id(), requester.getId(), new ChatMessageCreateCommand("삭제할 메시지"));

		mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}/messages/{messageId}", room.id(), message.id())
				.header("Authorization", "Bearer " + accessToken(target)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("CHAT-007"));
	}

	@Test
	@DisplayName("채팅방 참여자는 다른 사람이 보낸 메시지를 신고할 수 있다")
	void reportMessage() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());
		var message = chatService.sendMessage(room.id(), target.getId(), new ChatMessageCreateCommand("신고 대상 메시지"));

		mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/messages/{messageId}/reports", room.id(), message.id())
				.header("Authorization", "Bearer " + accessToken(requester))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ChatMessageReportRequest("부적절한 표현입니다."))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.messageId").value(message.id()))
			.andExpect(jsonPath("$.data.reporterMemberId").value(requester.getId()))
			.andExpect(jsonPath("$.data.reporterNickname").isEmpty())
			.andExpect(jsonPath("$.data.reportedMemberId").value(target.getId()))
			.andExpect(jsonPath("$.data.reportedNickname").isEmpty())
			.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	@DisplayName("관리자는 채팅 메시지 신고 목록을 조회하고 처리할 수 있다")
	void findAndHandleReportsByAdmin() throws Exception {
		Member reporter = saveMember("reporter");
		Member target = saveMember("target");
		Member admin = saveAdmin("admin");
		ChatRoomResult room = chatService.createPrivateRoom(reporter.getId(), target.getId());
		var message = chatService.sendMessage(room.id(), target.getId(), new ChatMessageCreateCommand("신고 대상 메시지"));
		var report = chatService.reportMessage(room.id(), message.id(), reporter.getId(), "관리자 확인이 필요합니다.");

		mockMvc.perform(get("/api/v1/admin/chat-message-reports")
				.param("status", ChatMessageReportStatus.PENDING.name())
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].id").value(report.id()))
			.andExpect(jsonPath("$.data[0].reporterNickname").value("reporter"))
			.andExpect(jsonPath("$.data[0].reportedNickname").value("target"))
			.andExpect(jsonPath("$.data[0].handlerNickname").isEmpty())
			.andExpect(jsonPath("$.data[0].messageContent").value("신고 대상 메시지"));

		mockMvc.perform(post("/api/v1/admin/chat-message-reports/{reportId}/assign", report.id())
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.assignedAdminMemberId").value(admin.getId()))
			.andExpect(jsonPath("$.data.assignedAdminNickname").value("admin"))
			.andExpect(jsonPath("$.data.assignedAt").exists());

		mockMvc.perform(post("/api/v1/admin/chat-message-reports/{reportId}/handle", report.id())
				.header("Authorization", "Bearer " + accessToken(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ChatMessageReportHandleRequest(
					ChatMessageReportStatus.RESOLVED,
					"확인 완료"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("RESOLVED"))
			.andExpect(jsonPath("$.data.handlerMemberId").value(admin.getId()))
			.andExpect(jsonPath("$.data.handlerNickname").value("admin"))
			.andExpect(jsonPath("$.data.handlingNote").value("확인 완료"));
	}

	@Test
	@DisplayName("관리자가 아니면 채팅 메시지 신고 목록을 조회할 수 없다")
	void rejectFindReportsByNonAdmin() throws Exception {
		Member requester = saveMember("requester");

		mockMvc.perform(get("/api/v1/admin/chat-message-reports")
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("CHAT-010"));
	}

	@Test
	@DisplayName("채팅 메시지 신고 처리 메모는 500자를 초과할 수 없다")
	void rejectTooLongReportHandlingNote() throws Exception {
		Member reporter = saveMember("note-reporter");
		Member target = saveMember("note-target");
		Member admin = saveAdmin("note-admin");
		ChatRoomResult room = chatService.createPrivateRoom(reporter.getId(), target.getId());
		var message = chatService.sendMessage(room.id(), target.getId(), new ChatMessageCreateCommand("신고 대상 메시지"));
		var report = chatService.reportMessage(room.id(), message.id(), reporter.getId(), "관리자 확인이 필요합니다.");

		mockMvc.perform(post("/api/v1/admin/chat-message-reports/{reportId}/handle", report.id())
				.header("Authorization", "Bearer " + accessToken(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ChatMessageReportHandleRequest(
					ChatMessageReportStatus.RESOLVED,
					"a".repeat(501)
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("GLOBAL-400"));
	}

	@Test
	@DisplayName("관리자는 채팅 메시지 신고를 전체와 처리 상태별로 조회할 수 있다")
	void findReportsByStatusForAdmin() throws Exception {
		Member reporter = saveMember("history-reporter");
		Member firstTarget = saveMember("history-first-target");
		Member secondTarget = saveMember("history-second-target");
		Member admin = saveAdmin("history-admin");
		ChatRoomResult firstRoom = chatService.createPrivateRoom(reporter.getId(), firstTarget.getId());
		var firstMessage = chatService.sendMessage(
			firstRoom.id(),
			firstTarget.getId(),
			new ChatMessageCreateCommand("처리된 신고 대상 메시지")
		);
		var resolvedReport = chatService.reportMessage(
			firstRoom.id(),
			firstMessage.id(),
			reporter.getId(),
			"처리 대상입니다."
		);
		chatService.assignMessageReport(resolvedReport.id(), admin.getId());
		chatService.handleMessageReport(
			resolvedReport.id(),
			admin.getId(),
			ChatMessageReportStatus.RESOLVED,
			"확인 완료"
		);
		ChatRoomResult secondRoom = chatService.createPrivateRoom(reporter.getId(), secondTarget.getId());
		var secondMessage = chatService.sendMessage(
			secondRoom.id(),
			secondTarget.getId(),
			new ChatMessageCreateCommand("대기 중 신고 대상 메시지")
		);
		var pendingReport = chatService.reportMessage(
			secondRoom.id(),
			secondMessage.id(),
			reporter.getId(),
			"대기 대상입니다."
		);

		mockMvc.perform(get("/api/v1/admin/chat-message-reports")
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].id").value(pendingReport.id()))
			.andExpect(jsonPath("$.data[0].status").value("PENDING"))
			.andExpect(jsonPath("$.data[1].id").value(resolvedReport.id()))
			.andExpect(jsonPath("$.data[1].status").value("RESOLVED"));
		mockMvc.perform(get("/api/v1/admin/chat-message-reports")
				.param("status", ChatMessageReportStatus.RESOLVED.name())
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].id").value(resolvedReport.id()))
			.andExpect(jsonPath("$.data[0].status").value("RESOLVED"));
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

	@Test
	@DisplayName("빈 메시지를 작성하면 GLOBAL-400 응답을 반환한다")
	void rejectBlankMessage() throws Exception {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/messages", room.id())
				.header("Authorization", "Bearer " + accessToken(requester))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ChatMessageCreateRequest(" "))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("GLOBAL-400"));
	}

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}

	private Member saveAdmin(String name) {
		Member member = Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		);
		member.grantRole(MemberRole.ADMIN);
		return memberRepository.saveAndFlush(member);
	}

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}

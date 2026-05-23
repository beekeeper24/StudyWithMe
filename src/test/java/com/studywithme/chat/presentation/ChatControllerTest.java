package com.studywithme.chat.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.chat.application.ChatMessageCreateCommand;
import com.studywithme.chat.application.ChatRoomResult;
import com.studywithme.chat.application.ChatService;
import com.studywithme.chat.repository.ChatMessageRepository;
import com.studywithme.chat.repository.ChatRoomMemberRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.member.domain.Member;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

	@AfterEach
	void tearDown() {
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

		mockMvc.perform(get("/api/v1/chat/rooms")
				.header("Authorization", "Bearer " + accessToken(requester)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].id").value(myRoom.id()))
			.andExpect(jsonPath("$.data[0].title").value("target"));
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
			.andExpect(jsonPath("$.data[0].content").value("안녕하세요"));
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

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}

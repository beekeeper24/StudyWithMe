package com.studywithme.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.studywithme.chat.domain.ChatRoomType;
import com.studywithme.chat.exception.ChatErrorCode;
import com.studywithme.chat.repository.ChatRoomMemberRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.study.application.StudyCreateCommand;
import com.studywithme.study.application.StudyResult;
import com.studywithme.study.application.StudyService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({
	ChatService.class,
	StudyService.class
})
class ChatServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatRoomMemberRepository chatRoomMemberRepository;

	@Autowired
	private ChatService chatService;

	@Autowired
	private StudyService studyService;

	@MockitoBean
	private OutboxEventPublisher outboxEventPublisher;

	@Test
	@DisplayName("1:1 채팅방은 두 회원 조합당 하나만 생성된다")
	void createPrivateRoomReturnsExistingRoomForSamePair() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");

		ChatRoomResult first = chatService.createPrivateRoom(requester.getId(), target.getId());
		ChatRoomResult second = chatService.createPrivateRoom(target.getId(), requester.getId());

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(second.type()).isEqualTo(ChatRoomType.PRIVATE);
		assertThat(chatRoomRepository.count()).isEqualTo(1);
		assertThat(chatRoomMemberRepository.findAllByRoomId(first.id())).hasSize(2);
		verify(outboxEventPublisher).publishPrivateChatRequested(first.id(), target.getId(), requester.getId());
		verify(outboxEventPublisher, never()).publishPrivateChatRequested(second.id(), requester.getId(), target.getId());
	}

	@Test
	@DisplayName("자기 자신과 1:1 채팅방을 만들 수 없다")
	void rejectPrivateRoomWithSelf() {
		Member requester = saveMember("requester");

		assertThatThrownBy(() -> chatService.createPrivateRoom(requester.getId(), requester.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.INVALID_PRIVATE_ROOM_TARGET);
	}

	@Test
	@DisplayName("스터디 채팅방은 현재 스터디 참여자들을 방 참여자로 등록한다")
	void createStudyRoomAddsStudyMembers() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());

		ChatRoomResult room = chatService.createStudyRoom(study.id(), owner.getId());

		assertThat(room.type()).isEqualTo(ChatRoomType.STUDY);
		assertThat(room.studyId()).isEqualTo(study.id());
		assertThat(chatRoomMemberRepository.existsByRoomIdAndMemberId(room.id(), owner.getId())).isTrue();
		assertThat(chatRoomMemberRepository.existsByRoomIdAndMemberId(room.id(), participant.getId())).isTrue();
	}

	@Test
	@DisplayName("이미 생성된 스터디 채팅방은 새 스터디 참여자도 방 참여자로 동기화한다")
	void existingStudyRoomAddsNewStudyMember() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		ChatRoomResult room = chatService.createStudyRoom(study.id(), owner.getId());
		studyService.join(study.id(), participant.getId());

		ChatRoomResult existingRoom = chatService.createStudyRoom(study.id(), participant.getId());

		assertThat(existingRoom.id()).isEqualTo(room.id());
		assertThat(chatRoomMemberRepository.existsByRoomIdAndMemberId(room.id(), participant.getId())).isTrue();
	}

	@Test
	@DisplayName("스터디 참여자가 아니면 스터디 채팅방을 만들 수 없다")
	void rejectStudyRoomCreationByNonStudyMember() {
		Member owner = saveMember("owner");
		Member outsider = saveMember("outsider");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);

		assertThatThrownBy(() -> chatService.createStudyRoom(study.id(), outsider.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
	}

	@Test
	@DisplayName("스터디 탈퇴자는 기존 스터디 채팅방이 내 채팅방 목록에서 제외된다")
	void excludeStudyRoomAfterLeavingStudy() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());
		ChatRoomResult room = chatService.createStudyRoom(study.id(), participant.getId());

		studyService.leave(study.id(), participant.getId());

		assertThat(chatService.findMyRooms(participant.getId())).extracting(ChatRoomResult::id)
			.doesNotContain(room.id());
		assertThat(chatService.findMyRooms(owner.getId())).extracting(ChatRoomResult::id)
			.contains(room.id());
	}

	@Test
	@DisplayName("스터디 탈퇴자는 기존 스터디 채팅방 메시지를 조회할 수 없다")
	void rejectStudyRoomMessageReadAfterLeavingStudy() {
		Member owner = saveMember("owner");
		Member participant = saveMember("participant");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		studyService.join(study.id(), participant.getId());
		ChatRoomResult room = chatService.createStudyRoom(study.id(), participant.getId());

		studyService.leave(study.id(), participant.getId());

		assertThatThrownBy(() -> chatService.findMessages(room.id(), participant.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
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

	@Test
	@DisplayName("채팅방 참여자는 메시지를 작성하고 생성순으로 조회한다")
	void sendAndFindMessagesByRoomMember() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		ChatMessageResult first = chatService.sendMessage(
			room.id(),
			requester.getId(),
			new ChatMessageCreateCommand("안녕하세요")
		);
		ChatMessageResult second = chatService.sendMessage(
			room.id(),
			target.getId(),
			new ChatMessageCreateCommand("반갑습니다")
		);

		List<ChatMessageResult> messages = chatService.findMessages(room.id(), requester.getId());

		assertThat(messages).extracting(ChatMessageResult::id)
			.containsExactly(first.id(), second.id());
		assertThat(messages).extracting(ChatMessageResult::content)
			.containsExactly("안녕하세요", "반갑습니다");
	}

	@Test
	@DisplayName("모집이 마감된 스터디 채팅방에도 참여자는 메시지를 작성할 수 있다")
	void sendMessageToRecruitmentClosedStudyRoom() {
		Member owner = saveMember("owner");
		StudyResult study = studyService.create(
			owner.getId(),
			new StudyCreateCommand("알고리즘 스터디", "매주 알고리즘 문제를 풉니다.")
		);
		ChatRoomResult room = chatService.createStudyRoom(study.id(), owner.getId());

		studyService.close(study.id(), owner.getId());

		ChatMessageResult message = chatService.sendMessage(
			room.id(),
			owner.getId(),
			new ChatMessageCreateCommand("마감 후 메시지")
		);

		assertThat(message.content()).isEqualTo("마감 후 메시지");
	}

	@Test
	@DisplayName("내 채팅방 목록은 내가 참여한 방만 조회한다")
	void findMyRooms() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		Member outsider = saveMember("outsider");
		ChatRoomResult myRoom = chatService.createPrivateRoom(requester.getId(), target.getId());
		ChatRoomResult outsiderRoom = chatService.createPrivateRoom(target.getId(), outsider.getId());

		List<ChatRoomResult> rooms = chatService.findMyRooms(requester.getId());

		assertThat(rooms).extracting(ChatRoomResult::id)
			.containsExactly(myRoom.id());
		assertThat(rooms).extracting(ChatRoomResult::id)
			.doesNotContain(outsiderRoom.id());
	}

	@Test
	@DisplayName("채팅방을 삭제하면 내 채팅방 목록에서만 숨긴다")
	void hideRoomFromMyRooms() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		chatService.hideRoom(room.id(), requester.getId());

		assertThat(chatService.findMyRooms(requester.getId())).isEmpty();
		assertThat(chatService.findMyRooms(target.getId())).extracting(ChatRoomResult::id)
			.containsExactly(room.id());
	}

	@Test
	@DisplayName("삭제한 1:1 채팅방은 다시 만들면 내 목록에 복구된다")
	void restoreHiddenPrivateRoomWhenCreatingAgain() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());
		chatService.hideRoom(room.id(), requester.getId());

		ChatRoomResult restoredRoom = chatService.createPrivateRoom(requester.getId(), target.getId());

		assertThat(restoredRoom.id()).isEqualTo(room.id());
		assertThat(chatService.findMyRooms(requester.getId())).extracting(ChatRoomResult::id)
			.containsExactly(room.id());
	}

	@Test
	@DisplayName("상대방이 삭제한 기존 1:1 채팅방을 다시 요청하면 상대방 목록에도 복구하고 알림을 보낸다")
	void restoreTargetHiddenPrivateRoomWhenRequestingAgain() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());
		chatService.hideRoom(room.id(), target.getId());
		clearInvocations(outboxEventPublisher);

		ChatRoomResult restoredRoom = chatService.createPrivateRoom(requester.getId(), target.getId());

		assertThat(restoredRoom.id()).isEqualTo(room.id());
		assertThat(chatService.findMyRooms(requester.getId())).extracting(ChatRoomResult::id)
			.containsExactly(room.id());
		assertThat(chatService.findMyRooms(target.getId())).extracting(ChatRoomResult::id)
			.containsExactly(room.id());
		verify(outboxEventPublisher).publishPrivateChatRequested(room.id(), target.getId(), requester.getId());
	}

	@Test
	@DisplayName("채팅방 참여자가 아니면 채팅방을 삭제할 수 없다")
	void rejectHideRoomByNonRoomMember() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		Member outsider = saveMember("outsider");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		assertThatThrownBy(() -> chatService.hideRoom(room.id(), outsider.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
	}

	@Test
	@DisplayName("채팅방 참여자가 아니면 메시지 목록을 조회할 수 없다")
	void rejectFindMessagesByNonRoomMember() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		Member outsider = saveMember("outsider");
		ChatRoomResult room = chatService.createPrivateRoom(requester.getId(), target.getId());

		assertThatThrownBy(() -> chatService.findMessages(room.id(), outsider.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
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
}

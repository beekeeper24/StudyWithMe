package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatMessage;
import com.studywithme.chat.domain.ChatRoom;
import com.studywithme.chat.domain.ChatRoomMember;
import com.studywithme.chat.exception.ChatErrorCode;
import com.studywithme.chat.repository.ChatMessageRepository;
import com.studywithme.chat.repository.ChatRoomMemberRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMemberStatus;
import com.studywithme.study.domain.StudyStatus;
import com.studywithme.study.exception.StudyErrorCode;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberRepository memberRepository;
	private final StudyRepository studyRepository;
	private final StudyMemberRepository studyMemberRepository;

	public ChatService(
		ChatRoomRepository chatRoomRepository,
		ChatRoomMemberRepository chatRoomMemberRepository,
		ChatMessageRepository chatMessageRepository,
		MemberRepository memberRepository,
		StudyRepository studyRepository,
		StudyMemberRepository studyMemberRepository
	) {
		this.chatRoomRepository = chatRoomRepository;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.memberRepository = memberRepository;
		this.studyRepository = studyRepository;
		this.studyMemberRepository = studyMemberRepository;
	}

	@Transactional
	public ChatRoomResult createPrivateRoom(Long requesterMemberId, Long targetMemberId) {
		if (requesterMemberId.equals(targetMemberId)) {
			throw new BusinessException(ChatErrorCode.INVALID_PRIVATE_ROOM_TARGET);
		}
		validateActiveTargetMember(targetMemberId);

		String roomKey = privateRoomKey(requesterMemberId, targetMemberId);
		return chatRoomRepository.findByRoomKey(roomKey)
			.map(room -> {
				restoreRoomMember(room.getId(), requesterMemberId);
				return toRoomResult(room, requesterMemberId);
			})
			.orElseGet(() -> {
				ChatRoom room = chatRoomRepository.save(ChatRoom.privateRoom(roomKey));
				chatRoomMemberRepository.save(ChatRoomMember.join(room.getId(), requesterMemberId));
				chatRoomMemberRepository.save(ChatRoomMember.join(room.getId(), targetMemberId));
				return toRoomResult(room, requesterMemberId);
			});
	}

	@Transactional
	public ChatRoomResult createStudyRoom(Long studyId, Long requesterMemberId) {
		Study study = studyRepository.findById(studyId)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
		if (study.getStatus() == StudyStatus.DELETED) {
			throw new BusinessException(StudyErrorCode.STUDY_NOT_FOUND);
		}
		if (!studyMemberRepository.existsByStudyIdAndMemberIdAndStatus(
			studyId,
			requesterMemberId,
			StudyMemberStatus.JOINED
		)) {
			throw new BusinessException(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
		}

		String roomKey = studyRoomKey(studyId);
		return chatRoomRepository.findByRoomKey(roomKey)
			.map(room -> {
				syncStudyRoomMembers(room.getId(), studyId);
				restoreRoomMember(room.getId(), requesterMemberId);
				return toRoomResult(room, requesterMemberId);
			})
			.orElseGet(() -> {
				ChatRoom room = chatRoomRepository.save(ChatRoom.studyRoom(roomKey, studyId));
				syncStudyRoomMembers(room.getId(), studyId);
				return toRoomResult(room, requesterMemberId);
			});
	}

	public List<ChatRoomResult> findMyRooms(Long memberId) {
		return chatRoomRepository.findAllByMemberId(memberId)
			.stream()
			.filter(room -> canUseRoom(room, memberId))
			.map(room -> toRoomResult(room, memberId))
			.toList();
	}

	@Transactional
	public void hideRoom(Long roomId, Long requesterMemberId) {
		ChatRoom room = findRoom(roomId);
		ChatRoomMember roomMember = findRoomMember(room.getId(), requesterMemberId);
		roomMember.hide();
	}

	public List<ChatRoomMemberResult> findRoomMembers(Long roomId, Long requesterMemberId) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, requesterMemberId);

		List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByRoomId(room.getId());
		Map<Long, Member> members = memberRepository.findAllById(roomMembers.stream()
				.map(ChatRoomMember::getMemberId)
				.toList())
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));

		return roomMembers.stream()
			.filter(roomMember -> canUseRoom(room, roomMember.getMemberId()))
			.map(roomMember -> {
				Member member = members.get(roomMember.getMemberId());
				return new ChatRoomMemberResult(
					roomMember.getMemberId(),
					member == null ? null : member.getNickname(),
					member == null ? null : member.getProfileImageUrl(),
					roomMember.getJoinedAt()
				);
			})
			.toList();
	}

	@Transactional
	public ChatMessageResult sendMessage(Long roomId, Long senderMemberId, ChatMessageCreateCommand command) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, senderMemberId);
		validateRoomWritable(room);

		ChatMessage message = chatMessageRepository.save(ChatMessage.create(
			room.getId(),
			senderMemberId,
			command.content()
		));
		return ChatMessageResult.from(message);
	}

	public List<ChatMessageResult> findMessages(Long roomId, Long requesterMemberId) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, requesterMemberId);

		return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAscIdAsc(room.getId())
			.stream()
			.map(ChatMessageResult::from)
			.toList();
	}

	public void validateRoomMembership(Long roomId, Long memberId) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, memberId);
	}

	private void validateActiveTargetMember(Long targetMemberId) {
		memberRepository.findById(targetMemberId)
			.filter(member -> member.getStatus() == MemberStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ChatErrorCode.TARGET_MEMBER_NOT_FOUND));
	}

	private ChatRoom findRoom(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	private void validateRoomMember(ChatRoom room, Long memberId) {
		if (!chatRoomMemberRepository.existsByRoomIdAndMemberId(room.getId(), memberId)
			|| !canUseRoom(room, memberId)) {
			throw new BusinessException(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
		}
	}

	private boolean canUseRoom(ChatRoom room, Long memberId) {
		if (room.getStudyId() == null) {
			return true;
		}
		Study study = studyRepository.findById(room.getStudyId()).orElse(null);
		if (study == null || study.getStatus() == StudyStatus.DELETED) {
			return false;
		}
		return studyMemberRepository.existsByStudyIdAndMemberIdAndStatus(
			room.getStudyId(),
			memberId,
			StudyMemberStatus.JOINED
		);
	}

	private void validateRoomWritable(ChatRoom room) {
		if (room.getStudyId() == null) {
			return;
		}
		Study study = studyRepository.findById(room.getStudyId())
			.orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
		if (study.getStatus() == StudyStatus.ENDED || study.getStatus() == StudyStatus.DELETED) {
			throw new BusinessException(ChatErrorCode.STUDY_CHAT_ROOM_CLOSED);
		}
	}

	private ChatRoomMember findRoomMember(Long roomId, Long memberId) {
		return chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId)
			.orElseThrow(() -> new BusinessException(ChatErrorCode.NOT_CHAT_ROOM_MEMBER));
	}

	private void restoreRoomMember(Long roomId, Long memberId) {
		findRoomMember(roomId, memberId).restore();
	}

	private void syncStudyRoomMembers(Long roomId, Long studyId) {
		studyMemberRepository.findAllByStudyIdAndStatus(studyId, StudyMemberStatus.JOINED)
			.forEach(studyMember -> {
				if (!chatRoomMemberRepository.existsByRoomIdAndMemberId(roomId, studyMember.getMemberId())) {
					chatRoomMemberRepository.save(ChatRoomMember.join(roomId, studyMember.getMemberId()));
				}
			});
	}

	private ChatRoomResult toRoomResult(ChatRoom room, Long requesterMemberId) {
		return ChatRoomResult.from(room, roomTitle(room, requesterMemberId));
	}

	private String roomTitle(ChatRoom room, Long requesterMemberId) {
		if (room.getStudyId() != null) {
			return studyRepository.findById(room.getStudyId())
				.map(Study::getTitle)
				.orElse("스터디 채팅");
		}
		return chatRoomMemberRepository.findAllByRoomId(room.getId())
			.stream()
			.map(ChatRoomMember::getMemberId)
			.filter(memberId -> !memberId.equals(requesterMemberId))
			.findFirst()
			.flatMap(memberRepository::findById)
			.map(Member::getNickname)
			.orElse("1:1 채팅");
	}

	private String privateRoomKey(Long firstMemberId, Long secondMemberId) {
		long first = Math.min(firstMemberId, secondMemberId);
		long second = Math.max(firstMemberId, secondMemberId);
		return "PRIVATE:" + first + ":" + second;
	}

	private String studyRoomKey(Long studyId) {
		return "STUDY:" + studyId;
	}
}

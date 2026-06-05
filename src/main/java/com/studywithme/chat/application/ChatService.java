package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatMessage;
import com.studywithme.chat.domain.ChatMessageReport;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import com.studywithme.chat.domain.ChatRoom;
import com.studywithme.chat.domain.ChatRoomMember;
import com.studywithme.chat.exception.ChatErrorCode;
import com.studywithme.chat.repository.ChatMessageReportRepository;
import com.studywithme.chat.repository.ChatMessageRepository;
import com.studywithme.chat.repository.ChatRoomMemberRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMemberStatus;
import com.studywithme.study.domain.StudyStatus;
import com.studywithme.study.exception.StudyErrorCode;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatMessageReportRepository chatMessageReportRepository;
	private final MemberRepository memberRepository;
	private final StudyRepository studyRepository;
	private final StudyMemberRepository studyMemberRepository;
	private final OutboxEventPublisher outboxEventPublisher;

	public ChatService(
		ChatRoomRepository chatRoomRepository,
		ChatRoomMemberRepository chatRoomMemberRepository,
		ChatMessageRepository chatMessageRepository,
		ChatMessageReportRepository chatMessageReportRepository,
		MemberRepository memberRepository,
		StudyRepository studyRepository,
		StudyMemberRepository studyMemberRepository,
		OutboxEventPublisher outboxEventPublisher
	) {
		this.chatRoomRepository = chatRoomRepository;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.chatMessageReportRepository = chatMessageReportRepository;
		this.memberRepository = memberRepository;
		this.studyRepository = studyRepository;
		this.studyMemberRepository = studyMemberRepository;
		this.outboxEventPublisher = outboxEventPublisher;
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
				if (restoreRoomMemberIfHidden(room.getId(), targetMemberId)) {
					outboxEventPublisher.publishPrivateChatRequested(room.getId(), targetMemberId, requesterMemberId);
				}
				return toRoomResult(room, requesterMemberId);
			})
			.orElseGet(() -> {
				ChatRoom room = chatRoomRepository.save(ChatRoom.privateRoom(roomKey));
				chatRoomMemberRepository.save(ChatRoomMember.join(room.getId(), requesterMemberId));
				chatRoomMemberRepository.save(ChatRoomMember.join(room.getId(), targetMemberId));
				outboxEventPublisher.publishPrivateChatRequested(room.getId(), targetMemberId, requesterMemberId);
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
			.sorted(Comparator.comparing(ChatService::roomListSortTime)
				.thenComparing(ChatRoomResult::id)
				.reversed())
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

	@Transactional
	public List<ChatMessageResult> findMessages(Long roomId, Long requesterMemberId) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, requesterMemberId);
		ChatRoomMember roomMember = findRoomMember(room.getId(), requesterMemberId);

		List<ChatMessage> messages = chatMessageRepository.findAllByRoomIdOrderByCreatedAtAscIdAsc(room.getId());
		if (!messages.isEmpty()) {
			roomMember.markReadUpTo(messages.getLast().getId());
		}
		return messages.stream()
			.map(message -> ChatMessageResult.from(message, countReadMembers(room, message)))
			.toList();
	}

	@Transactional
	public ChatMessageResult deleteMessage(Long roomId, Long messageId, Long requesterMemberId) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, requesterMemberId);
		ChatMessage message = chatMessageRepository.findByIdAndRoomId(messageId, room.getId())
			.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
		if (!message.getSenderMemberId().equals(requesterMemberId)) {
			throw new BusinessException(ChatErrorCode.NOT_CHAT_MESSAGE_SENDER);
		}
		message.delete(requesterMemberId);
		return ChatMessageResult.from(message, countReadMembers(room, message));
	}

	@Transactional
	public ChatMessageReportResult reportMessage(Long roomId, Long messageId, Long reporterMemberId, String reason) {
		ChatRoom room = findRoom(roomId);
		validateRoomMember(room, reporterMemberId);
		ChatMessage message = findMessage(room.getId(), messageId);
		if (message.isDeleted()) {
			throw new BusinessException(ChatErrorCode.DELETED_CHAT_MESSAGE_REPORT_NOT_ALLOWED);
		}
		if (message.getSenderMemberId().equals(reporterMemberId)) {
			throw new BusinessException(ChatErrorCode.CANNOT_REPORT_OWN_MESSAGE);
		}
		if (chatMessageReportRepository.existsByMessageIdAndReporterMemberId(message.getId(), reporterMemberId)) {
			throw new BusinessException(ChatErrorCode.CHAT_MESSAGE_REPORT_DUPLICATED);
		}
		ChatMessageReport report = chatMessageReportRepository.save(ChatMessageReport.create(
			room.getId(),
			message.getId(),
			reporterMemberId,
			message.getSenderMemberId(),
			reason
		));
		outboxEventPublisher.publishChatMessageReported(report.getId(), reporterMemberId, findActiveAdminIds());
		return toReportResult(report, message);
	}

	public List<ChatMessageReportResult> findMessageReports(Long requesterMemberId, ChatMessageReportStatus status) {
		ensureAdmin(requesterMemberId);
		List<ChatMessageReport> reports = status == null
			? chatMessageReportRepository.findAllByOrderByCreatedAtDescIdDesc()
			: chatMessageReportRepository.findAllByStatusOrderByCreatedAtDescIdDesc(status);
		return toReportResults(reports);
	}

	@Transactional
	public ChatMessageReportResult assignMessageReport(Long reportId, Long requesterMemberId) {
		ensureAdmin(requesterMemberId);
		ChatMessageReport report = chatMessageReportRepository.findById(reportId)
			.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
		if (report.getStatus() != ChatMessageReportStatus.PENDING) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ALREADY_HANDLED);
		}
		if (report.getAssignedAdminMemberId() != null
			&& !report.getAssignedAdminMemberId().equals(requesterMemberId)) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ALREADY_ASSIGNED);
		}
		try {
			report.assignTo(requesterMemberId);
			chatMessageReportRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ALREADY_ASSIGNED);
		}
		return toReportResult(report, findMessage(report.getRoomId(), report.getMessageId()));
	}

	@Transactional
	public ChatMessageReportResult handleMessageReport(
		Long reportId,
		Long requesterMemberId,
		ChatMessageReportStatus nextStatus,
		String handlingNote
	) {
		ensureAdmin(requesterMemberId);
		if (nextStatus == ChatMessageReportStatus.PENDING) {
			throw new BusinessException(ChatErrorCode.INVALID_CHAT_REPORT_STATUS);
		}
		ChatMessageReport report = chatMessageReportRepository.findById(reportId)
			.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
		if (report.getStatus() != ChatMessageReportStatus.PENDING) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ALREADY_HANDLED);
		}
		if (!requesterMemberId.equals(report.getAssignedAdminMemberId())) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ASSIGNEE_REQUIRED);
		}
		try {
			report.handle(requesterMemberId, nextStatus, handlingNote);
			chatMessageReportRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ALREADY_HANDLED);
		}
		return toReportResult(report, findMessage(report.getRoomId(), report.getMessageId()));
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

	private ChatMessage findMessage(Long roomId, Long messageId) {
		return chatMessageRepository.findByIdAndRoomId(messageId, roomId)
			.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
	}

	private void ensureAdmin(Long requesterMemberId) {
		boolean isAdmin = memberRepository.findById(requesterMemberId)
			.map(member -> member.getRoles().contains(MemberRole.ADMIN))
			.orElse(false);
		if (!isAdmin) {
			throw new BusinessException(ChatErrorCode.CHAT_REPORT_ADMIN_REQUIRED);
		}
	}

	private List<Long> findActiveAdminIds() {
		return memberRepository.findAllByRoleAndStatus(MemberRole.ADMIN, MemberStatus.ACTIVE)
			.stream()
			.map(Member::getId)
			.toList();
	}

	private ChatMessageReportResult toReportResult(ChatMessageReport report, ChatMessage message) {
		return ChatMessageReportResult.from(report, message, findReportMembers(List.of(report)));
	}

	private List<ChatMessageReportResult> toReportResults(List<ChatMessageReport> reports) {
		Map<Long, Member> members = findReportMembers(reports);
		return reports.stream()
			.map(report -> ChatMessageReportResult.from(report, findMessage(report.getRoomId(), report.getMessageId()), members))
			.toList();
	}

	private Map<Long, Member> findReportMembers(List<ChatMessageReport> reports) {
		Set<Long> memberIds = new LinkedHashSet<>();
		for (ChatMessageReport report : reports) {
			memberIds.add(report.getReporterMemberId());
			memberIds.add(report.getReportedMemberId());
			if (report.getAssignedAdminMemberId() != null) {
				memberIds.add(report.getAssignedAdminMemberId());
			}
			if (report.getHandlerMemberId() != null) {
				memberIds.add(report.getHandlerMemberId());
			}
		}
		return memberRepository.findAllById(memberIds)
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
	}

	private void validateRoomMember(ChatRoom room, Long memberId) {
		ChatRoomMember roomMember = findRoomMember(room.getId(), memberId);
		if (roomMember.getHiddenAt() != null || !canUseRoom(room, memberId)) {
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

	private boolean restoreRoomMemberIfHidden(Long roomId, Long memberId) {
		ChatRoomMember roomMember = findRoomMember(roomId, memberId);
		boolean hidden = roomMember.getHiddenAt() != null;
		roomMember.restore();
		return hidden;
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
		ChatRoomMember roomMember = findRoomMember(room.getId(), requesterMemberId);
		ChatMessageResult lastMessage = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDescIdDesc(room.getId())
			.map(ChatMessageResult::from)
			.orElse(null);
		long unreadCount = countUnreadMessages(room.getId(), requesterMemberId, roomMember.getLastReadMessageId());
		return ChatRoomResult.from(room, roomTitle(room, requesterMemberId), lastMessage, unreadCount);
	}

	private long countUnreadMessages(Long roomId, Long requesterMemberId, Long lastReadMessageId) {
		if (lastReadMessageId == null) {
			return chatMessageRepository.countByRoomIdAndSenderMemberIdNot(roomId, requesterMemberId);
		}
		return chatMessageRepository.countByRoomIdAndSenderMemberIdNotAndIdGreaterThan(
			roomId,
			requesterMemberId,
			lastReadMessageId
		);
	}

	private long countReadMembers(ChatRoom room, ChatMessage message) {
		return chatRoomMemberRepository.findAllByRoomId(room.getId())
			.stream()
			.filter(roomMember -> !roomMember.getMemberId().equals(message.getSenderMemberId()))
			.filter(roomMember -> roomMember.getHiddenAt() == null)
			.filter(roomMember -> canUseRoom(room, roomMember.getMemberId()))
			.filter(roomMember -> roomMember.getLastReadMessageId() != null)
			.filter(roomMember -> roomMember.getLastReadMessageId() >= message.getId())
			.count();
	}

	private static LocalDateTime roomListSortTime(ChatRoomResult result) {
		return result.lastMessageCreatedAt() == null ? result.createdAt() : result.lastMessageCreatedAt();
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

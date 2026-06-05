package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatMessage;
import com.studywithme.chat.domain.ChatMessageReport;
import com.studywithme.chat.domain.ChatMessageReportModerationAction;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import com.studywithme.member.domain.Member;
import java.time.LocalDateTime;
import java.util.Map;

public record ChatMessageReportResult(
	Long id,
	Long roomId,
	Long messageId,
	Long reporterMemberId,
	String reporterNickname,
	Long reportedMemberId,
	String reportedNickname,
	String messageContent,
	String reason,
	ChatMessageReportStatus status,
	ChatMessageReportModerationAction moderationAction,
	Long assignedAdminMemberId,
	String assignedAdminNickname,
	LocalDateTime assignedAt,
	Long handlerMemberId,
	String handlerNickname,
	String handlingNote,
	LocalDateTime createdAt,
	LocalDateTime handledAt
) {

	public static ChatMessageReportResult from(
		ChatMessageReport report,
		ChatMessage message,
		Map<Long, Member> members
	) {
		return new ChatMessageReportResult(
			report.getId(),
			report.getRoomId(),
			report.getMessageId(),
			report.getReporterMemberId(),
			nicknameOf(members, report.getReporterMemberId()),
			report.getReportedMemberId(),
			nicknameOf(members, report.getReportedMemberId()),
			message.getContent(),
			report.getReason(),
			report.getStatus(),
			report.getModerationAction(),
			report.getAssignedAdminMemberId(),
			nicknameOf(members, report.getAssignedAdminMemberId()),
			report.getAssignedAt(),
			report.getHandlerMemberId(),
			nicknameOf(members, report.getHandlerMemberId()),
			report.getHandlingNote(),
			report.getCreatedAt(),
			report.getHandledAt()
		);
	}

	private static String nicknameOf(Map<Long, Member> members, Long memberId) {
		if (memberId == null) {
			return null;
		}
		Member member = members.get(memberId);
		return member == null ? null : member.getNickname();
	}
}

package com.studywithme.chat.presentation;

import com.studywithme.chat.application.ChatMessageReportResult;
import com.studywithme.chat.domain.ChatMessageReportModerationAction;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import java.time.LocalDateTime;

public record ChatMessageReportResponse(
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

	public static ChatMessageReportResponse from(ChatMessageReportResult result) {
		return from(result, true);
	}

	public static ChatMessageReportResponse fromReportCreation(ChatMessageReportResult result) {
		return from(result, false);
	}

	private static ChatMessageReportResponse from(ChatMessageReportResult result, boolean includeMemberContext) {
		return new ChatMessageReportResponse(
			result.id(),
			result.roomId(),
			result.messageId(),
			result.reporterMemberId(),
			includeMemberContext ? result.reporterNickname() : null,
			result.reportedMemberId(),
			includeMemberContext ? result.reportedNickname() : null,
			result.messageContent(),
			result.reason(),
			result.status(),
			result.moderationAction(),
			result.assignedAdminMemberId(),
			includeMemberContext ? result.assignedAdminNickname() : null,
			result.assignedAt(),
			result.handlerMemberId(),
			includeMemberContext ? result.handlerNickname() : null,
			result.handlingNote(),
			result.createdAt(),
			result.handledAt()
		);
	}
}

package com.studywithme.chat.presentation;

import com.studywithme.chat.application.ChatMessageReportResult;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import java.time.LocalDateTime;

public record ChatMessageReportResponse(
	Long id,
	Long roomId,
	Long messageId,
	Long reporterMemberId,
	Long reportedMemberId,
	String messageContent,
	String reason,
	ChatMessageReportStatus status,
	Long handlerMemberId,
	String handlingNote,
	LocalDateTime createdAt,
	LocalDateTime handledAt
) {

	public static ChatMessageReportResponse from(ChatMessageReportResult result) {
		return new ChatMessageReportResponse(
			result.id(),
			result.roomId(),
			result.messageId(),
			result.reporterMemberId(),
			result.reportedMemberId(),
			result.messageContent(),
			result.reason(),
			result.status(),
			result.handlerMemberId(),
			result.handlingNote(),
			result.createdAt(),
			result.handledAt()
		);
	}
}

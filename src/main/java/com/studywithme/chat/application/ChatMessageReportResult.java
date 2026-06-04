package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatMessage;
import com.studywithme.chat.domain.ChatMessageReport;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import java.time.LocalDateTime;

public record ChatMessageReportResult(
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

	public static ChatMessageReportResult from(ChatMessageReport report, ChatMessage message) {
		return new ChatMessageReportResult(
			report.getId(),
			report.getRoomId(),
			report.getMessageId(),
			report.getReporterMemberId(),
			report.getReportedMemberId(),
			message.getContent(),
			report.getReason(),
			report.getStatus(),
			report.getHandlerMemberId(),
			report.getHandlingNote(),
			report.getCreatedAt(),
			report.getHandledAt()
		);
	}
}

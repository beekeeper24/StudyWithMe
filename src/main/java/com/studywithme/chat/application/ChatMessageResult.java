package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResult(
	Long id,
	Long roomId,
	Long senderMemberId,
	String content,
	LocalDateTime createdAt,
	long readMemberCount
) {

	public static ChatMessageResult from(ChatMessage message) {
		return from(message, 0);
	}

	public static ChatMessageResult from(ChatMessage message, long readMemberCount) {
		return new ChatMessageResult(
			message.getId(),
			message.getRoomId(),
			message.getSenderMemberId(),
			message.getContent(),
			message.getCreatedAt(),
			readMemberCount
		);
	}
}

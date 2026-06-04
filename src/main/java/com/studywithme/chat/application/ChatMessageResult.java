package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResult(
	Long id,
	Long roomId,
	Long senderMemberId,
	String content,
	LocalDateTime createdAt,
	long readMemberCount,
	boolean deleted
) {

	private static final String DELETED_CONTENT = "삭제된 메시지입니다.";

	public static ChatMessageResult from(ChatMessage message) {
		return from(message, 0);
	}

	public static ChatMessageResult from(ChatMessage message, long readMemberCount) {
		return new ChatMessageResult(
			message.getId(),
			message.getRoomId(),
			message.getSenderMemberId(),
			message.isDeleted() ? DELETED_CONTENT : message.getContent(),
			message.getCreatedAt(),
			readMemberCount,
			message.isDeleted()
		);
	}
}

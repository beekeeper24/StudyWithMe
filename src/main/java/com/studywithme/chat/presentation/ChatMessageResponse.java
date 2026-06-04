package com.studywithme.chat.presentation;

import com.studywithme.chat.application.ChatMessageResult;
import java.time.LocalDateTime;

public record ChatMessageResponse(
	Long id,
	Long roomId,
	Long senderMemberId,
	String content,
	LocalDateTime createdAt,
	long readMemberCount,
	boolean deleted
) {

	public static ChatMessageResponse from(ChatMessageResult result) {
		return new ChatMessageResponse(
			result.id(),
			result.roomId(),
			result.senderMemberId(),
			result.content(),
			result.createdAt(),
			result.readMemberCount(),
			result.deleted()
		);
	}
}

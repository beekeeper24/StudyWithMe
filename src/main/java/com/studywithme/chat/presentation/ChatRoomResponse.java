package com.studywithme.chat.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studywithme.chat.application.ChatRoomResult;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRoomResponse(
	Long id,
	String type,
	Long studyId,
	String title,
	LocalDateTime createdAt,
	String lastMessageContent,
	Long lastMessageSenderMemberId,
	LocalDateTime lastMessageCreatedAt,
	long unreadCount
) {

	public static ChatRoomResponse from(ChatRoomResult result) {
		return new ChatRoomResponse(
			result.id(),
			result.type().name(),
			result.studyId(),
			result.title(),
			result.createdAt(),
			result.lastMessageContent(),
			result.lastMessageSenderMemberId(),
			result.lastMessageCreatedAt(),
			result.unreadCount()
		);
	}
}

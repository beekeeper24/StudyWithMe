package com.studywithme.chat.presentation;

import com.studywithme.chat.application.ChatRoomResult;
import java.time.LocalDateTime;

public record ChatRoomResponse(
	Long id,
	String type,
	Long studyId,
	LocalDateTime createdAt
) {

	public static ChatRoomResponse from(ChatRoomResult result) {
		return new ChatRoomResponse(
			result.id(),
			result.type().name(),
			result.studyId(),
			result.createdAt()
		);
	}
}

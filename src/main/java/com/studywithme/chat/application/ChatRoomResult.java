package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatRoom;
import com.studywithme.chat.domain.ChatRoomType;
import java.time.LocalDateTime;

public record ChatRoomResult(
	Long id,
	ChatRoomType type,
	Long studyId,
	String title,
	LocalDateTime createdAt
) {

	public static ChatRoomResult from(ChatRoom room) {
		return from(room, null);
	}

	public static ChatRoomResult from(ChatRoom room, String title) {
		return new ChatRoomResult(
			room.getId(),
			room.getType(),
			room.getStudyId(),
			title,
			room.getCreatedAt()
		);
	}
}

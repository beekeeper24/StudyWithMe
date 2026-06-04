package com.studywithme.chat.application;

import com.studywithme.chat.domain.ChatRoom;
import com.studywithme.chat.domain.ChatRoomType;
import java.time.LocalDateTime;

public record ChatRoomResult(
	Long id,
	ChatRoomType type,
	Long studyId,
	String title,
	LocalDateTime createdAt,
	String lastMessageContent,
	Long lastMessageSenderMemberId,
	LocalDateTime lastMessageCreatedAt,
	long unreadCount
) {

	public static ChatRoomResult from(ChatRoom room) {
		return from(room, null, null, 0);
	}

	public static ChatRoomResult from(
		ChatRoom room,
		String title,
		ChatMessageResult lastMessage,
		long unreadCount
	) {
		return new ChatRoomResult(
			room.getId(),
			room.getType(),
			room.getStudyId(),
			title,
			room.getCreatedAt(),
			lastMessage == null ? null : lastMessage.content(),
			lastMessage == null ? null : lastMessage.senderMemberId(),
			lastMessage == null ? null : lastMessage.createdAt(),
			unreadCount
		);
	}
}

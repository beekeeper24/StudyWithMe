package com.studywithme.chat.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studywithme.chat.application.ChatRoomMemberResult;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRoomMemberResponse(
	Long memberId,
	String nickname,
	String profileImageUrl,
	LocalDateTime joinedAt
) {

	public static ChatRoomMemberResponse from(ChatRoomMemberResult result) {
		return new ChatRoomMemberResponse(
			result.memberId(),
			result.nickname(),
			result.profileImageUrl(),
			result.joinedAt()
		);
	}
}

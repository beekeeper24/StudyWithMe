package com.studywithme.chat.application;

import java.time.LocalDateTime;

public record ChatRoomMemberResult(
	Long memberId,
	String nickname,
	String profileImageUrl,
	LocalDateTime joinedAt
) {
}

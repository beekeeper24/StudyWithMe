package com.studywithme.chat.presentation;

import jakarta.validation.constraints.NotNull;

public record PrivateChatRoomCreateRequest(
	@NotNull
	Long targetMemberId
) {
}

package com.studywithme.chat.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageCreateRequest(
	@NotBlank
	@Size(max = 1000)
	String content
) {
}

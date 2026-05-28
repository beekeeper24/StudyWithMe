package com.studywithme.post.presentation;

import com.studywithme.post.domain.PostBoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
	PostBoardType boardType,

	@NotBlank
	@Size(max = 100)
	String title,

	@NotBlank
	@Size(max = 5000)
	String content
) {
}

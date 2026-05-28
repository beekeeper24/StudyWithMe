package com.studywithme.post.application;

import com.studywithme.post.domain.PostBoardType;

public record PostCreateCommand(
	PostBoardType boardType,
	String title,
	String content
) {
	public PostCreateCommand(String title, String content) {
		this(PostBoardType.FREE, title, content);
	}
}

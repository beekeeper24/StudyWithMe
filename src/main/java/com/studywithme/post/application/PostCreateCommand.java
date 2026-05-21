package com.studywithme.post.application;

public record PostCreateCommand(
	String title,
	String content
) {
}

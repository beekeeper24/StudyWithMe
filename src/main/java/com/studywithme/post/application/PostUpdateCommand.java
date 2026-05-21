package com.studywithme.post.application;

public record PostUpdateCommand(
	String title,
	String content
) {
}

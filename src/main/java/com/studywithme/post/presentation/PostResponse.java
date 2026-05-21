package com.studywithme.post.presentation;

import com.studywithme.post.application.PostResult;
import java.time.LocalDateTime;

public record PostResponse(
	Long id,
	Long authorMemberId,
	String title,
	String content,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static PostResponse from(PostResult result) {
		return new PostResponse(
			result.id(),
			result.authorMemberId(),
			result.title(),
			result.content(),
			result.status().name(),
			result.createdAt(),
			result.updatedAt()
		);
	}
}

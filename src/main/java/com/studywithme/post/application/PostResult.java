package com.studywithme.post.application;

import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostStatus;
import java.time.LocalDateTime;

public record PostResult(
	Long id,
	Long authorMemberId,
	String title,
	String content,
	PostStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static PostResult from(Post post) {
		return new PostResult(
			post.getId(),
			post.getAuthorMemberId(),
			post.getTitle(),
			post.getContent(),
			post.getStatus(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}

package com.studywithme.comment.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studywithme.comment.application.CommentResult;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentResponse(
	Long id,
	Long postId,
	Long authorMemberId,
	Long parentCommentId,
	String content,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static CommentResponse from(CommentResult result) {
		return new CommentResponse(
			result.id(),
			result.postId(),
			result.authorMemberId(),
			result.parentCommentId(),
			result.content(),
			result.status().name(),
			result.createdAt(),
			result.updatedAt()
		);
	}
}

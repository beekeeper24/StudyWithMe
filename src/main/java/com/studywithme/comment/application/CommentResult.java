package com.studywithme.comment.application;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import java.time.LocalDateTime;

public record CommentResult(
	Long id,
	Long postId,
	Long authorMemberId,
	Long parentCommentId,
	String content,
	CommentStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static CommentResult from(Comment comment) {
		return new CommentResult(
			comment.getId(),
			comment.getPostId(),
			comment.getAuthorMemberId(),
			comment.getParentCommentId(),
			comment.getContent(),
			comment.getStatus(),
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}
}

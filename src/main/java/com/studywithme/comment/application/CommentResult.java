package com.studywithme.comment.application;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.member.domain.Member;
import java.time.LocalDateTime;

public record CommentResult(
	Long id,
	Long postId,
	Long authorMemberId,
	String authorNickname,
	String authorProfileImageUrl,
	boolean ownedByRequester,
	Long parentCommentId,
	String content,
	CommentStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static CommentResult from(Comment comment) {
		return from(comment, null, null);
	}

	public static CommentResult from(Comment comment, Member author, Long requesterMemberId) {
		return new CommentResult(
			comment.getId(),
			comment.getPostId(),
			comment.getAuthorMemberId(),
			author == null ? null : author.getNickname(),
			author == null ? null : author.getProfileImageUrl(),
			requesterMemberId != null && comment.getAuthorMemberId().equals(requesterMemberId),
			comment.getParentCommentId(),
			comment.getContent(),
			comment.getStatus(),
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}
}

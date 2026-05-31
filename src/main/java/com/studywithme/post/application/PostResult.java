package com.studywithme.post.application;

import com.studywithme.member.domain.Member;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostBoardType;
import com.studywithme.post.domain.PostStatus;
import java.time.LocalDateTime;

public record PostResult(
	Long id,
	Long authorMemberId,
	PostBoardType boardType,
	long commentCount,
	String authorNickname,
	String authorProfileImageUrl,
	boolean ownedByRequester,
	String title,
	String content,
	PostStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static PostResult from(Post post) {
		return from(post, null, null, 0);
	}

	public static PostResult from(Post post, Member author, Long requesterMemberId) {
		return from(post, author, requesterMemberId, 0);
	}

	public static PostResult from(Post post, Member author, Long requesterMemberId, long commentCount) {
		return new PostResult(
			post.getId(),
			post.getAuthorMemberId(),
			post.getBoardType(),
			commentCount,
			author == null ? null : author.getNickname(),
			author == null ? null : author.getProfileImageUrl(),
			requesterMemberId != null && post.getAuthorMemberId().equals(requesterMemberId),
			post.getTitle(),
			post.getContent(),
			post.getStatus(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}

package com.studywithme.comment.domain;

import com.studywithme.comment.exception.CommentErrorCode;
import com.studywithme.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "author_member_id", nullable = false)
	private Long authorMemberId;

	@Column(name = "parent_comment_id")
	private Long parentCommentId;

	@Column(nullable = false, length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommentStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Comment() {
	}

	private Comment(Long postId, Long authorMemberId, Long parentCommentId, String content) {
		this.postId = postId;
		this.authorMemberId = authorMemberId;
		this.parentCommentId = parentCommentId;
		this.content = content;
		this.status = CommentStatus.PUBLISHED;
	}

	public static Comment create(Long postId, Long authorMemberId, Long parentCommentId, String content) {
		return new Comment(postId, authorMemberId, parentCommentId, content);
	}

	public boolean isReply() {
		return parentCommentId != null;
	}

	public void update(Long requesterMemberId, String content) {
		requireAuthor(requesterMemberId);
		this.content = content;
	}

	public void delete(Long requesterMemberId) {
		requireAuthor(requesterMemberId);
		delete();
	}

	public void delete() {
		this.status = CommentStatus.DELETED;
	}

	private void requireAuthor(Long requesterMemberId) {
		if (!authorMemberId.equals(requesterMemberId)) {
			throw new BusinessException(CommentErrorCode.NOT_COMMENT_AUTHOR);
		}
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getAuthorMemberId() {
		return authorMemberId;
	}

	public Long getParentCommentId() {
		return parentCommentId;
	}

	public String getContent() {
		return content;
	}

	public CommentStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}

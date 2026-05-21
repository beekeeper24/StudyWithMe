package com.studywithme.post.domain;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.post.exception.PostErrorCode;
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
@Table(name = "posts")
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "author_member_id", nullable = false)
	private Long authorMemberId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 5000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PostStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Post() {
	}

	private Post(String title, String content, Long authorMemberId) {
		this.title = title;
		this.content = content;
		this.authorMemberId = authorMemberId;
		this.status = PostStatus.PUBLISHED;
	}

	public static Post create(String title, String content, Long authorMemberId) {
		return new Post(title, content, authorMemberId);
	}

	public void update(Long requesterMemberId, String title, String content) {
		requireAuthor(requesterMemberId);
		this.title = title;
		this.content = content;
	}

	public void delete(Long requesterMemberId) {
		requireAuthor(requesterMemberId);
		this.status = PostStatus.DELETED;
	}

	private void requireAuthor(Long requesterMemberId) {
		if (!authorMemberId.equals(requesterMemberId)) {
			throw new BusinessException(PostErrorCode.NOT_POST_AUTHOR);
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

	public Long getAuthorMemberId() {
		return authorMemberId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public PostStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}

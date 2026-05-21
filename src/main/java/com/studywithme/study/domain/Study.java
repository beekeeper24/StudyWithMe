package com.studywithme.study.domain;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.study.exception.StudyErrorCode;
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
@Table(name = "studies")
public class Study {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_member_id", nullable = false)
	private Long ownerMemberId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StudyStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Study() {
	}

	private Study(String title, String description, Long ownerMemberId) {
		this.title = title;
		this.description = description;
		this.ownerMemberId = ownerMemberId;
		this.status = StudyStatus.RECRUITING;
	}

	public static Study create(String title, String description, Long ownerMemberId) {
		return new Study(title, description, ownerMemberId);
	}

	public void close(Long requesterMemberId) {
		if (!ownerMemberId.equals(requesterMemberId)) {
			throw new BusinessException(StudyErrorCode.NOT_STUDY_OWNER);
		}
		this.status = StudyStatus.CLOSED;
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

	public Long getOwnerMemberId() {
		return ownerMemberId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public StudyStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}

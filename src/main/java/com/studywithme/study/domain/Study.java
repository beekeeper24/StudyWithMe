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

	@Column(name = "progress_method", length = 500)
	private String progressMethod;

	@Column(name = "target_audience", length = 500)
	private String targetAudience;

	@Column(length = 1000)
	private String rules;

	@Column
	private Integer capacity;

	@Column(length = 200)
	private String schedule;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StudyStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Study() {
	}

	private Study(
		String title,
		String description,
		String progressMethod,
		String targetAudience,
		String rules,
		Integer capacity,
		String schedule,
		Long ownerMemberId
	) {
		this.title = title;
		this.description = description;
		this.progressMethod = progressMethod;
		this.targetAudience = targetAudience;
		this.rules = rules;
		this.capacity = capacity;
		this.schedule = schedule;
		this.ownerMemberId = ownerMemberId;
		this.status = StudyStatus.RECRUITING;
	}

	public static Study create(String title, String description, Long ownerMemberId) {
		return new Study(title, description, null, null, null, null, null, ownerMemberId);
	}

	public static Study create(
		String title,
		String description,
		String progressMethod,
		String targetAudience,
		String rules,
		Integer capacity,
		String schedule,
		Long ownerMemberId
	) {
		return new Study(
			title,
			description,
			progressMethod,
			targetAudience,
			rules,
			capacity,
			schedule,
			ownerMemberId
		);
	}

	public void close(Long requesterMemberId) {
		if (!ownerMemberId.equals(requesterMemberId)) {
			throw new BusinessException(StudyErrorCode.NOT_STUDY_OWNER);
		}
		this.status = StudyStatus.CLOSED;
	}

	public void update(
		Long requesterMemberId,
		String title,
		String description,
		String progressMethod,
		String targetAudience,
		String rules,
		Integer capacity,
		String schedule
	) {
		if (!ownerMemberId.equals(requesterMemberId)) {
			throw new BusinessException(StudyErrorCode.NOT_STUDY_OWNER);
		}
		this.title = title;
		this.description = description;
		this.progressMethod = progressMethod;
		this.targetAudience = targetAudience;
		this.rules = rules;
		this.capacity = capacity;
		this.schedule = schedule;
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

	public String getProgressMethod() {
		return progressMethod;
	}

	public String getTargetAudience() {
		return targetAudience;
	}

	public String getRules() {
		return rules;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public String getSchedule() {
		return schedule;
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

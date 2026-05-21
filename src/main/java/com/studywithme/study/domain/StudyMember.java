package com.studywithme.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "study_members",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_study_members_study_member",
		columnNames = {"study_id", "member_id"}
	)
)
public class StudyMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "study_id", nullable = false)
	private Long studyId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StudyMemberRole role;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	protected StudyMember() {
	}

	private StudyMember(Long studyId, Long memberId, StudyMemberRole role) {
		this.studyId = studyId;
		this.memberId = memberId;
		this.role = role;
	}

	public static StudyMember owner(Long studyId, Long memberId) {
		return new StudyMember(studyId, memberId, StudyMemberRole.OWNER);
	}

	public static StudyMember member(Long studyId, Long memberId) {
		return new StudyMember(studyId, memberId, StudyMemberRole.MEMBER);
	}

	@PrePersist
	void prePersist() {
		this.joinedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getStudyId() {
		return studyId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public StudyMemberRole getRole() {
		return role;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}
}

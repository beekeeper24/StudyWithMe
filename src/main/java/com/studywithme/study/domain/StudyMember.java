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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StudyMemberStatus status;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	@Column(name = "left_at")
	private LocalDateTime leftAt;

	protected StudyMember() {
	}

	private StudyMember(Long studyId, Long memberId, StudyMemberRole role) {
		this.studyId = studyId;
		this.memberId = memberId;
		this.role = role;
		this.status = StudyMemberStatus.JOINED;
	}

	private StudyMember(Long studyId, Long memberId, StudyMemberRole role, StudyMemberStatus status) {
		this.studyId = studyId;
		this.memberId = memberId;
		this.role = role;
		this.status = status;
	}

	public static StudyMember owner(Long studyId, Long memberId) {
		return new StudyMember(studyId, memberId, StudyMemberRole.OWNER);
	}

	public static StudyMember member(Long studyId, Long memberId) {
		return new StudyMember(studyId, memberId, StudyMemberRole.MEMBER);
	}

	public static StudyMember request(Long studyId, Long memberId) {
		return new StudyMember(studyId, memberId, StudyMemberRole.MEMBER, StudyMemberStatus.PENDING);
	}

	@PrePersist
	void prePersist() {
		this.joinedAt = LocalDateTime.now();
	}

	public void leave() {
		this.status = StudyMemberStatus.LEFT;
		this.leftAt = LocalDateTime.now();
	}

	public void cancelRequest() {
		this.status = StudyMemberStatus.LEFT;
		this.leftAt = LocalDateTime.now();
	}

	public void reject() {
		this.status = StudyMemberStatus.LEFT;
		this.leftAt = LocalDateTime.now();
	}

	public void rejoin() {
		this.status = StudyMemberStatus.JOINED;
		this.leftAt = null;
	}

	public void requestAgain() {
		this.status = StudyMemberStatus.PENDING;
		this.leftAt = null;
	}

	public void approve() {
		this.status = StudyMemberStatus.JOINED;
		this.leftAt = null;
	}

	public boolean isJoined() {
		return status == StudyMemberStatus.JOINED;
	}

	public boolean isPending() {
		return status == StudyMemberStatus.PENDING;
	}

	public boolean isLeft() {
		return status == StudyMemberStatus.LEFT;
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

	public StudyMemberStatus getStatus() {
		return status;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public LocalDateTime getLeftAt() {
		return leftAt;
	}
}

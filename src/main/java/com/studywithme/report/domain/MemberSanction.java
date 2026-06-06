package com.studywithme.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_sanctions")
public class MemberSanction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "target_member_id", nullable = false)
	private Long targetMemberId;

	@Column(name = "admin_member_id", nullable = false)
	private Long adminMemberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MemberSanctionType type;

	@Column(nullable = false, length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private MemberSanctionSourceType sourceType;

	@Column(name = "source_id")
	private Long sourceId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected MemberSanction() {
	}

	private MemberSanction(
		Long targetMemberId,
		Long adminMemberId,
		MemberSanctionType type,
		String reason,
		MemberSanctionSourceType sourceType,
		Long sourceId
	) {
		this.targetMemberId = targetMemberId;
		this.adminMemberId = adminMemberId;
		this.type = type;
		this.reason = reason;
		this.sourceType = sourceType;
		this.sourceId = sourceId;
	}

	public static MemberSanction create(
		Long targetMemberId,
		Long adminMemberId,
		MemberSanctionType type,
		String reason,
		MemberSanctionSourceType sourceType,
		Long sourceId
	) {
		return new MemberSanction(targetMemberId, adminMemberId, type, reason, sourceType, sourceId);
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getTargetMemberId() {
		return targetMemberId;
	}

	public Long getAdminMemberId() {
		return adminMemberId;
	}

	public MemberSanctionType getType() {
		return type;
	}

	public String getReason() {
		return reason;
	}

	public MemberSanctionSourceType getSourceType() {
		return sourceType;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

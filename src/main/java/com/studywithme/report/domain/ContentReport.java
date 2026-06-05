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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "content_reports",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_content_reports_target_reporter",
		columnNames = {"target_type", "target_id", "reporter_member_id"}
	)
)
public class ContentReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 20)
	private ContentReportTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "target_title", length = 100)
	private String targetTitle;

	@Column(name = "target_content", nullable = false, length = 5000)
	private String targetContent;

	@Column(name = "reporter_member_id", nullable = false)
	private Long reporterMemberId;

	@Column(name = "reported_member_id", nullable = false)
	private Long reportedMemberId;

	@Column(nullable = false, length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContentReportStatus status = ContentReportStatus.PENDING;

	@Column(name = "assigned_admin_member_id")
	private Long assignedAdminMemberId;

	@Column(name = "assigned_at")
	private LocalDateTime assignedAt;

	@Column(name = "handler_member_id")
	private Long handlerMemberId;

	@Column(name = "handling_note", length = 500)
	private String handlingNote;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "handled_at")
	private LocalDateTime handledAt;

	@Version
	@Column(nullable = false)
	private Long version = 0L;

	protected ContentReport() {
	}

	private ContentReport(
		ContentReportTargetType targetType,
		Long targetId,
		Long postId,
		String targetTitle,
		String targetContent,
		Long reporterMemberId,
		Long reportedMemberId,
		String reason
	) {
		this.targetType = targetType;
		this.targetId = targetId;
		this.postId = postId;
		this.targetTitle = targetTitle;
		this.targetContent = targetContent;
		this.reporterMemberId = reporterMemberId;
		this.reportedMemberId = reportedMemberId;
		this.reason = reason;
	}

	public static ContentReport create(
		ContentReportTargetType targetType,
		Long targetId,
		Long postId,
		String targetTitle,
		String targetContent,
		Long reporterMemberId,
		Long reportedMemberId,
		String reason
	) {
		return new ContentReport(
			targetType,
			targetId,
			postId,
			targetTitle,
			targetContent,
			reporterMemberId,
			reportedMemberId,
			reason
		);
	}

	public void assignTo(Long adminMemberId) {
		if (assignedAdminMemberId != null && assignedAdminMemberId.equals(adminMemberId)) {
			return;
		}
		this.assignedAdminMemberId = adminMemberId;
		this.assignedAt = LocalDateTime.now();
	}

	public void handle(Long handlerMemberId, ContentReportStatus nextStatus, String handlingNote) {
		if (nextStatus == ContentReportStatus.PENDING) {
			throw new IllegalArgumentException("Pending is not a handled report status.");
		}
		this.status = nextStatus;
		this.handlerMemberId = handlerMemberId;
		this.handlingNote = handlingNote;
		this.handledAt = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public ContentReportTargetType getTargetType() {
		return targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public Long getPostId() {
		return postId;
	}

	public String getTargetTitle() {
		return targetTitle;
	}

	public String getTargetContent() {
		return targetContent;
	}

	public Long getReporterMemberId() {
		return reporterMemberId;
	}

	public Long getReportedMemberId() {
		return reportedMemberId;
	}

	public String getReason() {
		return reason;
	}

	public ContentReportStatus getStatus() {
		return status;
	}

	public Long getAssignedAdminMemberId() {
		return assignedAdminMemberId;
	}

	public LocalDateTime getAssignedAt() {
		return assignedAt;
	}

	public Long getHandlerMemberId() {
		return handlerMemberId;
	}

	public String getHandlingNote() {
		return handlingNote;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getHandledAt() {
		return handledAt;
	}

	public Long getVersion() {
		return version;
	}
}

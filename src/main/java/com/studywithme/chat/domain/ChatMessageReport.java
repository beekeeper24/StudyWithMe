package com.studywithme.chat.domain;

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
	name = "chat_message_reports",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_chat_message_reports_message_reporter",
		columnNames = {"message_id", "reporter_member_id"}
	)
)
public class ChatMessageReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "message_id", nullable = false)
	private Long messageId;

	@Column(name = "reporter_member_id", nullable = false)
	private Long reporterMemberId;

	@Column(name = "reported_member_id", nullable = false)
	private Long reportedMemberId;

	@Column(nullable = false, length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChatMessageReportStatus status = ChatMessageReportStatus.PENDING;

	@Column(name = "handler_member_id")
	private Long handlerMemberId;

	@Column(name = "handling_note", length = 500)
	private String handlingNote;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "handled_at")
	private LocalDateTime handledAt;

	protected ChatMessageReport() {
	}

	private ChatMessageReport(Long roomId, Long messageId, Long reporterMemberId, Long reportedMemberId, String reason) {
		this.roomId = roomId;
		this.messageId = messageId;
		this.reporterMemberId = reporterMemberId;
		this.reportedMemberId = reportedMemberId;
		this.reason = reason;
	}

	public static ChatMessageReport create(
		Long roomId,
		Long messageId,
		Long reporterMemberId,
		Long reportedMemberId,
		String reason
	) {
		return new ChatMessageReport(roomId, messageId, reporterMemberId, reportedMemberId, reason);
	}

	public void handle(Long handlerMemberId, ChatMessageReportStatus nextStatus, String handlingNote) {
		if (nextStatus == ChatMessageReportStatus.PENDING) {
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

	public Long getRoomId() {
		return roomId;
	}

	public Long getMessageId() {
		return messageId;
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

	public ChatMessageReportStatus getStatus() {
		return status;
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
}

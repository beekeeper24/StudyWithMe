package com.studywithme.notification.domain;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.notification.exception.NotificationErrorCode;
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
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "receiver_member_id", nullable = false)
	private Long receiverMemberId;

	@Column(name = "actor_member_id", nullable = false)
	private Long actorMemberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 50)
	private NotificationTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "source_event_id", nullable = false, length = 36)
	private String sourceEventId;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Notification() {
	}

	private Notification(
		Long receiverMemberId,
		Long actorMemberId,
		NotificationType type,
		NotificationTargetType targetType,
		Long targetId,
		String sourceEventId,
		String message
	) {
		this.receiverMemberId = receiverMemberId;
		this.actorMemberId = actorMemberId;
		this.type = type;
		this.targetType = targetType;
		this.targetId = targetId;
		this.sourceEventId = sourceEventId;
		this.message = message;
	}

	public static Notification create(
		Long receiverMemberId,
		Long actorMemberId,
		NotificationType type,
		NotificationTargetType targetType,
		Long targetId,
		String sourceEventId,
		String message
	) {
		return new Notification(receiverMemberId, actorMemberId, type, targetType, targetId, sourceEventId, message);
	}

	public void markRead(Long requesterMemberId) {
		if (!receiverMemberId.equals(requesterMemberId)) {
			throw new BusinessException(NotificationErrorCode.NOT_NOTIFICATION_OWNER);
		}
		if (readAt == null) {
			readAt = LocalDateTime.now();
		}
	}

	public boolean isRead() {
		return readAt != null;
	}

	@PrePersist
	void prePersist() {
		createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getReceiverMemberId() {
		return receiverMemberId;
	}

	public Long getActorMemberId() {
		return actorMemberId;
	}

	public NotificationType getType() {
		return type;
	}

	public NotificationTargetType getTargetType() {
		return targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public String getSourceEventId() {
		return sourceEventId;
	}

	public String getMessage() {
		return message;
	}

	public LocalDateTime getReadAt() {
		return readAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

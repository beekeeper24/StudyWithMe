package com.studywithme.notification.application;

import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.domain.NotificationType;
import java.time.LocalDateTime;

public record NotificationResult(
	Long id,
	Long receiverMemberId,
	Long actorMemberId,
	NotificationType type,
	NotificationTargetType targetType,
	Long targetId,
	Long targetPostId,
	String message,
	boolean read,
	LocalDateTime createdAt,
	LocalDateTime readAt
) {

	public static NotificationResult from(Notification notification) {
		return new NotificationResult(
			notification.getId(),
			notification.getReceiverMemberId(),
			notification.getActorMemberId(),
			notification.getType(),
			notification.getTargetType(),
			notification.getTargetId(),
			null,
			notification.getMessage(),
			notification.isRead(),
			notification.getCreatedAt(),
			notification.getReadAt()
		);
	}

	public static NotificationResult from(Notification notification, Long targetPostId) {
		return new NotificationResult(
			notification.getId(),
			notification.getReceiverMemberId(),
			notification.getActorMemberId(),
			notification.getType(),
			notification.getTargetType(),
			notification.getTargetId(),
			targetPostId,
			notification.getMessage(),
			notification.isRead(),
			notification.getCreatedAt(),
			notification.getReadAt()
		);
	}
}

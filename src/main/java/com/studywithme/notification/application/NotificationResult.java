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
			notification.getMessage(),
			notification.isRead(),
			notification.getCreatedAt(),
			notification.getReadAt()
		);
	}
}

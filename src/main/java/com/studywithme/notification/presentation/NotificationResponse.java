package com.studywithme.notification.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studywithme.notification.application.NotificationResult;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
	Long id,
	Long receiverMemberId,
	Long actorMemberId,
	String type,
	String targetType,
	Long targetId,
	Long targetPostId,
	String message,
	boolean read,
	LocalDateTime createdAt,
	LocalDateTime readAt
) {

	public static NotificationResponse from(NotificationResult result) {
		return new NotificationResponse(
			result.id(),
			result.receiverMemberId(),
			result.actorMemberId(),
			result.type().name(),
			result.targetType().name(),
			result.targetId(),
			result.targetPostId(),
			result.message(),
			result.read(),
			result.createdAt(),
			result.readAt()
		);
	}
}

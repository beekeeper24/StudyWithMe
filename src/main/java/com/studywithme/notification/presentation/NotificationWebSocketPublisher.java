package com.studywithme.notification.presentation;

import com.studywithme.notification.application.NotificationRealtimePublisher;
import com.studywithme.notification.application.NotificationResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationWebSocketPublisher implements NotificationRealtimePublisher {

	private static final String NOTIFICATION_QUEUE = "/queue/notifications";

	private final SimpMessagingTemplate messagingTemplate;

	public NotificationWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void publish(NotificationResult notification) {
		messagingTemplate.convertAndSendToUser(
			notification.receiverMemberId().toString(),
			NOTIFICATION_QUEUE,
			NotificationResponse.from(notification)
		);
	}
}

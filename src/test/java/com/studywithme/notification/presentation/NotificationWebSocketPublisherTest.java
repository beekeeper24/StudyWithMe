package com.studywithme.notification.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.studywithme.notification.application.NotificationResult;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.domain.NotificationType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class NotificationWebSocketPublisherTest {

	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final NotificationWebSocketPublisher publisher = new NotificationWebSocketPublisher(messagingTemplate);

	@Test
	@DisplayName("알림 수신자의 user queue로 NotificationResponse를 전달한다")
	void publishToReceiverUserQueue() {
		NotificationResult notification = new NotificationResult(
			10L,
			1L,
			2L,
			NotificationType.COMMENT_ON_POST,
			NotificationTargetType.COMMENT,
			100L,
			1L,
			"새 댓글이 달렸습니다.",
			false,
			LocalDateTime.now(),
			null
		);

		publisher.publish(notification);

		verify(messagingTemplate).convertAndSendToUser(
			"1",
			"/queue/notifications",
			NotificationResponse.from(notification)
		);
	}
}

package com.studywithme.notification.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

	@Mock
	private NotificationOutboxProcessor notificationOutboxProcessor;

	private NotificationKafkaConsumer notificationKafkaConsumer;

	@BeforeEach
	void setUp() {
		notificationKafkaConsumer = new NotificationKafkaConsumer(
			new ObjectMapper(),
			notificationOutboxProcessor
		);
	}

	@Test
	@DisplayName("Kafka outbox envelope를 읽어 notification processor에 위임한다")
	void consumeDelegatesKafkaEnvelopeToProcessor() {
		String message = """
			{
			  "eventId": "event-1",
			  "eventType": "COMMENT_MENTIONED",
			  "aggregateType": "COMMENT",
			  "aggregateId": 10,
			  "occurredAt": "2026-05-22T20:00:00",
			  "payload": {
			    "postId": 1,
			    "commentId": 10,
			    "actorMemberId": 2,
			    "mentionedMemberIds": [3],
			    "replacedNotificationReceiverMemberIds": []
			  }
			}
			""";

		notificationKafkaConsumer.consume(message);

		then(notificationOutboxProcessor).should()
			.processKafkaEvent(
				"event-1",
				"COMMENT_MENTIONED",
				10L,
				"{\"postId\":1,\"commentId\":10,\"actorMemberId\":2,\"mentionedMemberIds\":[3],\"replacedNotificationReceiverMemberIds\":[]}"
			);
	}

	@Test
	@DisplayName("잘못된 Kafka message는 예외를 던져 Kafka retry 대상으로 남긴다")
	void consumeRejectsMalformedMessage() {
		assertThatThrownBy(() -> notificationKafkaConsumer.consume("{"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid outbox Kafka message");
	}
}

package com.studywithme.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.outbox.application.OutboxKafkaEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notification.kafka", name = "consumer-enabled", havingValue = "true")
public class NotificationKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOutboxProcessor notificationOutboxProcessor;

	public NotificationKafkaConsumer(
		ObjectMapper objectMapper,
		NotificationOutboxProcessor notificationOutboxProcessor
	) {
		this.objectMapper = objectMapper;
		this.notificationOutboxProcessor = notificationOutboxProcessor;
	}

	@KafkaListener(
		topics = "${app.outbox.kafka.topic:studywithme.outbox.events}",
		groupId = "${app.notification.kafka.group-id:studywithme-notification}"
	)
	public void consume(String message) {
		OutboxKafkaEvent event;
		try {
			event = objectMapper.readValue(message, OutboxKafkaEvent.class);
		} catch (Exception exception) {
			throw new IllegalArgumentException("Invalid outbox Kafka message", exception);
		}

		try {
			notificationOutboxProcessor.processKafkaEvent(
				event.eventId(),
				event.eventType(),
				event.aggregateId(),
				objectMapper.writeValueAsString(event.payload())
			);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to consume outbox Kafka message", exception);
		}
	}
}

package com.studywithme.outbox.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.outbox.kafka", name = "relay-enabled", havingValue = "true")
public class OutboxKafkaRelayWorker {

	private final OutboxKafkaRelay outboxKafkaRelay;
	private final OutboxKafkaProperties properties;

	public OutboxKafkaRelayWorker(OutboxKafkaRelay outboxKafkaRelay, OutboxKafkaProperties properties) {
		this.outboxKafkaRelay = outboxKafkaRelay;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${app.outbox.kafka.poll-interval-ms:5000}")
	public void publishPending() {
		outboxKafkaRelay.publishPending(properties.batchSize());
	}
}

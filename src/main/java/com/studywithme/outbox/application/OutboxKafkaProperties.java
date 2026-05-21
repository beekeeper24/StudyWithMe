package com.studywithme.outbox.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.kafka")
public record OutboxKafkaProperties(
	String topic,
	int batchSize
) {
	public OutboxKafkaProperties {
		if (topic == null || topic.isBlank()) {
			topic = "studywithme.outbox.events";
		}
		if (batchSize <= 0) {
			batchSize = 100;
		}
	}
}

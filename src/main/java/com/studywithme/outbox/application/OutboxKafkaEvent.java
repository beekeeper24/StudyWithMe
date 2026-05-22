package com.studywithme.outbox.application;

import com.fasterxml.jackson.databind.JsonNode;

public record OutboxKafkaEvent(
	String eventId,
	String eventType,
	String aggregateType,
	Long aggregateId,
	String occurredAt,
	JsonNode payload
) {
}

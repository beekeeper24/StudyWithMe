package com.studywithme.outbox.domain;

public enum OutboxKafkaPublishStatus {
	PENDING,
	PUBLISHED,
	FAILED,
	DEAD
}

package com.studywithme.outbox.domain;

public enum OutboxEventStatus {
	PENDING,
	PROCESSED,
	FAILED,
	DEAD
}

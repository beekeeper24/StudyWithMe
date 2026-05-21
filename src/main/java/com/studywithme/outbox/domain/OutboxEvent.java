package com.studywithme.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "event_type", nullable = false, length = 50)
	private String eventType;

	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private Long aggregateId;

	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OutboxEventStatus status;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "next_attempt_at", nullable = false)
	private LocalDateTime nextAttemptAt;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	protected OutboxEvent() {
	}

	private OutboxEvent(String eventType, String aggregateType, Long aggregateId, String payload) {
		LocalDateTime now = LocalDateTime.now();
		this.id = UUID.randomUUID().toString();
		this.eventType = eventType;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.payload = payload;
		this.status = OutboxEventStatus.PENDING;
		this.retryCount = 0;
		this.nextAttemptAt = now;
		this.occurredAt = now;
	}

	public static OutboxEvent create(String eventType, String aggregateType, Long aggregateId, String payload) {
		return new OutboxEvent(eventType, aggregateType, aggregateId, payload);
	}

	public void markProcessed() {
		this.status = OutboxEventStatus.PROCESSED;
		this.processedAt = LocalDateTime.now();
		this.lastError = null;
	}

	public void markFailed(String errorMessage) {
		this.retryCount++;
		this.status = retryCount >= 5 ? OutboxEventStatus.DEAD : OutboxEventStatus.FAILED;
		this.nextAttemptAt = LocalDateTime.now().plusSeconds(Math.min(600L, 10L * retryCount * retryCount));
		this.lastError = errorMessage == null ? null : errorMessage.substring(0, Math.min(1000, errorMessage.length()));
	}

	public void retryLater() {
		if (status == OutboxEventStatus.FAILED) {
			this.status = OutboxEventStatus.PENDING;
		}
	}

	public String getId() {
		return id;
	}

	public String getEventType() {
		return eventType;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public Long getAggregateId() {
		return aggregateId;
	}

	public String getPayload() {
		return payload;
	}

	public OutboxEventStatus getStatus() {
		return status;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public LocalDateTime getNextAttemptAt() {
		return nextAttemptAt;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}

	public String getLastError() {
		return lastError;
	}
}

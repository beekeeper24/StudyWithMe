package com.studywithme.notification.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.domain.NotificationType;
import com.studywithme.notification.repository.NotificationRepository;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.domain.OutboxEventStatus;
import com.studywithme.outbox.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationOutboxProcessor {

	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outboxEventRepository;
	private final NotificationRepository notificationRepository;
	private final ObjectProvider<NotificationRealtimePublisher> notificationRealtimePublisher;

	public NotificationOutboxProcessor(
		ObjectMapper objectMapper,
		OutboxEventRepository outboxEventRepository,
		NotificationRepository notificationRepository,
		ObjectProvider<NotificationRealtimePublisher> notificationRealtimePublisher
	) {
		this.objectMapper = objectMapper;
		this.outboxEventRepository = outboxEventRepository;
		this.notificationRepository = notificationRepository;
		this.notificationRealtimePublisher = notificationRealtimePublisher;
	}

	@Transactional
	public int processPending(int limit) {
		List<OutboxEvent> events = outboxEventRepository.findAllByStatusInAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
			List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
			LocalDateTime.now(),
			PageRequest.of(0, limit)
		);
		events.forEach(event -> processOne(event.getId()));
		return events.size();
	}

	@Transactional
	public void processOne(String eventId) {
		OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
		try {
			processEvent(event.getId(), event.getEventType(), event.getAggregateId(), event.getPayload());
			event.markProcessed();
		} catch (Exception exception) {
			event.markFailed(exception.getMessage());
		}
	}

	@Transactional
	public void processKafkaEvent(String sourceEventId, String eventType, Long aggregateId, String payload) {
		try {
			processEvent(sourceEventId, eventType, aggregateId, payload);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to process Kafka notification event", exception);
		}
	}

	private void processEvent(String sourceEventId, String eventType, Long aggregateId, String payload) throws Exception {
		if ("COMMENT_CREATED".equals(eventType)) {
			processCommentCreated(sourceEventId, payload);
		} else if ("REPLY_CREATED".equals(eventType)) {
			processReplyCreated(sourceEventId, payload);
		} else if ("COMMENT_MENTIONED".equals(eventType)) {
			processCommentMentioned(sourceEventId, payload);
		}
	}

	private void processCommentCreated(String sourceEventId, String eventPayload) throws Exception {
		JsonNode payload = objectMapper.readTree(eventPayload);
		Long receiverMemberId = payload.required("postAuthorMemberId").asLong();
		if (isReplacedByMention(payload.required("commentId").asLong(), receiverMemberId)) {
			return;
		}
		createNotificationIfNeeded(
			sourceEventId,
			receiverMemberId,
			payload.required("actorMemberId").asLong(),
			NotificationType.COMMENT_ON_POST,
			payload.required("commentId").asLong(),
			"새 댓글이 달렸습니다."
		);
	}

	private void processReplyCreated(String sourceEventId, String eventPayload) throws Exception {
		JsonNode payload = objectMapper.readTree(eventPayload);
		Long receiverMemberId = payload.required("parentCommentAuthorMemberId").asLong();
		if (isReplacedByMention(payload.required("commentId").asLong(), receiverMemberId)) {
			return;
		}
		createNotificationIfNeeded(
			sourceEventId,
			receiverMemberId,
			payload.required("actorMemberId").asLong(),
			NotificationType.REPLY_ON_COMMENT,
			payload.required("commentId").asLong(),
			"새 답글이 달렸습니다."
		);
	}

	private void processCommentMentioned(String sourceEventId, String eventPayload) throws Exception {
		JsonNode payload = objectMapper.readTree(eventPayload);
		Long actorMemberId = payload.required("actorMemberId").asLong();
		Long commentId = payload.required("commentId").asLong();
		for (Long mentionedMemberId : readLongArray(payload.required("mentionedMemberIds"))) {
			createNotificationIfNeeded(
				sourceEventId,
				mentionedMemberId,
				actorMemberId,
				NotificationType.MENTIONED_IN_COMMENT,
				commentId,
				"댓글에서 회원님을 멘션했습니다."
			);
		}
	}

	private boolean isReplacedByMention(Long commentId, Long receiverMemberId) throws Exception {
		List<OutboxEvent> mentionEvents = outboxEventRepository.findAllByEventTypeAndAggregateTypeAndAggregateIdOrderByOccurredAtAsc(
			"COMMENT_MENTIONED",
			"COMMENT",
			commentId
		);
		for (OutboxEvent mentionEvent : mentionEvents) {
			JsonNode payload = objectMapper.readTree(mentionEvent.getPayload());
			if (readLongArray(payload.required("replacedNotificationReceiverMemberIds")).contains(receiverMemberId)) {
				return true;
			}
		}
		return false;
	}

	private List<Long> readLongArray(JsonNode arrayNode) {
		List<Long> values = new ArrayList<>();
		arrayNode.forEach(node -> values.add(node.asLong()));
		return values;
	}

	private void createNotificationIfNeeded(
		String sourceEventId,
		Long receiverMemberId,
		Long actorMemberId,
		NotificationType type,
		Long targetId,
		String message
	) {
		if (receiverMemberId.equals(actorMemberId)) {
			return;
		}
		if (notificationRepository.existsBySourceEventIdAndReceiverMemberIdAndType(
			sourceEventId,
			receiverMemberId,
			type
		)) {
			return;
		}
		try {
			Notification notification = notificationRepository.save(Notification.create(
				receiverMemberId,
				actorMemberId,
				type,
				NotificationTargetType.COMMENT,
				targetId,
				sourceEventId,
				message
			));
			publishAfterCommit(NotificationResult.from(notification));
		} catch (DataIntegrityViolationException ignored) {
			// Another worker may have processed the same at-least-once event first.
		}
	}

	private void publishAfterCommit(NotificationResult notification) {
		NotificationRealtimePublisher publisher = notificationRealtimePublisher.getIfAvailable();
		if (publisher == null) {
			return;
		}
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			publisher.publish(notification);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publisher.publish(notification);
			}
		});
	}
}

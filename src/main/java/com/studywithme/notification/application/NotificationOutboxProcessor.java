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
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxProcessor {

	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outboxEventRepository;
	private final NotificationRepository notificationRepository;

	public NotificationOutboxProcessor(
		ObjectMapper objectMapper,
		OutboxEventRepository outboxEventRepository,
		NotificationRepository notificationRepository
	) {
		this.objectMapper = objectMapper;
		this.outboxEventRepository = outboxEventRepository;
		this.notificationRepository = notificationRepository;
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
			if ("COMMENT_CREATED".equals(event.getEventType())) {
				processCommentCreated(event);
			} else if ("REPLY_CREATED".equals(event.getEventType())) {
				processReplyCreated(event);
			}
			event.markProcessed();
		} catch (Exception exception) {
			event.markFailed(exception.getMessage());
		}
	}

	private void processCommentCreated(OutboxEvent event) throws Exception {
		JsonNode payload = objectMapper.readTree(event.getPayload());
		createNotificationIfNeeded(
			event,
			payload.required("postAuthorMemberId").asLong(),
			payload.required("actorMemberId").asLong(),
			NotificationType.COMMENT_ON_POST,
			payload.required("commentId").asLong(),
			"새 댓글이 달렸습니다."
		);
	}

	private void processReplyCreated(OutboxEvent event) throws Exception {
		JsonNode payload = objectMapper.readTree(event.getPayload());
		createNotificationIfNeeded(
			event,
			payload.required("parentCommentAuthorMemberId").asLong(),
			payload.required("actorMemberId").asLong(),
			NotificationType.REPLY_ON_COMMENT,
			payload.required("commentId").asLong(),
			"새 답글이 달렸습니다."
		);
	}

	private void createNotificationIfNeeded(
		OutboxEvent event,
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
			event.getId(),
			receiverMemberId,
			type
		)) {
			return;
		}
		try {
			notificationRepository.save(Notification.create(
				receiverMemberId,
				actorMemberId,
				type,
				NotificationTargetType.COMMENT,
				targetId,
				event.getId(),
				message
			));
		} catch (DataIntegrityViolationException ignored) {
			// Another worker may have processed the same at-least-once event first.
		}
	}
}

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
			} else if ("COMMENT_MENTIONED".equals(event.getEventType())) {
				processCommentMentioned(event);
			}
			event.markProcessed();
		} catch (Exception exception) {
			event.markFailed(exception.getMessage());
		}
	}

	private void processCommentCreated(OutboxEvent event) throws Exception {
		JsonNode payload = objectMapper.readTree(event.getPayload());
		Long receiverMemberId = payload.required("postAuthorMemberId").asLong();
		if (isReplacedByMention(payload.required("commentId").asLong(), receiverMemberId)) {
			return;
		}
		createNotificationIfNeeded(
			event,
			receiverMemberId,
			payload.required("actorMemberId").asLong(),
			NotificationType.COMMENT_ON_POST,
			payload.required("commentId").asLong(),
			"새 댓글이 달렸습니다."
		);
	}

	private void processReplyCreated(OutboxEvent event) throws Exception {
		JsonNode payload = objectMapper.readTree(event.getPayload());
		Long receiverMemberId = payload.required("parentCommentAuthorMemberId").asLong();
		if (isReplacedByMention(payload.required("commentId").asLong(), receiverMemberId)) {
			return;
		}
		createNotificationIfNeeded(
			event,
			receiverMemberId,
			payload.required("actorMemberId").asLong(),
			NotificationType.REPLY_ON_COMMENT,
			payload.required("commentId").asLong(),
			"새 답글이 달렸습니다."
		);
	}

	private void processCommentMentioned(OutboxEvent event) throws Exception {
		JsonNode payload = objectMapper.readTree(event.getPayload());
		Long actorMemberId = payload.required("actorMemberId").asLong();
		Long commentId = payload.required("commentId").asLong();
		for (Long mentionedMemberId : readLongArray(payload.required("mentionedMemberIds"))) {
			createNotificationIfNeeded(
				event,
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

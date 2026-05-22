package com.studywithme.outbox.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.repository.OutboxEventRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {

	private static final String AGGREGATE_TYPE_COMMENT = "COMMENT";

	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outboxEventRepository;

	public OutboxEventPublisher(
		ObjectMapper objectMapper,
		OutboxEventRepository outboxEventRepository
	) {
		this.objectMapper = objectMapper;
		this.outboxEventRepository = outboxEventRepository;
	}

	public void publishCommentCreated(
		Long postId,
		Long commentId,
		Long postAuthorMemberId,
		Long actorMemberId
	) {
		CommentCreatedPayload payload = new CommentCreatedPayload(
			postId,
			commentId,
			postAuthorMemberId,
			actorMemberId
		);
		save("COMMENT_CREATED", commentId, payload);
	}

	public void publishReplyCreated(
		Long postId,
		Long parentCommentId,
		Long commentId,
		Long parentCommentAuthorMemberId,
		Long actorMemberId
	) {
		ReplyCreatedPayload payload = new ReplyCreatedPayload(
			postId,
			parentCommentId,
			commentId,
			parentCommentAuthorMemberId,
			actorMemberId
		);
		save("REPLY_CREATED", commentId, payload);
	}

	public void publishCommentMentioned(
		Long postId,
		Long commentId,
		Long actorMemberId,
		List<Long> mentionedMemberIds,
		List<Long> replacedNotificationReceiverMemberIds
	) {
		if (mentionedMemberIds == null || mentionedMemberIds.isEmpty()) {
			return;
		}
		CommentMentionedPayload payload = new CommentMentionedPayload(
			postId,
			commentId,
			actorMemberId,
			mentionedMemberIds,
			replacedNotificationReceiverMemberIds == null ? List.of() : replacedNotificationReceiverMemberIds
		);
		save("COMMENT_MENTIONED", commentId, payload);
	}

	private void save(String eventType, Long aggregateId, Object payload) {
		try {
			outboxEventRepository.save(OutboxEvent.create(
				eventType,
				AGGREGATE_TYPE_COMMENT,
				aggregateId,
				objectMapper.writeValueAsString(payload)
			));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize outbox payload", exception);
		}
	}

	private record CommentCreatedPayload(
		Long postId,
		Long commentId,
		Long postAuthorMemberId,
		Long actorMemberId
	) {
	}

	private record ReplyCreatedPayload(
		Long postId,
		Long parentCommentId,
		Long commentId,
		Long parentCommentAuthorMemberId,
		Long actorMemberId
	) {
	}

	private record CommentMentionedPayload(
		Long postId,
		Long commentId,
		Long actorMemberId,
		List<Long> mentionedMemberIds,
		List<Long> replacedNotificationReceiverMemberIds
	) {
	}
}

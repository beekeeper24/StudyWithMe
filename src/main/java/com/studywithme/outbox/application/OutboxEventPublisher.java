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
	private static final String AGGREGATE_TYPE_STUDY = "STUDY";

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

	public void publishStudyJoinRequested(Long studyId, Long ownerMemberId, Long actorMemberId) {
		saveStudy("STUDY_JOIN_REQUESTED", studyId, new StudySingleReceiverPayload(
			studyId,
			ownerMemberId,
			actorMemberId
		));
	}

	public void publishStudyJoinApproved(Long studyId, Long participantMemberId, Long actorMemberId) {
		saveStudy("STUDY_JOIN_APPROVED", studyId, new StudySingleReceiverPayload(
			studyId,
			participantMemberId,
			actorMemberId
		));
	}

	public void publishStudyJoinRejected(Long studyId, Long participantMemberId, Long actorMemberId) {
		saveStudy("STUDY_JOIN_REJECTED", studyId, new StudySingleReceiverPayload(
			studyId,
			participantMemberId,
			actorMemberId
		));
	}

	public void publishStudyJoinCancelled(Long studyId, Long ownerMemberId, Long actorMemberId) {
		saveStudy("STUDY_JOIN_CANCELLED", studyId, new StudySingleReceiverPayload(
			studyId,
			ownerMemberId,
			actorMemberId
		));
	}

	public void publishStudyEnded(Long studyId, Long actorMemberId, List<Long> receiverMemberIds) {
		publishStudyMultiReceiver("STUDY_ENDED", studyId, actorMemberId, receiverMemberIds);
	}

	public void publishStudyDeleted(Long studyId, Long actorMemberId, List<Long> receiverMemberIds) {
		publishStudyMultiReceiver("STUDY_DELETED", studyId, actorMemberId, receiverMemberIds);
	}

	private void publishStudyMultiReceiver(
		String eventType,
		Long studyId,
		Long actorMemberId,
		List<Long> receiverMemberIds
	) {
		if (receiverMemberIds == null || receiverMemberIds.isEmpty()) {
			return;
		}
		saveStudy(eventType, studyId, new StudyMultiReceiverPayload(
			studyId,
			actorMemberId,
			receiverMemberIds
		));
	}

	private void save(String eventType, Long aggregateId, Object payload) {
		save(eventType, AGGREGATE_TYPE_COMMENT, aggregateId, payload);
	}

	private void saveStudy(String eventType, Long aggregateId, Object payload) {
		save(eventType, AGGREGATE_TYPE_STUDY, aggregateId, payload);
	}

	private void save(String eventType, String aggregateType, Long aggregateId, Object payload) {
		try {
			outboxEventRepository.save(OutboxEvent.create(
				eventType,
				aggregateType,
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

	private record StudySingleReceiverPayload(
		Long studyId,
		Long receiverMemberId,
		Long actorMemberId
	) {
	}

	private record StudyMultiReceiverPayload(
		Long studyId,
		Long actorMemberId,
		List<Long> receiverMemberIds
	) {
	}
}

package com.studywithme.notification.application;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.exception.NotificationErrorCode;
import com.studywithme.notification.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final CommentRepository commentRepository;

	public NotificationService(
		NotificationRepository notificationRepository,
		CommentRepository commentRepository
	) {
		this.notificationRepository = notificationRepository;
		this.commentRepository = commentRepository;
	}

	@Transactional(readOnly = true)
	public List<NotificationResult> findMine(Long requesterMemberId) {
		return notificationRepository.findAllByReceiverMemberIdOrderByCreatedAtDesc(requesterMemberId)
			.stream()
			.map(this::toResult)
			.toList();
	}

	@Transactional
	public NotificationResult markRead(Long notificationId, Long requesterMemberId) {
		Notification notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
		notification.markRead(requesterMemberId);
		return toResult(notification);
	}

	@Transactional
	public List<NotificationResult> markAllRead(Long requesterMemberId) {
		return notificationRepository.findAllByReceiverMemberIdOrderByCreatedAtDesc(requesterMemberId)
			.stream()
			.peek(notification -> notification.markRead(requesterMemberId))
			.map(this::toResult)
			.toList();
	}

	@Transactional
	public void delete(Long notificationId, Long requesterMemberId) {
		Notification notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
		if (!notification.getReceiverMemberId().equals(requesterMemberId)) {
			throw new BusinessException(NotificationErrorCode.NOT_NOTIFICATION_OWNER);
		}
		notificationRepository.delete(notification);
	}

	private NotificationResult toResult(Notification notification) {
		if (notification.getTargetType() != NotificationTargetType.COMMENT) {
			return NotificationResult.from(notification);
		}
		Long targetPostId = commentRepository.findById(notification.getTargetId())
			.map(Comment::getPostId)
			.orElse(null);
		return NotificationResult.from(notification, targetPostId);
	}
}

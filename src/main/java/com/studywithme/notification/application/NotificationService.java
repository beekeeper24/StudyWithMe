package com.studywithme.notification.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.exception.NotificationErrorCode;
import com.studywithme.notification.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional(readOnly = true)
	public List<NotificationResult> findMine(Long requesterMemberId) {
		return notificationRepository.findAllByReceiverMemberIdOrderByCreatedAtDesc(requesterMemberId)
			.stream()
			.map(NotificationResult::from)
			.toList();
	}

	@Transactional
	public NotificationResult markRead(Long notificationId, Long requesterMemberId) {
		Notification notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
		notification.markRead(requesterMemberId);
		return NotificationResult.from(notification);
	}
}

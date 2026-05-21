package com.studywithme.notification.repository;

import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findAllByReceiverMemberIdOrderByCreatedAtDesc(Long receiverMemberId);

	boolean existsBySourceEventIdAndReceiverMemberIdAndType(
		String sourceEventId,
		Long receiverMemberId,
		NotificationType type
	);
}

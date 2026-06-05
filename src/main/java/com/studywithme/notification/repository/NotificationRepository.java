package com.studywithme.notification.repository;

import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.domain.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findAllByReceiverMemberIdOrderByCreatedAtDesc(Long receiverMemberId);

	List<Notification> findAllByTargetTypeAndTargetIdAndReadAtIsNull(
		NotificationTargetType targetType,
		Long targetId
	);

	boolean existsBySourceEventIdAndReceiverMemberIdAndType(
		String sourceEventId,
		Long receiverMemberId,
		NotificationType type
	);
}

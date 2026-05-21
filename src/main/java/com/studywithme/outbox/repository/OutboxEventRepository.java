package com.studywithme.outbox.repository;

import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.domain.OutboxEventStatus;
import com.studywithme.outbox.domain.OutboxKafkaPublishStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

	List<OutboxEvent> findAllByOrderByOccurredAtAsc();

	List<OutboxEvent> findAllByStatusInAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
		List<OutboxEventStatus> statuses,
		LocalDateTime nextAttemptAt,
		Pageable pageable
	);

	List<OutboxEvent> findAllByKafkaPublishStatusInAndKafkaNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
		List<OutboxKafkaPublishStatus> statuses,
		LocalDateTime kafkaNextAttemptAt,
		Pageable pageable
	);
}

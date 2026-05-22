package com.studywithme.outbox.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.domain.OutboxKafkaPublishStatus;
import com.studywithme.outbox.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxKafkaRelay {

	private final ObjectMapper objectMapper;
	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final OutboxKafkaProperties properties;

	public OutboxKafkaRelay(
		ObjectMapper objectMapper,
		OutboxEventRepository outboxEventRepository,
		KafkaTemplate<String, String> kafkaTemplate,
		OutboxKafkaProperties properties
	) {
		this.objectMapper = objectMapper;
		this.outboxEventRepository = outboxEventRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Transactional
	public int publishPending(int limit) {
		List<OutboxEvent> events = outboxEventRepository.findAllByKafkaPublishStatusInAndKafkaNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
			List.of(OutboxKafkaPublishStatus.PENDING, OutboxKafkaPublishStatus.FAILED),
			LocalDateTime.now(),
			PageRequest.of(0, limit)
		);
		events.forEach(event -> publishOne(event.getId()));
		return events.size();
	}

	@Transactional
	public void publishOne(String eventId) {
		OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
		if (event.isKafkaPublished()) {
			return;
		}

		try {
			kafkaTemplate.send(properties.topic(), event.getId(), toKafkaPayload(event)).get();
			event.markKafkaPublished();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			event.markKafkaPublishFailed(exception.getMessage());
		} catch (ExecutionException | RuntimeException exception) {
			event.markKafkaPublishFailed(rootMessage(exception));
		} catch (Exception exception) {
			event.markKafkaPublishFailed(exception.getMessage());
		}
	}

	private String toKafkaPayload(OutboxEvent event) throws Exception {
		JsonNode payload = objectMapper.readTree(event.getPayload());
		OutboxKafkaEvent envelope = new OutboxKafkaEvent(
			event.getId(),
			event.getEventType(),
			event.getAggregateType(),
			event.getAggregateId(),
			event.getOccurredAt().toString(),
			payload
		);
		return objectMapper.writeValueAsString(envelope);
	}

	private String rootMessage(Throwable exception) {
		Throwable current = exception;
		while (current.getCause() != null) {
			current = current.getCause();
		}
		return current.getMessage();
	}
}

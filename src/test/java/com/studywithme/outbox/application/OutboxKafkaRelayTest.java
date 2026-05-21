package com.studywithme.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.domain.OutboxKafkaPublishStatus;
import com.studywithme.outbox.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxKafkaRelayTest {

	private static final String TOPIC = "studywithme.outbox.events";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private OutboxKafkaRelay relay;

	@BeforeEach
	void setUp() {
		relay = new OutboxKafkaRelay(
			objectMapper,
			outboxEventRepository,
			kafkaTemplate,
			new OutboxKafkaProperties(TOPIC, 100)
		);
	}

	@Test
	@DisplayName("due outbox event를 Kafka로 publish하고 publish 상태를 PUBLISHED로 바꾼다")
	void publishPendingPublishesDueEventsAndMarksPublished() throws Exception {
		OutboxEvent event = outboxEvent();
		given(outboxEventRepository.findAllByKafkaPublishStatusInAndKafkaNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
			eq(List.of(OutboxKafkaPublishStatus.PENDING, OutboxKafkaPublishStatus.FAILED)),
			any(LocalDateTime.class),
			any(Pageable.class)
		)).willReturn(List.of(event));
		given(outboxEventRepository.findById(event.getId())).willReturn(Optional.of(event));
		given(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.willReturn(CompletableFuture.completedFuture(null));

		int count = relay.publishPending(10);

		assertThat(count).isEqualTo(1);
		assertThat(event.getKafkaPublishStatus()).isEqualTo(OutboxKafkaPublishStatus.PUBLISHED);
		assertThat(event.getKafkaPublishedAt()).isNotNull();

		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(kafkaTemplate).send(eq(TOPIC), eq(event.getId()), payloadCaptor.capture());
		JsonNode kafkaPayload = objectMapper.readTree(payloadCaptor.getValue());
		assertThat(kafkaPayload.required("eventId").asText()).isEqualTo(event.getId());
		assertThat(kafkaPayload.required("eventType").asText()).isEqualTo("COMMENT_CREATED");
		assertThat(kafkaPayload.required("payload").required("commentId").asLong()).isEqualTo(10L);
	}

	@Test
	@DisplayName("Kafka publish 실패 시 FAILED로 남기고 재시도 정보를 기록한다")
	void publishFailureMarksFailed() {
		OutboxEvent event = outboxEvent();
		given(outboxEventRepository.findById(event.getId())).willReturn(Optional.of(event));
		CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
		failed.completeExceptionally(new RuntimeException("broker down"));
		given(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.willReturn(failed);

		relay.publishOne(event.getId());

		assertThat(event.getKafkaPublishStatus()).isEqualTo(OutboxKafkaPublishStatus.FAILED);
		assertThat(event.getKafkaRetryCount()).isEqualTo(1);
		assertThat(event.getKafkaLastError()).contains("broker down");
		assertThat(event.getKafkaNextAttemptAt()).isAfter(event.getOccurredAt());
	}

	@Test
	@DisplayName("Kafka publish가 5회 실패하면 DEAD가 된다")
	void publishFailureBecomesDeadAfterFiveAttempts() {
		OutboxEvent event = outboxEvent();
		given(outboxEventRepository.findById(event.getId())).willReturn(Optional.of(event));
		CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
		failed.completeExceptionally(new RuntimeException("broker down"));
		given(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.willReturn(failed);

		for (int i = 0; i < 5; i++) {
			relay.publishOne(event.getId());
		}

		assertThat(event.getKafkaPublishStatus()).isEqualTo(OutboxKafkaPublishStatus.DEAD);
		assertThat(event.getKafkaRetryCount()).isEqualTo(5);
	}

	@Test
	@DisplayName("이미 PUBLISHED인 event는 다시 Kafka로 보내지 않는다")
	void alreadyPublishedEventIsSkipped() {
		OutboxEvent event = outboxEvent();
		event.markKafkaPublished();
		given(outboxEventRepository.findById(event.getId())).willReturn(Optional.of(event));

		relay.publishOne(event.getId());

		verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
	}

	private OutboxEvent outboxEvent() {
		return OutboxEvent.create(
			"COMMENT_CREATED",
			"COMMENT",
			10L,
			"{\"postId\":1,\"commentId\":10,\"actorMemberId\":2}"
		);
	}
}

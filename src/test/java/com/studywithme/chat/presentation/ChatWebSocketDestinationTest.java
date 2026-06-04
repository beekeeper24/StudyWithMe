package com.studywithme.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.chat.exception.ChatErrorCode;
import com.studywithme.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatWebSocketDestinationTest {

	@Test
	@DisplayName("채팅 user queue 구독 destination에서 room id를 추출한다")
	void parseUserQueueSubscribeRoomId() {
		Long roomId = ChatWebSocketDestination.parseRoomId("/user/queue/chat.rooms.1");

		assertThat(roomId).isEqualTo(1L);
	}

	@Test
	@DisplayName("채팅 메시지 발행 destination에서 room id를 추출한다")
	void parseSendRoomId() {
		Long roomId = ChatWebSocketDestination.parseRoomId("/app/chat.rooms.10.messages");

		assertThat(roomId).isEqualTo(10L);
	}

	@Test
	@DisplayName("형식이 맞지 않는 채팅 destination은 거부한다")
	void rejectMalformedDestination() {
		assertThatThrownBy(() -> ChatWebSocketDestination.parseRoomId("/topic/chat.rooms.bad"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
	}

	@Test
	@DisplayName("공용 room topic 구독 destination은 거부한다")
	void rejectPublicTopicSubscribeDestination() {
		assertThatThrownBy(() -> ChatWebSocketDestination.parseRoomId("/topic/chat.rooms.1"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
	}
}

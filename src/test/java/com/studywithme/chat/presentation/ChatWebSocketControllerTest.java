package com.studywithme.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studywithme.chat.application.ChatMessageCreateCommand;
import com.studywithme.chat.application.ChatMessageResult;
import com.studywithme.chat.application.ChatService;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ChatWebSocketControllerTest {

	private final ChatService chatService = mock(ChatService.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final ChatWebSocketController controller = new ChatWebSocketController(chatService, messagingTemplate);

	@Test
	@DisplayName("WebSocket 채팅 메시지는 DB에 저장한 뒤 room topic으로 전달한다")
	void sendMessageStoresAndPublishes() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, Set.of("USER"));
		ChatMessageResult saved = new ChatMessageResult(100L, 10L, principal.memberId(), "안녕하세요", LocalDateTime.now());
		when(chatService.sendMessage(eq(10L), eq(principal.memberId()), org.mockito.ArgumentMatchers.any()))
			.thenReturn(saved);

		ChatMessageResponse response = controller.sendMessage(
			10L,
			new ChatWebSocketMessageRequest("안녕하세요"),
			principal
		);

		ArgumentCaptor<ChatMessageCreateCommand> commandCaptor = ArgumentCaptor.forClass(ChatMessageCreateCommand.class);
		verify(chatService).sendMessage(eq(10L), eq(principal.memberId()), commandCaptor.capture());
		verify(messagingTemplate).convertAndSend("/topic/chat.rooms.10", response);
		assertThat(commandCaptor.getValue().content()).isEqualTo("안녕하세요");
		assertThat(response.roomId()).isEqualTo(10L);
		assertThat(response.senderMemberId()).isEqualTo(principal.memberId());
		assertThat(response.content()).isEqualTo("안녕하세요");
	}
}

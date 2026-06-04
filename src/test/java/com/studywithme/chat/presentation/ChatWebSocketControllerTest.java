package com.studywithme.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studywithme.chat.application.ChatMessageCreateCommand;
import com.studywithme.chat.application.ChatMessageResult;
import com.studywithme.chat.application.ChatRoomMemberResult;
import com.studywithme.chat.application.ChatService;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import java.time.LocalDateTime;
import java.util.List;
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
	@DisplayName("WebSocket 채팅 메시지는 DB에 저장한 뒤 현재 방 참여자 user queue로 전달한다")
	void sendMessageStoresAndPublishesToCurrentRoomMembers() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, Set.of("USER"));
		ChatMessageResult saved = new ChatMessageResult(100L, 10L, principal.memberId(), "안녕하세요", LocalDateTime.now(), 0);
		LocalDateTime joinedAt = LocalDateTime.now();
		when(chatService.sendMessage(eq(10L), eq(principal.memberId()), org.mockito.ArgumentMatchers.any()))
			.thenReturn(saved);
		when(chatService.findRoomMembers(10L, principal.memberId()))
			.thenReturn(List.of(
				new ChatRoomMemberResult(principal.memberId(), "sender", null, joinedAt),
				new ChatRoomMemberResult(2L, "target", null, joinedAt)
			));

		ChatMessageResponse response = controller.sendMessage(
			10L,
			new ChatWebSocketMessageRequest("안녕하세요"),
			principal
		);

		ArgumentCaptor<ChatMessageCreateCommand> commandCaptor = ArgumentCaptor.forClass(ChatMessageCreateCommand.class);
		verify(chatService).sendMessage(eq(10L), eq(principal.memberId()), commandCaptor.capture());
		verify(messagingTemplate).convertAndSendToUser("1", "/queue/chat.rooms.10", response);
		verify(messagingTemplate).convertAndSendToUser("2", "/queue/chat.rooms.10", response);
		verify(messagingTemplate, never()).convertAndSend("/topic/chat.rooms.10", response);
		assertThat(commandCaptor.getValue().content()).isEqualTo("안녕하세요");
		assertThat(response.roomId()).isEqualTo(10L);
		assertThat(response.senderMemberId()).isEqualTo(principal.memberId());
		assertThat(response.content()).isEqualTo("안녕하세요");
	}
}

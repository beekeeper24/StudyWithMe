package com.studywithme.chat.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.chat.application.ChatMessageCreateCommand;
import com.studywithme.chat.application.ChatService;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;

	public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
		this.chatService = chatService;
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/chat.rooms.{roomId}.messages")
	public ChatMessageResponse sendMessage(
		@DestinationVariable Long roomId,
		@Valid @Payload ChatWebSocketMessageRequest request,
		Principal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		ChatMessageResponse response = ChatMessageResponse.from(chatService.sendMessage(
			roomId,
			authenticatedPrincipal.memberId(),
			new ChatMessageCreateCommand(request.content())
		));
		messagingTemplate.convertAndSend("/topic/chat.rooms." + roomId, response);
		return response;
	}

	private AuthenticatedMemberPrincipal requirePrincipal(Principal principal) {
		if (principal instanceof AuthenticatedMemberPrincipal authenticatedPrincipal) {
			return authenticatedPrincipal;
		}
		throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
	}
}

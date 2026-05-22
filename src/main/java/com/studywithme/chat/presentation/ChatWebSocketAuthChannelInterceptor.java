package com.studywithme.chat.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.token.AccessTokenClaims;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.chat.application.ChatService;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class ChatWebSocketAuthChannelInterceptor implements ChannelInterceptor {

	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String NOTIFICATION_QUEUE_DESTINATION = "/user/queue/notifications";

	private final JwtTokenProvider jwtTokenProvider;
	private final ChatService chatService;

	public ChatWebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, ChatService chatService) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.chatService = chatService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}

		if (accessor.getCommand() == StompCommand.CONNECT) {
			AuthenticatedMemberPrincipal principal = authenticate(accessor);
			accessor.setUser(principal);
			return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
		}

		if (accessor.getCommand() == StompCommand.SUBSCRIBE || accessor.getCommand() == StompCommand.SEND) {
			AuthenticatedMemberPrincipal principal = requirePrincipal(accessor.getUser());
			if (accessor.getCommand() == StompCommand.SUBSCRIBE
				&& NOTIFICATION_QUEUE_DESTINATION.equals(accessor.getDestination())) {
				return message;
			}
			Long roomId = ChatWebSocketDestination.parseRoomId(accessor.getDestination());
			chatService.validateRoomMembership(roomId, principal.memberId());
		}

		return message;
	}

	private AuthenticatedMemberPrincipal authenticate(StompHeaderAccessor accessor) {
		String authorization = accessor.getFirstNativeHeader(AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		AccessTokenClaims claims = jwtTokenProvider.parse(authorization.substring(BEARER_PREFIX.length()));
		return new AuthenticatedMemberPrincipal(claims.memberId(), claims.roles());
	}

	private AuthenticatedMemberPrincipal requirePrincipal(Principal principal) {
		if (principal instanceof AuthenticatedMemberPrincipal authenticatedPrincipal) {
			return authenticatedPrincipal;
		}
		throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
	}
}

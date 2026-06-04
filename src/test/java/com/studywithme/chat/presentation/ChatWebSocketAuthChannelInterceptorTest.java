package com.studywithme.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.TokenProperties;
import com.studywithme.chat.application.ChatService;
import com.studywithme.chat.exception.ChatErrorCode;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;

class ChatWebSocketAuthChannelInterceptorTest {

	private final ChatService chatService = mock(ChatService.class);
	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(tokenProperties());
	private final ChatWebSocketAuthChannelInterceptor interceptor = new ChatWebSocketAuthChannelInterceptor(
		jwtTokenProvider,
		chatService
	);

	@Test
	@DisplayName("STOMP CONNECT에 bearer token이 없으면 AUTH-003으로 거부한다")
	void rejectConnectWithoutBearerToken() {
		Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, null, null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("STOMP CONNECT에 유효한 bearer token이 있으면 Principal을 설정한다")
	void setPrincipalOnConnectWithValidBearerToken() {
		Member member = member(1L);
		Message<byte[]> message = stompMessage(
			StompCommand.CONNECT,
			null,
			null,
			"Bearer " + jwtTokenProvider.createAccessToken(member).token()
		);

		Message<?> result = interceptor.preSend(message, null);
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

		assertThat(accessor).isNotNull();
		assertThat(accessor.getUser()).isInstanceOf(AuthenticatedMemberPrincipal.class);
		assertThat(((AuthenticatedMemberPrincipal) accessor.getUser()).memberId()).isEqualTo(member.getId());
	}

	@Test
	@DisplayName("채팅방 참여자가 아니면 STOMP SUBSCRIBE를 거부한다")
	void rejectSubscribeByNonRoomMember() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, java.util.Set.of("USER"));
		doThrow(new BusinessException(ChatErrorCode.NOT_CHAT_ROOM_MEMBER))
			.when(chatService)
			.validateRoomMembership(10L, principal.memberId());
		Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/user/queue/chat.rooms.10", principal, null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.NOT_CHAT_ROOM_MEMBER);
	}

	@Test
	@DisplayName("채팅방 참여자는 STOMP user queue SUBSCRIBE를 통과한다")
	void allowUserQueueSubscribeByRoomMember() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, java.util.Set.of("USER"));
		Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/user/queue/chat.rooms.10", principal, null);

		Message<?> result = interceptor.preSend(message, null);

		assertThat(result).isSameAs(message);
	}

	@Test
	@DisplayName("채팅방 참여자는 STOMP SEND를 통과한다")
	void allowSendByRoomMember() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, java.util.Set.of("USER"));
		Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/chat.rooms.10.messages", principal, null);

		Message<?> result = interceptor.preSend(message, null);

		assertThat(result).isSameAs(message);
	}

	@Test
	@DisplayName("인증한 회원은 알림 user queue를 구독할 수 있다")
	void allowNotificationSubscribeByAuthenticatedMember() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, java.util.Set.of("USER"));
		Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/user/queue/notifications", principal, null);

		Message<?> result = interceptor.preSend(message, null);

		assertThat(result).isSameAs(message);
	}

	@Test
	@DisplayName("인증하지 않은 알림 user queue 구독은 AUTH-003으로 거부한다")
	void rejectNotificationSubscribeWithoutPrincipal() {
		Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/user/queue/notifications", null, null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("알림 user queue로 직접 STOMP SEND를 보낼 수 없다")
	void rejectSendToNotificationUserQueue() {
		AuthenticatedMemberPrincipal principal = new AuthenticatedMemberPrincipal(1L, java.util.Set.of("USER"));
		Message<byte[]> message = stompMessage(StompCommand.SEND, "/user/queue/notifications", principal, null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
	}

	private Message<byte[]> stompMessage(
		StompCommand command,
		String destination,
		Object user,
		String authorization
	) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		accessor.setLeaveMutable(true);
		if (destination != null) {
			accessor.setDestination(destination);
		}
		if (user instanceof AuthenticatedMemberPrincipal principal) {
			accessor.setUser(principal);
		}
		if (authorization != null) {
			accessor.addNativeHeader("Authorization", authorization);
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private Member member(Long id) {
		Member member = Member.createOAuthMember("member@example.com", "member", OAuthProvider.GOOGLE, "google-1", null);
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	private TokenProperties tokenProperties() {
		return new TokenProperties(
			"studywithme-test",
			"studywithme-test-secret-key-must-be-at-least-32-bytes",
			Duration.ofMinutes(30),
			Duration.ofDays(14)
		);
	}
}

package com.studywithme.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.chat.application.ChatRoomResult;
import com.studywithme.chat.application.ChatService;
import com.studywithme.chat.presentation.ChatWebSocketMessageRequest;
import com.studywithme.chat.repository.ChatMessageRepository;
import com.studywithme.chat.repository.ChatRoomMemberRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentService;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.notification.application.NotificationOutboxProcessor;
import com.studywithme.notification.repository.NotificationRepository;
import com.studywithme.outbox.repository.OutboxEventRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.post.repository.PostRepository;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketStompIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatRoomMemberRepository chatRoomMemberRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ChatService chatService;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private NotificationOutboxProcessor notificationOutboxProcessor;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll();
		outboxEventRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
		chatMessageRepository.deleteAll();
		chatRoomMemberRepository.deleteAll();
		chatRoomRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("실제 STOMP 연결로 채팅 메시지를 보내고 room topic에서 수신한다")
	void sendAndReceiveChatMessageOverStomp() throws Exception {
		Member sender = saveMember("sender");
		Member receiver = saveMember("receiver");
		ChatRoomResult room = chatService.createPrivateRoom(sender.getId(), receiver.getId());
		StompSession session = connect(sender);
		BlockingQueue<Map<String, Object>> receivedMessages = new LinkedBlockingQueue<>();
		session.subscribe("/topic/chat.rooms." + room.id(), mapFrameHandler(receivedMessages));

		session.send("/app/chat.rooms." + room.id() + ".messages", new ChatWebSocketMessageRequest("안녕하세요"));

		Map<String, Object> payload = receivedMessages.poll(5, TimeUnit.SECONDS);
		assertThat(payload).isNotNull();
		assertThat(((Number) payload.get("roomId")).longValue()).isEqualTo(room.id());
		assertThat(((Number) payload.get("senderMemberId")).longValue()).isEqualTo(sender.getId());
		assertThat(payload.get("content")).isEqualTo("안녕하세요");
	}

	@Test
	@DisplayName("실제 STOMP 연결로 알림 user queue를 구독하고 outbox 처리 알림을 수신한다")
	void receiveNotificationOverStompUserQueue() throws Exception {
		Member receiver = saveMember("receiver");
		Member actor = saveMember("actor");
		StompSession session = connect(receiver);
		BlockingQueue<Map<String, Object>> receivedNotifications = new LinkedBlockingQueue<>();
		session.subscribe("/user/queue/notifications", mapFrameHandler(receivedNotifications));

		PostResult post = postService.create(receiver.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(post.id(), actor.getId(), new CommentCreateCommand("댓글"));
		notificationOutboxProcessor.processPending(10);

		Map<String, Object> payload = receivedNotifications.poll(5, TimeUnit.SECONDS);
		assertThat(payload).isNotNull();
		assertThat(((Number) payload.get("receiverMemberId")).longValue()).isEqualTo(receiver.getId());
		assertThat(((Number) payload.get("actorMemberId")).longValue()).isEqualTo(actor.getId());
		assertThat(payload.get("type")).isEqualTo("COMMENT_ON_POST");
		assertThat(payload.get("message")).isEqualTo("새 댓글이 달렸습니다.");
	}

	private StompSession connect(Member member) throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		StompHeaders connectHeaders = new StompHeaders();
		connectHeaders.add("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member).token());
		return stompClient
			.connectAsync(
				"ws://localhost:" + port + "/ws",
				new WebSocketHttpHeaders(),
				connectHeaders,
				new StompSessionHandlerAdapter() {
			})
			.get(5, TimeUnit.SECONDS);
	}

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}

	private StompFrameHandler mapFrameHandler(BlockingQueue<Map<String, Object>> messages) {
		return new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void handleFrame(StompHeaders headers, Object payload) {
				messages.add((Map<String, Object>) payload);
			}
		};
	}
}

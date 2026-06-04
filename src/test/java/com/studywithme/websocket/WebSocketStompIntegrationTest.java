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
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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

	@Autowired
	private SimpUserRegistry simpUserRegistry;

	@BeforeEach
	void setUp() {
		cleanDatabase();
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	private void cleanDatabase() {
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
	@DisplayName("실제 STOMP 연결로 채팅 메시지를 보내고 user queue에서 수신한다")
	void sendAndReceiveChatMessageOverStomp() throws Exception {
		Member sender = saveMember("sender");
		Member receiver = saveMember("receiver");
		ChatRoomResult room = chatService.createPrivateRoom(sender.getId(), receiver.getId());
		StompSession session = connect(sender);
		try {
			BlockingQueue<Map<String, Object>> receivedMessages = new LinkedBlockingQueue<>();
			session.subscribe("/user/queue/chat.rooms." + room.id(), mapFrameHandler(receivedMessages));

			session.send("/app/chat.rooms." + room.id() + ".messages", new ChatWebSocketMessageRequest("안녕하세요"));

			Map<String, Object> payload = receivedMessages.poll(5, TimeUnit.SECONDS);
			assertThat(payload).isNotNull();
			assertThat(((Number) payload.get("roomId")).longValue()).isEqualTo(room.id());
			assertThat(((Number) payload.get("senderMemberId")).longValue()).isEqualTo(sender.getId());
			assertThat(payload.get("content")).isEqualTo("안녕하세요");
		} finally {
			session.disconnect();
		}
	}

	@Test
	@DisplayName("실제 STOMP 연결로 알림 user queue를 구독하고 outbox 처리 알림을 수신한다")
	void receiveNotificationOverStompUserQueue() throws Exception {
		Member receiver = saveMember("receiver");
		Member actor = saveMember("actor");
		StompSession session = connect(receiver);
		try {
			BlockingQueue<Map<String, Object>> receivedNotifications = new LinkedBlockingQueue<>();
			session.subscribe("/user/queue/notifications", mapFrameHandler(receivedNotifications));
			awaitUserSubscription(receiver, "/user/queue/notifications");

			PostResult post = postService.create(receiver.getId(), new PostCreateCommand("게시글", "내용"));
			commentService.create(post.id(), actor.getId(), new CommentCreateCommand("댓글"));
			notificationOutboxProcessor.processPending(10);

			Map<String, Object> payload = receivedNotifications.poll(5, TimeUnit.SECONDS);
			assertThat(payload).isNotNull();
			assertThat(((Number) payload.get("receiverMemberId")).longValue()).isEqualTo(receiver.getId());
			assertThat(((Number) payload.get("actorMemberId")).longValue()).isEqualTo(actor.getId());
			assertThat(payload.get("type")).isEqualTo("COMMENT_ON_POST");
			assertThat(payload.get("message")).isEqualTo("새 댓글이 달렸습니다.");
		} finally {
			session.disconnect();
		}
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

	private void awaitUserSubscription(Member member, String destination) throws InterruptedException {
		boolean registered = false;
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			registered = simpUserRegistry.getUsers()
				.stream()
				.filter(user -> user.getName().equals(member.getId().toString()))
				.flatMap(user -> user.getSessions().stream())
				.flatMap(session -> session.getSubscriptions().stream())
				.anyMatch(subscription -> destination.equals(subscription.getDestination()));
			if (registered) {
				break;
			}
			Thread.sleep(50);
		}
		assertThat(registered).isTrue();
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

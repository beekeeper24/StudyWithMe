package com.studywithme.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.chat.domain.ChatMessage;
import com.studywithme.chat.domain.ChatMessageReport;
import com.studywithme.chat.domain.ChatRoom;
import com.studywithme.chat.repository.ChatMessageReportRepository;
import com.studywithme.chat.repository.ChatMessageRepository;
import com.studywithme.chat.repository.ChatRoomRepository;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentResult;
import com.studywithme.comment.application.CommentService;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.mention.application.MentionExtractor;
import com.studywithme.mention.application.MentionTargetResolver;
import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.domain.NotificationType;
import com.studywithme.notification.repository.NotificationRepository;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.domain.OutboxEventStatus;
import com.studywithme.outbox.repository.OutboxEventRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.report.application.ContentReportService;
import com.studywithme.report.domain.ContentReport;
import com.studywithme.report.repository.ContentReportRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
	PostService.class,
	CommentService.class,
	OutboxEventPublisher.class,
	MentionExtractor.class,
		MentionTargetResolver.class,
		NotificationOutboxProcessor.class,
		NotificationService.class,
		ContentReportService.class,
		ObjectMapper.class
	})
class NotificationOutboxProcessorTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ChatMessageReportRepository chatMessageReportRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ContentReportRepository contentReportRepository;

	@Autowired
	private ContentReportService contentReportService;

	@Autowired
	private NotificationOutboxProcessor processor;

	@Test
	@DisplayName("PRIVATE_CHAT_REQUESTED event를 처리하면 상대방에게 채팅방 알림을 만든다")
	void processPrivateChatRequestedCreatesNotificationForTargetMember() {
		Member requester = saveMember("requester");
		Member target = saveMember("target");
		Long roomId = 101L;
		outboxEventRepository.deleteAll();
		notificationRepository.deleteAll();
		OutboxEventPublisher publisher = new OutboxEventPublisher(new ObjectMapper(), outboxEventRepository);
		publisher.publishPrivateChatRequested(roomId, target.getId(), requester.getId());

		int processedCount = processor.processPending(10);

		List<Notification> notifications = notificationRepository.findAll();
		assertThat(processedCount).isEqualTo(1);
		assertThat(notifications).singleElement().satisfies(notification -> {
			assertThat(notification.getReceiverMemberId()).isEqualTo(target.getId());
			assertThat(notification.getActorMemberId()).isEqualTo(requester.getId());
			assertThat(notification.getType()).isEqualTo(NotificationType.PRIVATE_CHAT_REQUESTED);
			assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.CHAT_ROOM);
			assertThat(notification.getTargetId()).isEqualTo(roomId);
			assertThat(notification.getMessage()).isEqualTo("1:1 채팅 요청이 도착했습니다.");
		});
	}

	@Test
	@DisplayName("CHAT_MESSAGE_REPORTED event를 처리하면 관리자들에게 신고 알림을 만든다")
	void processChatMessageReportedCreatesNotificationsForAdmins() {
		Member reporter = saveMember("reporter");
		Member reported = saveMember("reported");
		Member firstAdmin = saveAdmin("first-admin");
		Member secondAdmin = saveAdmin("second-admin");
		ChatMessageReport report = saveChatReport("report-notification", reporter, reported);
		outboxEventRepository.deleteAll();
		notificationRepository.deleteAll();
		OutboxEventPublisher publisher = new OutboxEventPublisher(new ObjectMapper(), outboxEventRepository);
		publisher.publishChatMessageReported(report.getId(), reporter.getId(), List.of(firstAdmin.getId(), secondAdmin.getId()));

		int processedCount = processor.processPending(10);

		List<Notification> notifications = notificationRepository.findAll();
		assertThat(processedCount).isEqualTo(1);
		assertThat(notifications).hasSize(2);
		assertThat(notifications).extracting(Notification::getReceiverMemberId)
			.containsExactlyInAnyOrder(firstAdmin.getId(), secondAdmin.getId());
		assertThat(notifications).allSatisfy(notification -> {
			assertThat(notification.getActorMemberId()).isEqualTo(reporter.getId());
			assertThat(notification.getType()).isEqualTo(NotificationType.CHAT_MESSAGE_REPORTED);
			assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.CHAT_REPORT);
			assertThat(notification.getTargetId()).isEqualTo(report.getId());
			assertThat(notification.getMessage()).isEqualTo("채팅 메시지 신고가 접수되었습니다.");
		});
	}

	@Test
	@DisplayName("CONTENT_REPORTED event를 처리하면 관리자들에게 커뮤니티 신고 알림을 만든다")
	void processContentReportedCreatesNotificationsForAdmins() {
		Member author = saveMember("content-reported-author");
		Member reporter = saveMember("content-reporter");
		Member firstAdmin = saveAdmin("content-first-admin");
		Member secondAdmin = saveAdmin("content-second-admin");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("신고 대상 글", "본문"));
		outboxEventRepository.deleteAll();
		notificationRepository.deleteAll();

		contentReportService.reportPost(post.id(), reporter.getId(), "확인 필요");

		int processedCount = processor.processPending(10);

		ContentReport report = contentReportRepository.findAll().getFirst();
		List<Notification> notifications = notificationRepository.findAll();
		assertThat(processedCount).isEqualTo(1);
		assertThat(notifications).hasSize(2);
		assertThat(notifications).extracting(Notification::getReceiverMemberId)
			.containsExactlyInAnyOrder(firstAdmin.getId(), secondAdmin.getId());
		assertThat(notifications).allSatisfy(notification -> {
			assertThat(notification.getActorMemberId()).isEqualTo(reporter.getId());
			assertThat(notification.getType()).isEqualTo(NotificationType.CONTENT_REPORTED);
			assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.CONTENT_REPORT);
			assertThat(notification.getTargetId()).isEqualTo(report.getId());
			assertThat(notification.getMessage()).isEqualTo("커뮤니티 신고가 접수되었습니다.");
		});
	}

	@Test
	@DisplayName("이미 담당자가 지정된 콘텐츠 신고 event는 관리자 신고 알림을 만들지 않는다")
	void skipContentReportNotificationsWhenReportAlreadyAssigned() {
		Member author = saveMember("assigned-content-author");
		Member reporter = saveMember("assigned-content-reporter");
		Member admin = saveAdmin("assigned-content-admin");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("신고 대상 글", "본문"));
		outboxEventRepository.deleteAll();
		notificationRepository.deleteAll();
		contentReportService.reportPost(post.id(), reporter.getId(), "확인 필요");
		ContentReport report = contentReportRepository.findAll().getFirst();
		report.assignTo(admin.getId());
		contentReportRepository.saveAndFlush(report);

		int processedCount = processor.processPending(10);

		assertThat(processedCount).isEqualTo(1);
		assertThat(notificationRepository.findAll()).isEmpty();
		assertThat(outboxEventRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
	}

	@Test
	@DisplayName("이미 담당자가 지정된 신고 event는 관리자 신고 알림을 만들지 않는다")
	void skipChatReportNotificationsWhenReportAlreadyAssigned() {
		Member reporter = saveMember("assigned-reporter");
		Member reported = saveMember("assigned-reported");
		Member admin = saveAdmin("assigned-admin");
		ChatMessageReport report = saveChatReport("assigned-report-notification", reporter, reported);
		report.assignTo(admin.getId());
		chatMessageReportRepository.saveAndFlush(report);
		outboxEventRepository.deleteAll();
		notificationRepository.deleteAll();
		OutboxEventPublisher publisher = new OutboxEventPublisher(new ObjectMapper(), outboxEventRepository);
		publisher.publishChatMessageReported(report.getId(), reporter.getId(), List.of(admin.getId()));

		int processedCount = processor.processPending(10);

		assertThat(processedCount).isEqualTo(1);
		assertThat(notificationRepository.findAll()).isEmpty();
		assertThat(outboxEventRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
	}

	@Test
	@DisplayName("COMMENT_CREATED event를 처리하면 게시글 작성자에게 알림을 만든다")
	void processCommentCreatedCreatesNotificationForPostAuthor() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		CommentResult comment = commentService.create(post.id(), commentAuthor.getId(), new CommentCreateCommand("댓글"));

		int processedCount = processor.processPending(10);

		List<Notification> notifications = notificationRepository.findAll();
		assertThat(processedCount).isEqualTo(1);
		assertThat(notifications).hasSize(1);
		Notification notification = notifications.getFirst();
		assertThat(notification.getReceiverMemberId()).isEqualTo(postAuthor.getId());
		assertThat(notification.getActorMemberId()).isEqualTo(commentAuthor.getId());
		assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT_ON_POST);
		assertThat(notification.getTargetId()).isEqualTo(comment.id());
		assertThat(outboxEventRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
	}

	@Test
	@DisplayName("REPLY_CREATED event를 처리하면 부모 댓글 작성자에게 알림을 만든다")
	void processReplyCreatedCreatesNotificationForParentCommentAuthor() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		Member replyAuthor = saveMember("reply-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		CommentResult parent = commentService.create(post.id(), commentAuthor.getId(), new CommentCreateCommand("댓글"));
		CommentResult reply = commentService.reply(parent.id(), replyAuthor.getId(), new CommentCreateCommand("답글"));

		int processedCount = processor.processPending(10);

		List<Notification> notifications = notificationRepository.findAll();
		assertThat(processedCount).isEqualTo(2);
		assertThat(notifications).extracting(Notification::getTargetId)
			.contains(parent.id(), reply.id());
		assertThat(notifications).anySatisfy(notification -> {
			assertThat(notification.getReceiverMemberId()).isEqualTo(commentAuthor.getId());
			assertThat(notification.getActorMemberId()).isEqualTo(replyAuthor.getId());
			assertThat(notification.getType()).isEqualTo(NotificationType.REPLY_ON_COMMENT);
			assertThat(notification.getTargetId()).isEqualTo(reply.id());
		});
	}

	@Test
	@DisplayName("본인이 자기 게시글에 댓글을 쓰면 알림을 만들지 않는다")
	void suppressSelfNotification() {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		int processedCount = processor.processPending(10);

		assertThat(processedCount).isEqualTo(1);
		assertThat(notificationRepository.findAll()).isEmpty();
		assertThat(outboxEventRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
	}

	@Test
	@DisplayName("같은 event를 재처리해도 중복 알림을 만들지 않는다")
	void processingSameEventTwiceIsIdempotent() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(post.id(), commentAuthor.getId(), new CommentCreateCommand("댓글"));
		OutboxEvent event = outboxEventRepository.findAll().getFirst();

		processor.processOne(event.getId());
		processor.processOne(event.getId());

		assertThat(notificationRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("COMMENT_MENTIONED event를 처리하면 멘션 대상자에게 알림을 만든다")
	void processCommentMentionedCreatesNotificationForMentionedMember() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		Member mentionedMember = saveMember("mentioned");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		CommentResult comment = commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("@mentioned 확인해주세요")
		);

		int processedCount = processor.processPending(10);

		assertThat(processedCount).isEqualTo(2);
		assertThat(notificationRepository.findAll()).anySatisfy(notification -> {
			assertThat(notification.getReceiverMemberId()).isEqualTo(mentionedMember.getId());
			assertThat(notification.getActorMemberId()).isEqualTo(commentAuthor.getId());
			assertThat(notification.getType()).isEqualTo(NotificationType.MENTIONED_IN_COMMENT);
			assertThat(notification.getTargetId()).isEqualTo(comment.id());
		});
	}

	@Test
	@DisplayName("같은 댓글에서 기본 댓글 알림 대상자가 멘션되면 멘션 알림만 만든다")
	void mentionReplacesCommentNotificationForSameReceiver() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("@post-author 확인해주세요")
		);

		processor.processPending(10);

		List<Notification> notifications = notificationRepository.findAll();
		assertThat(notifications).hasSize(1);
		assertThat(notifications.getFirst().getReceiverMemberId()).isEqualTo(postAuthor.getId());
		assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.MENTIONED_IN_COMMENT);
	}

	@Test
	@DisplayName("같은 댓글에서 같은 회원을 여러 번 멘션해도 알림은 하나만 만든다")
	void duplicateMentionsInSameCommentCreateOneNotification() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		Member mentionedMember = saveMember("mentioned");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("@mentioned @mentioned 확인해주세요")
		);

		processor.processPending(10);

		assertThat(notificationRepository.findAll()).filteredOn(
			notification -> notification.getReceiverMemberId().equals(mentionedMember.getId())
				&& notification.getType() == NotificationType.MENTIONED_IN_COMMENT
		).hasSize(1);
	}

	@Test
	@DisplayName("자기 자신을 멘션해도 멘션 알림을 만들지 않는다")
	void suppressSelfMentionNotification() {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(
			post.id(),
			author.getId(),
			new CommentCreateCommand("@author 혼잣말")
		);

		int processedCount = processor.processPending(10);

		assertThat(processedCount).isEqualTo(1);
		assertThat(notificationRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("같은 답글에서 기본 답글 알림 대상자가 멘션되면 멘션 알림만 만든다")
	void mentionReplacesReplyNotificationForSameReceiver() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		Member replyAuthor = saveMember("reply-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		CommentResult parent = commentService.create(post.id(), commentAuthor.getId(), new CommentCreateCommand("댓글"));
		CommentResult reply = commentService.reply(
			parent.id(),
			replyAuthor.getId(),
			new CommentCreateCommand("@comment-author 답글 확인해주세요")
		);

		processor.processPending(10);

		assertThat(notificationRepository.findAll()).filteredOn(
			notification -> notification.getReceiverMemberId().equals(commentAuthor.getId())
				&& notification.getTargetId().equals(reply.id())
		).singleElement()
			.satisfies(notification -> assertThat(notification.getType()).isEqualTo(NotificationType.MENTIONED_IN_COMMENT));
	}

	@Test
	@DisplayName("Kafka event 처리도 source event id 기준으로 멘션 알림을 idempotent하게 만든다")
	void processKafkaEventCreatesMentionNotificationIdempotently() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		Member mentionedMember = saveMember("mentioned");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		CommentResult comment = commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("일반 댓글")
		);
		String payload = """
			{
			  "postId": %d,
			  "commentId": %d,
			  "actorMemberId": %d,
			  "mentionedMemberIds": [%d],
			  "replacedNotificationReceiverMemberIds": []
			}
			""".formatted(post.id(), comment.id(), commentAuthor.getId(), mentionedMember.getId());

		processor.processKafkaEvent("kafka-event-1", "COMMENT_MENTIONED", comment.id(), payload);
		processor.processKafkaEvent("kafka-event-1", "COMMENT_MENTIONED", comment.id(), payload);

		assertThat(notificationRepository.findAll()).filteredOn(
			notification -> notification.getReceiverMemberId().equals(mentionedMember.getId())
				&& notification.getSourceEventId().equals("kafka-event-1")
				&& notification.getType() == NotificationType.MENTIONED_IN_COMMENT
		).hasSize(1);
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

	private Member saveAdmin(String name) {
		Member member = Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		);
		member.grantRole(MemberRole.ADMIN);
		return memberRepository.saveAndFlush(member);
	}

	private ChatMessageReport saveChatReport(String roomKey, Member reporter, Member reported) {
		ChatRoom room = chatRoomRepository.saveAndFlush(ChatRoom.privateRoom(roomKey));
		ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.create(
			room.getId(),
			reported.getId(),
			"신고 대상 메시지"
		));
		return chatMessageReportRepository.saveAndFlush(ChatMessageReport.create(
			room.getId(),
			message.getId(),
			reporter.getId(),
			reported.getId(),
			"신고 사유"
		));
	}
}

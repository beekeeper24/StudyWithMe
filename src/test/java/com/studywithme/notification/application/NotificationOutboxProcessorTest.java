package com.studywithme.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentResult;
import com.studywithme.comment.application.CommentService;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationType;
import com.studywithme.notification.repository.NotificationRepository;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.domain.OutboxEventStatus;
import com.studywithme.outbox.repository.OutboxEventRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
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
	NotificationOutboxProcessor.class,
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
	private NotificationOutboxProcessor processor;

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

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}
}

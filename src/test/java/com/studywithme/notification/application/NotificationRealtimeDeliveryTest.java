package com.studywithme.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentService;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.mention.application.MentionExtractor;
import com.studywithme.mention.application.MentionTargetResolver;
import com.studywithme.notification.repository.NotificationRepository;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
	PostService.class,
	CommentService.class,
	OutboxEventPublisher.class,
	MentionExtractor.class,
	MentionTargetResolver.class,
	NotificationOutboxProcessor.class,
	ObjectMapper.class,
	NotificationRealtimeDeliveryTest.RecordingNotificationRealtimePublisher.class
})
class NotificationRealtimeDeliveryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationOutboxProcessor processor;

	@Autowired
	private RecordingNotificationRealtimePublisher publisher;

	@Test
	@DisplayName("알림은 DB에 저장된 뒤 transaction commit 이후 실시간 전달된다")
	@Transactional
	void publishNotificationAfterCommit() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		commentService.create(post.id(), commentAuthor.getId(), new CommentCreateCommand("댓글"));

		processor.processPending(10);

		assertThat(notificationRepository.findAll()).hasSize(1);
		assertThat(publisher.publishedNotifications()).isEmpty();

		TestTransaction.flagForCommit();
		TestTransaction.end();

		assertThat(publisher.publishedNotifications())
			.singleElement()
			.satisfies(notification -> {
				assertThat(notification.receiverMemberId()).isEqualTo(postAuthor.getId());
				assertThat(notification.actorMemberId()).isEqualTo(commentAuthor.getId());
				assertThat(notification.targetPostId()).isEqualTo(post.id());
			});
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

	@TestConfiguration
	static class RecordingNotificationRealtimePublisher implements NotificationRealtimePublisher {

		private final List<NotificationResult> publishedNotifications = new ArrayList<>();

		@Override
		public void publish(NotificationResult notification) {
			publishedNotifications.add(notification);
		}

		List<NotificationResult> publishedNotifications() {
			return publishedNotifications;
		}
	}
}

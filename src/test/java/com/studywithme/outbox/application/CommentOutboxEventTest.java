package com.studywithme.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentResult;
import com.studywithme.comment.application.CommentService;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.outbox.domain.OutboxEvent;
import com.studywithme.outbox.repository.OutboxEventRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({PostService.class, CommentService.class, OutboxEventPublisher.class, ObjectMapper.class})
class CommentOutboxEventTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Test
	@DisplayName("댓글을 작성하면 COMMENT_CREATED outbox event가 같은 트랜잭션에서 저장된다")
	void createCommentStoresOutboxEvent() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));

		CommentResult comment = commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("댓글입니다.")
		);

		List<OutboxEvent> events = outboxEventRepository.findAll();
		assertThat(events).hasSize(1);
		OutboxEvent event = events.getFirst();
		assertThat(event.getEventType()).isEqualTo("COMMENT_CREATED");
		assertThat(event.getAggregateType()).isEqualTo("COMMENT");
		assertThat(event.getAggregateId()).isEqualTo(comment.id());
		assertThat(event.getPayload()).contains("\"postId\":" + post.id());
		assertThat(event.getPayload()).contains("\"commentId\":" + comment.id());
		assertThat(event.getPayload()).contains("\"postAuthorMemberId\":" + postAuthor.getId());
		assertThat(event.getPayload()).contains("\"actorMemberId\":" + commentAuthor.getId());
	}

	@Test
	@DisplayName("답글을 작성하면 REPLY_CREATED outbox event가 같은 트랜잭션에서 저장된다")
	void createReplyStoresOutboxEvent() {
		Member postAuthor = saveMember("post-author");
		Member commentAuthor = saveMember("comment-author");
		Member replyAuthor = saveMember("reply-author");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "내용"));
		CommentResult parent = commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("댓글입니다.")
		);

		CommentResult reply = commentService.reply(
			parent.id(),
			replyAuthor.getId(),
			new CommentCreateCommand("답글입니다.")
		);

		List<OutboxEvent> events = outboxEventRepository.findAllByOrderByOccurredAtAsc();
		assertThat(events).hasSize(2);
		OutboxEvent event = events.get(1);
		assertThat(event.getEventType()).isEqualTo("REPLY_CREATED");
		assertThat(event.getAggregateType()).isEqualTo("COMMENT");
		assertThat(event.getAggregateId()).isEqualTo(reply.id());
		assertThat(event.getPayload()).contains("\"parentCommentId\":" + parent.id());
		assertThat(event.getPayload()).contains("\"parentCommentAuthorMemberId\":" + commentAuthor.getId());
		assertThat(event.getPayload()).contains("\"actorMemberId\":" + replyAuthor.getId());
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

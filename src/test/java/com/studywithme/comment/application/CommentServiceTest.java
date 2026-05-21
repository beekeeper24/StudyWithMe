package com.studywithme.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.comment.exception.CommentErrorCode;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.outbox.application.OutboxEventPublisher;
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
class CommentServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Test
	@DisplayName("인증한 회원은 게시글에 댓글을 작성할 수 있다")
	void createComment() {
		Member author = saveMember("author");
		PostResult post = savePost(author);

		CommentResult result = commentService.create(
			post.id(),
			author.getId(),
			new CommentCreateCommand("첫 댓글입니다.")
		);

		assertThat(result.postId()).isEqualTo(post.id());
		assertThat(result.authorMemberId()).isEqualTo(author.getId());
		assertThat(result.parentCommentId()).isNull();
		assertThat(result.status()).isEqualTo(CommentStatus.PUBLISHED);
	}

	@Test
	@DisplayName("인증한 회원은 댓글에 1단계 답글을 작성할 수 있다")
	void createReply() {
		Member author = saveMember("author");
		Member replier = saveMember("replier");
		PostResult post = savePost(author);
		CommentResult parent = commentService.create(
			post.id(),
			author.getId(),
			new CommentCreateCommand("부모 댓글")
		);

		CommentResult reply = commentService.reply(
			parent.id(),
			replier.getId(),
			new CommentCreateCommand("답글입니다.")
		);

		assertThat(reply.postId()).isEqualTo(post.id());
		assertThat(reply.parentCommentId()).isEqualTo(parent.id());
		assertThat(reply.authorMemberId()).isEqualTo(replier.getId());
	}

	@Test
	@DisplayName("답글에는 다시 답글을 작성할 수 없다")
	void rejectNestedReply() {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult parent = commentService.create(post.id(), author.getId(), new CommentCreateCommand("부모"));
		CommentResult reply = commentService.reply(parent.id(), author.getId(), new CommentCreateCommand("답글"));

		assertThatThrownBy(() -> commentService.reply(
			reply.id(),
			author.getId(),
			new CommentCreateCommand("중첩 답글")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CommentErrorCode.NESTED_REPLY_NOT_ALLOWED);
	}

	@Test
	@DisplayName("게시글 댓글 목록은 댓글과 답글을 생성순으로 조회한다")
	void findCommentsByPost() {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult first = commentService.create(post.id(), author.getId(), new CommentCreateCommand("첫 댓글"));
		CommentResult reply = commentService.reply(first.id(), author.getId(), new CommentCreateCommand("답글"));

		List<CommentResult> comments = commentService.findAllByPostId(post.id());

		assertThat(comments).extracting(CommentResult::id)
			.containsExactly(first.id(), reply.id());
	}

	@Test
	@DisplayName("작성자는 댓글을 수정할 수 있다")
	void updateCommentByAuthor() {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("수정 전"));

		CommentResult result = commentService.update(
			comment.id(),
			author.getId(),
			new CommentUpdateCommand("수정 후")
		);

		assertThat(result.content()).isEqualTo("수정 후");
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 댓글을 수정할 수 없다")
	void rejectUpdateByNonAuthor() {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		assertThatThrownBy(() -> commentService.update(
			comment.id(),
			nonAuthor.getId(),
			new CommentUpdateCommand("수정")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CommentErrorCode.NOT_COMMENT_AUTHOR);
	}

	@Test
	@DisplayName("작성자는 댓글을 삭제 상태로 바꿀 수 있다")
	void deleteCommentByAuthor() {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		commentService.delete(comment.id(), author.getId());

		assertThat(commentRepository.findById(comment.id()).orElseThrow().getStatus())
			.isEqualTo(CommentStatus.DELETED);
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 댓글을 삭제할 수 없다")
	void rejectDeleteByNonAuthor() {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		assertThatThrownBy(() -> commentService.delete(comment.id(), nonAuthor.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CommentErrorCode.NOT_COMMENT_AUTHOR);
	}

	@Test
	@DisplayName("삭제된 부모 댓글의 답글은 공개 목록에서 숨긴다")
	void hideRepliesUnderDeletedParent() {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult parent = commentService.create(post.id(), author.getId(), new CommentCreateCommand("부모"));
		CommentResult reply = commentService.reply(parent.id(), author.getId(), new CommentCreateCommand("답글"));
		commentService.delete(parent.id(), author.getId());

		List<CommentResult> comments = commentService.findAllByPostId(post.id());

		assertThat(comments).extracting(CommentResult::id)
			.doesNotContain(parent.id(), reply.id());
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

	private PostResult savePost(Member author) {
		return postService.create(author.getId(), new PostCreateCommand("게시글", "내용"));
	}
}

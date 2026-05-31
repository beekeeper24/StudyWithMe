package com.studywithme.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.domain.Post;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CommentRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Test
	@DisplayName("게시글 댓글을 저장하면 작성자와 게시글 참조가 함께 저장된다")
	void saveComment() {
		Member author = saveMember("author");
		Post post = savePost(author);

		Comment comment = commentRepository.saveAndFlush(Comment.create(
			post.getId(),
			author.getId(),
			null,
			"첫 댓글입니다."
		));

		assertThat(comment.getId()).isNotNull();
		assertThat(comment.getPostId()).isEqualTo(post.getId());
		assertThat(comment.getAuthorMemberId()).isEqualTo(author.getId());
		assertThat(comment.getParentCommentId()).isNull();
		assertThat(comment.getStatus()).isEqualTo(CommentStatus.PUBLISHED);
	}

	@Test
	@DisplayName("게시글 댓글 목록은 삭제 댓글을 제외하고 생성순으로 조회한다")
	void findPublishedCommentsByPost() {
		Member author = saveMember("author");
		Post post = savePost(author);
		Comment first = commentRepository.save(Comment.create(post.getId(), author.getId(), null, "첫 댓글"));
		Comment deleted = commentRepository.save(Comment.create(post.getId(), author.getId(), null, "삭제 댓글"));
		Comment second = commentRepository.save(Comment.create(post.getId(), author.getId(), null, "두 번째 댓글"));
		deleted.delete(author.getId());
		commentRepository.flush();

		List<Comment> comments = commentRepository.findAllByPostIdAndStatusOrderByCreatedAtAsc(
			post.getId(),
			CommentStatus.PUBLISHED
		);

		assertThat(comments).extracting(Comment::getId)
			.containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("게시글별 공개 댓글 수는 삭제 댓글과 삭제 부모 아래 답글을 제외한다")
	void countVisibleCommentsByPostIds() {
		Member author = saveMember("author");
		Post post = savePost(author);
		Comment parent = commentRepository.save(Comment.create(post.getId(), author.getId(), null, "댓글"));
		commentRepository.save(Comment.create(post.getId(), author.getId(), parent.getId(), "답글"));
		Comment deleted = commentRepository.save(Comment.create(post.getId(), author.getId(), null, "삭제 댓글"));
		Comment deletedParent = commentRepository.save(Comment.create(post.getId(), author.getId(), null, "삭제 부모"));
		Comment hiddenReply = commentRepository.save(Comment.create(post.getId(), author.getId(), deletedParent.getId(), "숨김 답글"));
		deleted.delete(author.getId());
		deletedParent.delete(author.getId());
		commentRepository.flush();

		List<CommentCountView> counts = commentRepository.countVisibleCommentsByPostIds(
			List.of(post.getId()),
			CommentStatus.PUBLISHED
		);

		assertThat(counts).hasSize(1);
		assertThat(counts.getFirst().getPostId()).isEqualTo(post.getId());
		assertThat(counts.getFirst().getCommentCount()).isEqualTo(2);
		assertThat(hiddenReply.getStatus()).isEqualTo(CommentStatus.PUBLISHED);
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

	private Post savePost(Member author) {
		return postRepository.saveAndFlush(Post.create("게시글", "내용", author.getId()));
	}
}

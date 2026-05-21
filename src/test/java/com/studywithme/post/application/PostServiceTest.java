package com.studywithme.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.domain.PostStatus;
import com.studywithme.post.exception.PostErrorCode;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(PostService.class)
class PostServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private PostService postService;

	@Test
	@DisplayName("인증한 회원은 게시글을 작성할 수 있다")
	void createPost() {
		Member author = saveMember("author");

		PostResult result = postService.create(
			author.getId(),
			new PostCreateCommand("첫 게시글", "반갑습니다.")
		);

		assertThat(result.authorMemberId()).isEqualTo(author.getId());
		assertThat(result.title()).isEqualTo("첫 게시글");
		assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	@DisplayName("공개 게시글 목록은 삭제된 게시글을 제외한다")
	void findAllPublishedPosts() {
		Member author = saveMember("author");
		PostResult visiblePost = postService.create(author.getId(), new PostCreateCommand("공개 글", "내용"));
		PostResult deletedPost = postService.create(author.getId(), new PostCreateCommand("삭제 글", "내용"));
		postService.delete(deletedPost.id(), author.getId());

		List<PostResult> posts = postService.findAll();

		assertThat(posts).extracting(PostResult::id)
			.containsExactly(visiblePost.id());
	}

	@Test
	@DisplayName("공개 게시글 상세를 조회할 수 있다")
	void findPublishedPostById() {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("공개 글", "내용"));

		PostResult result = postService.findById(post.id());

		assertThat(result.id()).isEqualTo(post.id());
		assertThat(result.title()).isEqualTo("공개 글");
	}

	@Test
	@DisplayName("작성자는 게시글을 수정할 수 있다")
	void updatePostByAuthor() {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("수정 전", "내용"));

		PostResult result = postService.update(
			post.id(),
			author.getId(),
			new PostUpdateCommand("수정 후", "바뀐 내용")
		);

		assertThat(result.title()).isEqualTo("수정 후");
		assertThat(result.content()).isEqualTo("바뀐 내용");
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 게시글을 수정할 수 없다")
	void rejectUpdateByNonAuthor() {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));

		assertThatThrownBy(() -> postService.update(
			post.id(),
			nonAuthor.getId(),
			new PostUpdateCommand("수정", "내용")
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(PostErrorCode.NOT_POST_AUTHOR);
	}

	@Test
	@DisplayName("작성자는 게시글을 삭제 상태로 바꿀 수 있다")
	void deletePostByAuthor() {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));

		postService.delete(post.id(), author.getId());

		assertThat(postRepository.findById(post.id()).orElseThrow().getStatus())
			.isEqualTo(PostStatus.DELETED);
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 게시글을 삭제할 수 없다")
	void rejectDeleteByNonAuthor() {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));

		assertThatThrownBy(() -> postService.delete(post.id(), nonAuthor.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(PostErrorCode.NOT_POST_AUTHOR);
	}

	@Test
	@DisplayName("삭제된 게시글 상세는 찾을 수 없다")
	void deletedPostIsHiddenFromDetail() {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));
		postService.delete(post.id(), author.getId());

		assertThatThrownBy(() -> postService.findById(post.id()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(PostErrorCode.POST_NOT_FOUND);
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

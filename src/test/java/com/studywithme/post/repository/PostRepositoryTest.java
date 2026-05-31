package com.studywithme.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostBoardType;
import com.studywithme.post.domain.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class PostRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Test
	@DisplayName("게시글을 저장하면 작성자와 공개 상태가 함께 저장된다")
	void savePost() {
		Member author = saveMember("author");

		Post post = postRepository.saveAndFlush(Post.create(
			"첫 게시글",
			"반갑습니다.",
			author.getId()
		));

		assertThat(post.getId()).isNotNull();
		assertThat(post.getAuthorMemberId()).isEqualTo(author.getId());
		assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
	}

	@Test
	@DisplayName("공개 게시글 목록은 삭제된 게시글을 제외하고 최신순으로 조회한다")
	void findPublishedPostsExcludesDeletedPosts() {
		Member author = saveMember("author");
		Post oldPost = postRepository.save(Post.create("오래된 글", "내용", author.getId()));
		Post deletedPost = postRepository.save(Post.create("삭제된 글", "내용", author.getId()));
		Post newPost = postRepository.save(Post.create("최신 글", "내용", author.getId()));
		deletedPost.delete(author.getId());
		postRepository.flush();

		Page<Post> posts = postRepository.findAllByStatus(
			PostStatus.PUBLISHED,
			latestPageRequest()
		);

		assertThat(posts.getContent()).extracting(Post::getId)
			.containsExactly(newPost.getId(), oldPost.getId());
	}

	@Test
	@DisplayName("공개 게시글 검색은 삭제 글을 제외하고 게시판과 키워드를 함께 적용한다")
	void searchPublishedPostsByBoardTypeAndKeyword() {
		Member author = saveMember("author");
		Post freePost = postRepository.save(Post.create("React 질문", "내용", author.getId()));
		Post reviewPost = postRepository.save(Post.create(PostBoardType.REVIEW, "React 후기", "내용", author.getId()));
		Post deletedPost = postRepository.save(Post.create(PostBoardType.REVIEW, "React 삭제", "내용", author.getId()));
		deletedPost.delete(author.getId());
		postRepository.flush();

		Page<Post> posts = postRepository.searchPublishedPosts(
			PostBoardType.REVIEW,
			PostStatus.PUBLISHED,
			"react",
			true,
			true,
			true,
			latestPageRequest()
		);

		assertThat(posts.getContent()).extracting(Post::getId)
			.containsExactly(reviewPost.getId())
			.doesNotContain(freePost.getId(), deletedPost.getId());
	}

	@Test
	@DisplayName("공개 게시글 검색은 작성자 닉네임도 적용한다")
	void searchPublishedPostsByAuthorNickname() {
		Member reactAuthor = saveMember("react-master");
		Member springAuthor = saveMember("spring-master");
		Post matchedPost = postRepository.save(Post.create("일반 글", "내용", reactAuthor.getId()));
		Post otherPost = postRepository.save(Post.create("다른 글", "내용", springAuthor.getId()));
		postRepository.flush();

		Page<Post> posts = postRepository.searchPublishedPosts(
			null,
			PostStatus.PUBLISHED,
			"react",
			true,
			true,
			true,
			latestPageRequest()
		);

		assertThat(posts.getContent()).extracting(Post::getId)
			.containsExactly(matchedPost.getId())
			.doesNotContain(otherPost.getId());
	}

	@Test
	@DisplayName("공개 게시글 검색은 선택한 검색 범위만 적용한다")
	void searchPublishedPostsBySelectedScope() {
		Member author = saveMember("react-master");
		Post titlePost = postRepository.save(Post.create("React 제목", "일반 내용", author.getId()));
		Post contentPost = postRepository.save(Post.create("일반 제목", "React 본문", author.getId()));
		Post authorPost = postRepository.save(Post.create("일반 글", "일반 내용", author.getId()));
		postRepository.flush();

		Page<Post> titleOnlyPosts = postRepository.searchPublishedPosts(
			null,
			PostStatus.PUBLISHED,
			"react",
			true,
			false,
			false,
			latestPageRequest()
		);
		Page<Post> authorOnlyPosts = postRepository.searchPublishedPosts(
			null,
			PostStatus.PUBLISHED,
			"react",
			false,
			false,
			true,
			latestPageRequest()
		);

		assertThat(titleOnlyPosts.getContent()).extracting(Post::getId)
			.containsExactly(titlePost.getId())
			.doesNotContain(contentPost.getId(), authorPost.getId());
		assertThat(authorOnlyPosts.getContent()).extracting(Post::getId)
			.containsExactly(authorPost.getId(), contentPost.getId(), titlePost.getId());
	}

	private PageRequest latestPageRequest() {
		return PageRequest.of(
			0,
			50,
			Sort.by(Sort.Direction.DESC, "createdAt")
				.and(Sort.by(Sort.Direction.DESC, "id"))
		);
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

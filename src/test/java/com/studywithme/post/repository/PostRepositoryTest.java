package com.studywithme.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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

		List<Post> posts = postRepository.findAllByStatusOrderByCreatedAtDesc(
			PostStatus.PUBLISHED,
			PageRequest.of(0, 50)
		);

		assertThat(posts).extracting(Post::getId)
			.containsExactly(newPost.getId(), oldPost.getId());
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

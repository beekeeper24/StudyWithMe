package com.studywithme.post.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.post.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		postRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증하지 않고 게시글을 작성하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedCreatePost() throws Exception {
		mockMvc.perform(post("/api/v1/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "첫 게시글",
						"content": "반갑습니다."
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 게시글을 수정하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedUpdatePost() throws Exception {
		mockMvc.perform(put("/api/v1/posts/{postId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "수정",
						"content": "내용"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 게시글을 삭제하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedDeletePost() throws Exception {
		mockMvc.perform(delete("/api/v1/posts/{postId}", 1L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("게시글 목록은 공개 조회할 수 있다")
	void listPostsPublicly() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("첫 게시글", "반갑습니다."));

		mockMvc.perform(get("/api/v1/posts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].title").value("첫 게시글"));
	}

	@Test
	@DisplayName("게시글 목록은 작성자 표시 정보와 요청자 소유 여부를 내려준다")
	void listPostsWithAuthorDisplayAndOwnership() throws Exception {
		Member author = saveMember("author", "작가", "https://example.com/author.png");
		postService.create(author.getId(), new PostCreateCommand("첫 게시글", "반갑습니다."));

		mockMvc.perform(get("/api/v1/posts")
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].authorMemberId").value(author.getId()))
			.andExpect(jsonPath("$.data[0].authorNickname").value("작가"))
			.andExpect(jsonPath("$.data[0].authorProfileImageUrl").value("https://example.com/author.png"))
			.andExpect(jsonPath("$.data[0].ownedByRequester").value(true));
	}

	@Test
	@DisplayName("게시글 목록을 비회원으로 조회하면 요청자 소유 여부는 false다")
	void listPostsPubliclyWithFalseOwnership() throws Exception {
		Member author = saveMember("author", "작가", null);
		postService.create(author.getId(), new PostCreateCommand("첫 게시글", "반갑습니다."));

		mockMvc.perform(get("/api/v1/posts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].authorNickname").value("작가"))
			.andExpect(jsonPath("$.data[0].ownedByRequester").value(false));
	}

	@Test
	@DisplayName("게시글 상세는 공개 조회할 수 있다")
	void getPostPublicly() throws Exception {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("첫 게시글", "반갑습니다."));

		mockMvc.perform(get("/api/v1/posts/{postId}", post.id()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(post.id()))
			.andExpect(jsonPath("$.data.title").value("첫 게시글"));
	}

	@Test
	@DisplayName("게시글 상세는 작성자 표시 정보와 요청자 소유 여부를 내려준다")
	void getPostWithAuthorDisplayAndOwnership() throws Exception {
		Member author = saveMember("author", "작가", "https://example.com/author.png");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("첫 게시글", "반갑습니다."));

		mockMvc.perform(get("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.authorMemberId").value(author.getId()))
			.andExpect(jsonPath("$.data.authorNickname").value("작가"))
			.andExpect(jsonPath("$.data.authorProfileImageUrl").value("https://example.com/author.png"))
			.andExpect(jsonPath("$.data.ownedByRequester").value(true));
	}

	@Test
	@DisplayName("인증한 회원은 게시글을 작성할 수 있다")
	void createPostWithBearerToken() throws Exception {
		Member author = saveMember("author");

		mockMvc.perform(post("/api/v1/posts")
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "첫 게시글",
						"content": "반갑습니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("첫 게시글"))
			.andExpect(jsonPath("$.data.authorMemberId").value(author.getId()));
	}

	@Test
	@DisplayName("작성자는 게시글을 수정할 수 있다")
	void updatePostByAuthorWithBearerToken() throws Exception {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("수정 전", "내용"));

		mockMvc.perform(put("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "수정 후",
						"content": "바뀐 내용"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("수정 후"))
			.andExpect(jsonPath("$.data.content").value("바뀐 내용"));
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 게시글을 수정할 수 없다")
	void rejectUpdatePostByNonAuthor() throws Exception {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));

		mockMvc.perform(put("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(nonAuthor))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "수정",
						"content": "내용"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("POST-002"));
	}

	@Test
	@DisplayName("작성자는 게시글을 삭제할 수 있다")
	void deletePostByAuthorWithBearerToken() throws Exception {
		Member author = saveMember("author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));

		mockMvc.perform(delete("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 게시글을 삭제할 수 없다")
	void rejectDeletePostByNonAuthor() throws Exception {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("제목", "내용"));

		mockMvc.perform(delete("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(nonAuthor)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("POST-002"));
	}

	private Member saveMember(String name) {
		return saveMember(name, name, null);
	}

	private Member saveMember(String name, String nickname, String profileImageUrl) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			nickname,
			OAuthProvider.GOOGLE,
			"google-" + name,
			profileImageUrl
		));
	}

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}

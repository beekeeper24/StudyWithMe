package com.studywithme.comment.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentResult;
import com.studywithme.comment.application.CommentService;
import com.studywithme.comment.repository.CommentRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("delete from comments where parent_comment_id is not null");
		jdbcTemplate.update("delete from comments where parent_comment_id is null");
		postRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증하지 않고 댓글을 작성하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedCreateComment() throws Exception {
		mockMvc.perform(post("/api/v1/posts/{postId}/comments", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "첫 댓글입니다."
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 답글을 작성하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedCreateReply() throws Exception {
		mockMvc.perform(post("/api/v1/comments/{commentId}/replies", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "답글입니다."
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 댓글을 수정하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedUpdateComment() throws Exception {
		mockMvc.perform(put("/api/v1/comments/{commentId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "수정"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증하지 않고 댓글을 삭제하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedDeleteComment() throws Exception {
		mockMvc.perform(delete("/api/v1/comments/{commentId}", 1L))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("게시글 댓글 목록은 공개 조회할 수 있다")
	void listCommentsPublicly() throws Exception {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		commentService.create(post.id(), author.getId(), new CommentCreateCommand("첫 댓글입니다."));

		mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.id()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].content").value("첫 댓글입니다."));
	}

	@Test
	@DisplayName("인증한 회원은 댓글을 작성할 수 있다")
	void createCommentWithBearerToken() throws Exception {
		Member author = saveMember("author");
		PostResult post = savePost(author);

		mockMvc.perform(post("/api/v1/posts/{postId}/comments", post.id())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "첫 댓글입니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.postId").value(post.id()))
			.andExpect(jsonPath("$.data.parentCommentId").doesNotExist())
			.andExpect(jsonPath("$.data.content").value("첫 댓글입니다."));
	}

	@Test
	@DisplayName("인증한 회원은 댓글에 답글을 작성할 수 있다")
	void createReplyWithBearerToken() throws Exception {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult parent = commentService.create(
			post.id(),
			author.getId(),
			new CommentCreateCommand("부모 댓글")
		);

		mockMvc.perform(post("/api/v1/comments/{commentId}/replies", parent.id())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "답글입니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.parentCommentId").value(parent.id()))
			.andExpect(jsonPath("$.data.content").value("답글입니다."));
	}

	@Test
	@DisplayName("작성자는 댓글을 수정할 수 있다")
	void updateCommentByAuthorWithBearerToken() throws Exception {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("수정 전"));

		mockMvc.perform(put("/api/v1/comments/{commentId}", comment.id())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "수정 후"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").value("수정 후"));
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 댓글을 수정할 수 없다")
	void rejectUpdateCommentByNonAuthor() throws Exception {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		mockMvc.perform(put("/api/v1/comments/{commentId}", comment.id())
				.header("Authorization", "Bearer " + accessToken(nonAuthor))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"content": "수정"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COMMENT-002"));
	}

	@Test
	@DisplayName("작성자는 댓글을 삭제할 수 있다")
	void deleteCommentByAuthorWithBearerToken() throws Exception {
		Member author = saveMember("author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		mockMvc.perform(delete("/api/v1/comments/{commentId}", comment.id())
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("작성자가 아닌 회원은 댓글을 삭제할 수 없다")
	void rejectDeleteCommentByNonAuthor() throws Exception {
		Member author = saveMember("author");
		Member nonAuthor = saveMember("non-author");
		PostResult post = savePost(author);
		CommentResult comment = commentService.create(post.id(), author.getId(), new CommentCreateCommand("댓글"));

		mockMvc.perform(delete("/api/v1/comments/{commentId}", comment.id())
				.header("Authorization", "Bearer " + accessToken(nonAuthor)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COMMENT-002"));
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

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}

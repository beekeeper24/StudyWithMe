package com.studywithme.report.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentResult;
import com.studywithme.comment.application.CommentService;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.post.repository.PostRepository;
import com.studywithme.report.domain.ContentReportStatus;
import com.studywithme.report.repository.ContentReportRepository;
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
class ContentReportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ContentReportRepository contentReportRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		contentReportRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증한 회원은 게시글을 신고할 수 있다")
	void reportPost() throws Exception {
		Member author = saveMember("post-author");
		Member reporter = saveMember("post-reporter");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("신고 대상 글", "본문"));

		mockMvc.perform(post("/api/v1/posts/{postId}/reports", post.id())
				.header("Authorization", "Bearer " + accessToken(reporter))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ContentReportRequest("부적절한 게시글입니다."))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.targetType").value("POST"))
			.andExpect(jsonPath("$.data.targetId").value(post.id()))
			.andExpect(jsonPath("$.data.reporterMemberId").value(reporter.getId()))
			.andExpect(jsonPath("$.data.reportedMemberId").value(author.getId()))
			.andExpect(jsonPath("$.data.reporterNickname").doesNotExist())
			.andExpect(jsonPath("$.data.reportedNickname").doesNotExist());
	}

	@Test
	@DisplayName("인증한 회원은 댓글을 신고할 수 있다")
	void reportComment() throws Exception {
		Member postAuthor = saveMember("comment-post-author");
		Member commentAuthor = saveMember("comment-author");
		Member reporter = saveMember("comment-reporter");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "본문"));
		CommentResult comment = commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("신고 대상 댓글")
		);

		mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.id())
				.header("Authorization", "Bearer " + accessToken(reporter))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ContentReportRequest("부적절한 댓글입니다."))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.targetType").value("COMMENT"))
			.andExpect(jsonPath("$.data.targetId").value(comment.id()))
			.andExpect(jsonPath("$.data.postId").value(post.id()));
	}

	@Test
	@DisplayName("관리자는 콘텐츠 신고를 조회하고 담당 후 처리할 수 있다")
	void findAssignAndHandleContentReportByAdmin() throws Exception {
		Member author = saveMember("admin-report-author");
		Member reporter = saveMember("admin-report-reporter");
		Member admin = saveAdmin("admin-report-admin");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("신고 대상 글", "본문"));
		mockMvc.perform(post("/api/v1/posts/{postId}/reports", post.id())
				.header("Authorization", "Bearer " + accessToken(reporter))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ContentReportRequest("확인 필요"))))
			.andExpect(status().isOk());
		Long reportId = contentReportRepository.findAll().getFirst().getId();

		mockMvc.perform(get("/api/v1/admin/content-reports")
				.param("status", "PENDING")
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].id").value(reportId))
			.andExpect(jsonPath("$.data[0].reporterNickname").value("admin-report-reporter"))
			.andExpect(jsonPath("$.data[0].reportedNickname").value("admin-report-author"));

		mockMvc.perform(post("/api/v1/admin/content-reports/{reportId}/assign", reportId)
				.header("Authorization", "Bearer " + accessToken(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.assignedAdminMemberId").value(admin.getId()))
			.andExpect(jsonPath("$.data.assignedAdminNickname").value("admin-report-admin"));

		mockMvc.perform(post("/api/v1/admin/content-reports/{reportId}/handle", reportId)
				.header("Authorization", "Bearer " + accessToken(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ContentReportHandleRequest(
					ContentReportStatus.RESOLVED,
					"처리 완료"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RESOLVED"))
			.andExpect(jsonPath("$.data.handlerMemberId").value(admin.getId()))
			.andExpect(jsonPath("$.data.handlerNickname").value("admin-report-admin"))
			.andExpect(jsonPath("$.data.handlingNote").value("처리 완료"));
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

	private Member saveAdmin(String name) {
		Member member = Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		);
		member.grantRole(MemberRole.ADMIN);
		return memberRepository.saveAndFlush(member);
	}

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}

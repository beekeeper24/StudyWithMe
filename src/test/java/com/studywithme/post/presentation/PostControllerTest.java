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
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.post.domain.PostBoardType;
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
			.andExpect(jsonPath("$.data.content[0].title").value("첫 게시글"))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	@DisplayName("게시글 목록은 게시판 종류로 필터링할 수 있다")
	void listPostsByBoardType() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("자유 글", "내용"));
		postService.create(author.getId(), new PostCreateCommand(PostBoardType.QUESTION, "질문 글", "내용"));

		mockMvc.perform(get("/api/v1/posts")
				.param("boardType", "QUESTION"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].boardType").value("QUESTION"))
			.andExpect(jsonPath("$.data.content[0].title").value("질문 글"));
	}

	@Test
	@DisplayName("게시글 목록은 제목과 본문 키워드로 검색할 수 있다")
	void listPostsByKeyword() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("React 집중 스터디 후기", "좋았습니다."));
		postService.create(author.getId(), new PostCreateCommand("일반 글", "Spring 질문을 정리합니다."));
		postService.create(author.getId(), new PostCreateCommand("잡담", "오늘 점심 이야기"));

		mockMvc.perform(get("/api/v1/posts")
				.param("keyword", "spring"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].title").value("일반 글"));
	}

	@Test
	@DisplayName("게시글 목록은 제목만 검색할 수 있다")
	void listPostsByTitleSearchScope() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("React 제목", "일반 내용"));
		postService.create(author.getId(), new PostCreateCommand("일반 제목", "React 본문"));

		mockMvc.perform(get("/api/v1/posts")
				.param("keyword", "React")
				.param("searchScope", "TITLE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].title").value("React 제목"));
	}

	@Test
	@DisplayName("게시글 목록은 제목과 본문만 검색할 수 있다")
	void listPostsByTitleContentSearchScope() throws Exception {
		Member reactAuthor = saveMember("react-author", "리액트장인", null);
		Member otherAuthor = saveMember("other-author", "다른장인", null);
		postService.create(reactAuthor.getId(), new PostCreateCommand("일반 제목", "일반 내용"));
		postService.create(otherAuthor.getId(), new PostCreateCommand("일반 제목", "React 본문"));

		mockMvc.perform(get("/api/v1/posts")
				.param("keyword", "React")
				.param("searchScope", "TITLE_CONTENT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].title").value("일반 제목"))
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("다른장인"));
	}

	@Test
	@DisplayName("게시글 목록은 작성자 닉네임으로 검색할 수 있다")
	void listPostsByAuthorNicknameKeyword() throws Exception {
		Member reactAuthor = saveMember("react-author", "리액트장인", null);
		Member springAuthor = saveMember("spring-author", "스프링장인", null);
		postService.create(reactAuthor.getId(), new PostCreateCommand("일반 후기", "내용"));
		postService.create(springAuthor.getId(), new PostCreateCommand("다른 후기", "내용"));

		mockMvc.perform(get("/api/v1/posts")
				.param("keyword", "리액트"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("리액트장인"))
			.andExpect(jsonPath("$.data.content[0].title").value("일반 후기"));
	}

	@Test
	@DisplayName("게시글 목록은 작성자만 검색할 수 있다")
	void listPostsByAuthorSearchScope() throws Exception {
		Member reactAuthor = saveMember("react-author", "리액트장인", null);
		Member otherAuthor = saveMember("other-author", "다른장인", null);
		postService.create(reactAuthor.getId(), new PostCreateCommand("일반 제목", "일반 내용"));
		postService.create(otherAuthor.getId(), new PostCreateCommand("React 제목", "React 본문"));

		mockMvc.perform(get("/api/v1/posts")
				.param("keyword", "리액트")
				.param("searchScope", "AUTHOR"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("리액트장인"))
			.andExpect(jsonPath("$.data.content[0].title").value("일반 제목"));
	}

	@Test
	@DisplayName("게시글 검색은 게시판 종류와 함께 적용된다")
	void listPostsByBoardTypeAndKeyword() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("React 질문", "내용"));
		postService.create(author.getId(), new PostCreateCommand(PostBoardType.REVIEW, "React 후기", "내용"));

		mockMvc.perform(get("/api/v1/posts")
				.param("boardType", "REVIEW")
				.param("keyword", "React"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].boardType").value("REVIEW"))
			.andExpect(jsonPath("$.data.content[0].title").value("React 후기"));
	}

	@Test
	@DisplayName("게시글 목록은 페이지와 크기를 지정해 조회할 수 있다")
	void listPostsWithPagination() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("첫 번째", "내용"));
		postService.create(author.getId(), new PostCreateCommand("두 번째", "내용"));
		postService.create(author.getId(), new PostCreateCommand("세 번째", "내용"));

		mockMvc.perform(get("/api/v1/posts")
				.param("page", "1")
				.param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].title").value("첫 번째"))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(2))
			.andExpect(jsonPath("$.data.hasNext").value(false))
			.andExpect(jsonPath("$.data.hasPrevious").value(true));
	}

	@Test
	@DisplayName("게시글 목록은 오래된순으로 정렬할 수 있다")
	void listPostsByOldestSortOrder() throws Exception {
		Member author = saveMember("author");
		postService.create(author.getId(), new PostCreateCommand("첫 번째", "내용"));
		postService.create(author.getId(), new PostCreateCommand("두 번째", "내용"));
		postService.create(author.getId(), new PostCreateCommand("세 번째", "내용"));

		mockMvc.perform(get("/api/v1/posts")
				.param("sortOrder", "OLDEST"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.content[0].title").value("첫 번째"))
			.andExpect(jsonPath("$.data.content[1].title").value("두 번째"))
			.andExpect(jsonPath("$.data.content[2].title").value("세 번째"));
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
			.andExpect(jsonPath("$.data.content[0].authorMemberId").value(author.getId()))
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("작가"))
			.andExpect(jsonPath("$.data.content[0].authorProfileImageUrl").value("https://example.com/author.png"))
			.andExpect(jsonPath("$.data.content[0].ownedByRequester").value(true));
	}

	@Test
	@DisplayName("게시글 목록을 비회원으로 조회하면 요청자 소유 여부는 false다")
	void listPostsPubliclyWithFalseOwnership() throws Exception {
		Member author = saveMember("author", "작가", null);
		postService.create(author.getId(), new PostCreateCommand("첫 게시글", "반갑습니다."));

		mockMvc.perform(get("/api/v1/posts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("작가"))
			.andExpect(jsonPath("$.data.content[0].ownedByRequester").value(false));
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
						"boardType": "QUESTION",
						"title": "첫 게시글",
						"content": "반갑습니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.boardType").value("QUESTION"))
			.andExpect(jsonPath("$.data.title").value("첫 게시글"))
			.andExpect(jsonPath("$.data.authorMemberId").value(author.getId()));
	}

	@Test
	@DisplayName("일반 회원은 공지사항 게시글을 작성할 수 없다")
	void rejectNoticePostByNonAdmin() throws Exception {
		Member author = saveMember("author");

		mockMvc.perform(post("/api/v1/posts")
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"boardType": "NOTICE",
						"title": "공지",
						"content": "관리자 공지입니다."
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("POST-003"));
	}

	@Test
	@DisplayName("관리자는 공지사항 게시글을 작성할 수 있다")
	void createNoticePostByAdmin() throws Exception {
		Member admin = saveAdmin("admin");

		mockMvc.perform(post("/api/v1/posts")
				.header("Authorization", "Bearer " + accessToken(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"boardType": "NOTICE",
						"title": "공지",
						"content": "관리자 공지입니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.boardType").value("NOTICE"))
			.andExpect(jsonPath("$.data.title").value("공지"));
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
	@DisplayName("일반 회원은 공지사항 게시글을 수정할 수 없다")
	void rejectUpdateNoticePostByNonAdmin() throws Exception {
		Member admin = saveAdmin("admin");
		Member nonAdmin = saveMember("non-admin");
		PostResult post = postService.create(
			admin.getId(),
			new PostCreateCommand(PostBoardType.NOTICE, "공지", "내용")
		);

		mockMvc.perform(put("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(nonAdmin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "수정",
						"content": "내용"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("POST-003"));
	}

	@Test
	@DisplayName("관리자는 다른 관리자가 작성한 공지사항 게시글도 수정할 수 있다")
	void updateNoticePostByAdmin() throws Exception {
		Member authorAdmin = saveAdmin("author-admin");
		Member anotherAdmin = saveAdmin("another-admin");
		PostResult post = postService.create(
			authorAdmin.getId(),
			new PostCreateCommand(PostBoardType.NOTICE, "공지", "내용")
		);

		mockMvc.perform(put("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(anotherAdmin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "수정된 공지",
						"content": "수정된 내용"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("수정된 공지"))
			.andExpect(jsonPath("$.data.content").value("수정된 내용"));
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

	@Test
	@DisplayName("일반 회원은 공지사항 게시글을 삭제할 수 없다")
	void rejectDeleteNoticePostByNonAdmin() throws Exception {
		Member admin = saveAdmin("admin");
		Member nonAdmin = saveMember("non-admin");
		PostResult post = postService.create(
			admin.getId(),
			new PostCreateCommand(PostBoardType.NOTICE, "공지", "내용")
		);

		mockMvc.perform(delete("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(nonAdmin)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("POST-003"));
	}

	@Test
	@DisplayName("관리자는 다른 관리자가 작성한 공지사항 게시글도 삭제할 수 있다")
	void deleteNoticePostByAdmin() throws Exception {
		Member authorAdmin = saveAdmin("author-admin");
		Member anotherAdmin = saveAdmin("another-admin");
		PostResult post = postService.create(
			authorAdmin.getId(),
			new PostCreateCommand(PostBoardType.NOTICE, "공지", "내용")
		);

		mockMvc.perform(delete("/api/v1/posts/{postId}", post.id())
				.header("Authorization", "Bearer " + accessToken(anotherAdmin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
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

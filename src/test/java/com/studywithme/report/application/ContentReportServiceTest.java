package com.studywithme.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.comment.application.CommentCreateCommand;
import com.studywithme.comment.application.CommentResult;
import com.studywithme.comment.application.CommentService;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.mention.application.MentionExtractor;
import com.studywithme.mention.application.MentionTargetResolver;
import com.studywithme.notification.application.NotificationService;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.post.application.PostCreateCommand;
import com.studywithme.post.application.PostResult;
import com.studywithme.post.application.PostService;
import com.studywithme.post.repository.PostRepository;
import com.studywithme.report.domain.ContentReportStatus;
import com.studywithme.report.domain.ContentReportTargetType;
import com.studywithme.report.exception.ContentReportErrorCode;
import com.studywithme.report.repository.ContentReportRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({
	PostService.class,
	CommentService.class,
	ContentReportService.class,
	NotificationService.class,
	MentionExtractor.class,
	MentionTargetResolver.class,
	ObjectMapper.class
})
class ContentReportServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ContentReportRepository contentReportRepository;

	@Autowired
	private PostService postService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private ContentReportService contentReportService;

	@MockitoBean
	private OutboxEventPublisher outboxEventPublisher;

	@Test
	@DisplayName("회원은 다른 회원의 게시글을 신고할 수 있다")
	void reportPost() {
		Member author = saveMember("post-author");
		Member reporter = saveMember("post-reporter");
		Member admin = saveAdmin("post-admin");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("신고 대상 글", "본문입니다."));
		clearInvocations(outboxEventPublisher);

		ContentReportResult report = contentReportService.reportPost(
			post.id(),
			reporter.getId(),
			"부적절한 게시글입니다."
		);

		assertThat(report.targetType()).isEqualTo(ContentReportTargetType.POST);
		assertThat(report.targetId()).isEqualTo(post.id());
		assertThat(report.postId()).isEqualTo(post.id());
		assertThat(report.reporterMemberId()).isEqualTo(reporter.getId());
		assertThat(report.reportedMemberId()).isEqualTo(author.getId());
		assertThat(report.targetTitle()).isEqualTo("신고 대상 글");
		assertThat(report.targetContent()).isEqualTo("본문입니다.");
		assertThat(report.status()).isEqualTo(ContentReportStatus.PENDING);
		verify(outboxEventPublisher).publishContentReported(report.id(), reporter.getId(), List.of(admin.getId()));
	}

	@Test
	@DisplayName("회원은 다른 회원의 댓글을 신고할 수 있다")
	void reportComment() {
		Member postAuthor = saveMember("comment-post-author");
		Member commentAuthor = saveMember("comment-author");
		Member reporter = saveMember("comment-reporter");
		PostResult post = postService.create(postAuthor.getId(), new PostCreateCommand("게시글", "본문"));
		CommentResult comment = commentService.create(
			post.id(),
			commentAuthor.getId(),
			new CommentCreateCommand("신고 대상 댓글")
		);

		ContentReportResult report = contentReportService.reportComment(
			comment.id(),
			reporter.getId(),
			"부적절한 댓글입니다."
		);

		assertThat(report.targetType()).isEqualTo(ContentReportTargetType.COMMENT);
		assertThat(report.targetId()).isEqualTo(comment.id());
		assertThat(report.postId()).isEqualTo(post.id());
		assertThat(report.reportedMemberId()).isEqualTo(commentAuthor.getId());
		assertThat(report.targetTitle()).isNull();
		assertThat(report.targetContent()).isEqualTo("신고 대상 댓글");
		assertThat(report.status()).isEqualTo(ContentReportStatus.PENDING);
	}

	@Test
	@DisplayName("자신이 작성한 게시글은 신고할 수 없다")
	void rejectReportOwnPost() {
		Member author = saveMember("own-post-author");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("내 글", "본문"));

		assertThatThrownBy(() -> contentReportService.reportPost(post.id(), author.getId(), "내 글 신고"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ContentReportErrorCode.CANNOT_REPORT_OWN_CONTENT);
	}

	@Test
	@DisplayName("같은 콘텐츠를 중복 신고할 수 없다")
	void rejectDuplicateReport() {
		Member author = saveMember("duplicate-author");
		Member reporter = saveMember("duplicate-reporter");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("글", "본문"));
		contentReportService.reportPost(post.id(), reporter.getId(), "첫 신고");

		assertThatThrownBy(() -> contentReportService.reportPost(post.id(), reporter.getId(), "중복 신고"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ContentReportErrorCode.CONTENT_REPORT_DUPLICATED);
	}

	@Test
	@DisplayName("관리자는 콘텐츠 신고를 담당하고 처리할 수 있다")
	void assignAndHandleReportByAdmin() {
		Member author = saveMember("handle-author");
		Member reporter = saveMember("handle-reporter");
		Member admin = saveAdmin("handle-admin");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("글", "본문"));
		ContentReportResult report = contentReportService.reportPost(post.id(), reporter.getId(), "확인 필요");

		ContentReportResult assigned = contentReportService.assignReport(report.id(), admin.getId());

		assertThat(assigned.assignedAdminMemberId()).isEqualTo(admin.getId());
		assertThat(assigned.assignedAdminNickname()).isEqualTo("handle-admin");
		assertThat(assigned.assignedAt()).isNotNull();

		ContentReportResult handled = contentReportService.handleReport(
			report.id(),
			admin.getId(),
			ContentReportStatus.RESOLVED,
			"처리 완료"
		);

		assertThat(handled.status()).isEqualTo(ContentReportStatus.RESOLVED);
		assertThat(handled.handlerMemberId()).isEqualTo(admin.getId());
		assertThat(handled.handlerNickname()).isEqualTo("handle-admin");
		assertThat(handled.handlingNote()).isEqualTo("처리 완료");
		assertThat(handled.handledAt()).isNotNull();
	}

	@Test
	@DisplayName("일반 회원은 콘텐츠 신고 관리자 목록을 조회할 수 없다")
	void rejectFindReportsByNonAdmin() {
		Member requester = saveMember("non-admin-reader");

		assertThatThrownBy(() -> contentReportService.findReports(requester.getId(), ContentReportStatus.PENDING))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ContentReportErrorCode.CONTENT_REPORT_ADMIN_REQUIRED);
	}

	@Test
	@DisplayName("일반 회원은 콘텐츠 신고를 담당할 수 없다")
	void rejectAssignReportByNonAdmin() {
		Member author = saveMember("non-admin-assign-author");
		Member reporter = saveMember("non-admin-assign-reporter");
		Member requester = saveMember("non-admin-assign-requester");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("글", "본문"));
		ContentReportResult report = contentReportService.reportPost(post.id(), reporter.getId(), "확인 필요");

		assertThatThrownBy(() -> contentReportService.assignReport(report.id(), requester.getId()))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ContentReportErrorCode.CONTENT_REPORT_ADMIN_REQUIRED);
	}

	@Test
	@DisplayName("담당자가 아닌 관리자는 콘텐츠 신고를 처리할 수 없다")
	void rejectHandleReportByOtherAdmin() {
		Member author = saveMember("other-admin-author");
		Member reporter = saveMember("other-admin-reporter");
		Member assignedAdmin = saveAdmin("assigned-admin");
		Member otherAdmin = saveAdmin("other-admin");
		PostResult post = postService.create(author.getId(), new PostCreateCommand("글", "본문"));
		ContentReportResult report = contentReportService.reportPost(post.id(), reporter.getId(), "확인 필요");
		contentReportService.assignReport(report.id(), assignedAdmin.getId());

		assertThatThrownBy(() -> contentReportService.handleReport(
				report.id(),
				otherAdmin.getId(),
				ContentReportStatus.RESOLVED,
				"처리 시도"
			))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ContentReportErrorCode.CONTENT_REPORT_ASSIGNEE_REQUIRED);
	}

	@Test
	@DisplayName("탈퇴한 관리자는 콘텐츠 신고 알림 대상에서 제외한다")
	void reportPostPublishesOnlyActiveAdminNotificationEvent() {
		Member author = saveMember("active-admin-author");
		Member reporter = saveMember("active-admin-reporter");
		Member activeAdmin = saveAdmin("active-admin");
		Member withdrawnAdmin = saveAdmin("withdrawn-admin");
		withdrawnAdmin.withdraw("withdrawn-admin@example.com", "withdrawn:admin");
		memberRepository.saveAndFlush(withdrawnAdmin);
		PostResult post = postService.create(author.getId(), new PostCreateCommand("글", "본문"));
		clearInvocations(outboxEventPublisher);

		ContentReportResult report = contentReportService.reportPost(post.id(), reporter.getId(), "확인 필요");

		verify(outboxEventPublisher).publishContentReported(report.id(), reporter.getId(), List.of(activeAdmin.getId()));
		assertThat(withdrawnAdmin.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
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
}

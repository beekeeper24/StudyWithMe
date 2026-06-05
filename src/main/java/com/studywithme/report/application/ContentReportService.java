package com.studywithme.report.application;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.comment.exception.CommentErrorCode;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.notification.application.NotificationService;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostStatus;
import com.studywithme.post.exception.PostErrorCode;
import com.studywithme.post.repository.PostRepository;
import com.studywithme.report.domain.ContentReport;
import com.studywithme.report.domain.ContentReportModerationAction;
import com.studywithme.report.domain.ContentReportStatus;
import com.studywithme.report.domain.ContentReportTargetType;
import com.studywithme.report.exception.ContentReportErrorCode;
import com.studywithme.report.repository.ContentReportRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentReportService {

	private final ContentReportRepository contentReportRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;
	private final OutboxEventPublisher outboxEventPublisher;
	private final NotificationService notificationService;

	public ContentReportService(
		ContentReportRepository contentReportRepository,
		PostRepository postRepository,
		CommentRepository commentRepository,
		MemberRepository memberRepository,
		OutboxEventPublisher outboxEventPublisher,
		NotificationService notificationService
	) {
		this.contentReportRepository = contentReportRepository;
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
		this.memberRepository = memberRepository;
		this.outboxEventPublisher = outboxEventPublisher;
		this.notificationService = notificationService;
	}

	@Transactional
	public ContentReportResult reportPost(Long postId, Long reporterMemberId, String reason) {
		Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
		if (post.getAuthorMemberId().equals(reporterMemberId)) {
			throw new BusinessException(ContentReportErrorCode.CANNOT_REPORT_OWN_CONTENT);
		}
		ensureNotDuplicated(ContentReportTargetType.POST, post.getId(), reporterMemberId);
		ContentReport report = contentReportRepository.save(ContentReport.create(
			ContentReportTargetType.POST,
			post.getId(),
			post.getId(),
			post.getTitle(),
			post.getContent(),
			reporterMemberId,
			post.getAuthorMemberId(),
			reason
		));
		outboxEventPublisher.publishContentReported(report.getId(), reporterMemberId, findActiveAdminIds());
		return toResult(report);
	}

	@Transactional
	public ContentReportResult reportComment(Long commentId, Long reporterMemberId, String reason) {
		Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
		postRepository.findByIdAndStatus(comment.getPostId(), PostStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
		if (comment.getAuthorMemberId().equals(reporterMemberId)) {
			throw new BusinessException(ContentReportErrorCode.CANNOT_REPORT_OWN_CONTENT);
		}
		ensureNotDuplicated(ContentReportTargetType.COMMENT, comment.getId(), reporterMemberId);
		ContentReport report = contentReportRepository.save(ContentReport.create(
			ContentReportTargetType.COMMENT,
			comment.getId(),
			comment.getPostId(),
			null,
			comment.getContent(),
			reporterMemberId,
			comment.getAuthorMemberId(),
			reason
		));
		outboxEventPublisher.publishContentReported(report.getId(), reporterMemberId, findActiveAdminIds());
		return toResult(report);
	}

	public List<ContentReportResult> findReports(Long requesterMemberId, ContentReportStatus status) {
		ensureAdmin(requesterMemberId);
		List<ContentReport> reports = status == null
			? contentReportRepository.findAllByOrderByCreatedAtDescIdDesc()
			: contentReportRepository.findAllByStatusOrderByCreatedAtDescIdDesc(status);
		return toResults(reports);
	}

	@Transactional
	public ContentReportResult assignReport(Long reportId, Long requesterMemberId) {
		ensureAdmin(requesterMemberId);
		ContentReport report = findReport(reportId);
		if (report.getStatus() != ContentReportStatus.PENDING) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ALREADY_HANDLED);
		}
		if (report.getAssignedAdminMemberId() != null
			&& !report.getAssignedAdminMemberId().equals(requesterMemberId)) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ALREADY_ASSIGNED);
		}
		try {
			report.assignTo(requesterMemberId);
			contentReportRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ALREADY_ASSIGNED);
		}
		notificationService.markContentReportNotificationsRead(report.getId());
		return toResult(report);
	}

	@Transactional
	public ContentReportResult handleReport(
		Long reportId,
		Long requesterMemberId,
		ContentReportStatus nextStatus,
		ContentReportModerationAction moderationAction,
		String handlingNote
	) {
		ensureAdmin(requesterMemberId);
		if (nextStatus == ContentReportStatus.PENDING) {
			throw new BusinessException(ContentReportErrorCode.INVALID_CONTENT_REPORT_STATUS);
		}
		ContentReportModerationAction normalizedAction = normalizeModerationAction(moderationAction);
		if (nextStatus != ContentReportStatus.RESOLVED && normalizedAction != ContentReportModerationAction.NONE) {
			throw new BusinessException(ContentReportErrorCode.INVALID_CONTENT_REPORT_MODERATION_ACTION);
		}
		ContentReport report = findReport(reportId);
		if (report.getStatus() != ContentReportStatus.PENDING) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ALREADY_HANDLED);
		}
		if (!requesterMemberId.equals(report.getAssignedAdminMemberId())) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ASSIGNEE_REQUIRED);
		}
		try {
			if (normalizedAction == ContentReportModerationAction.DELETE_TARGET) {
				deleteReportedTarget(report);
			}
			report.handle(requesterMemberId, nextStatus, normalizedAction, handlingNote);
			contentReportRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ALREADY_HANDLED);
		}
		return toResult(report);
	}

	public ContentReportResult handleReport(
		Long reportId,
		Long requesterMemberId,
		ContentReportStatus nextStatus,
		String handlingNote
	) {
		return handleReport(reportId, requesterMemberId, nextStatus, ContentReportModerationAction.NONE, handlingNote);
	}

	private ContentReportModerationAction normalizeModerationAction(ContentReportModerationAction moderationAction) {
		return moderationAction == null ? ContentReportModerationAction.NONE : moderationAction;
	}

	private void deleteReportedTarget(ContentReport report) {
		if (report.getTargetType() == ContentReportTargetType.POST) {
			postRepository.findById(report.getTargetId()).ifPresent(Post::delete);
			return;
		}
		commentRepository.findById(report.getTargetId()).ifPresent(Comment::delete);
	}

	private ContentReport findReport(Long reportId) {
		return contentReportRepository.findById(reportId)
			.orElseThrow(() -> new BusinessException(ContentReportErrorCode.CONTENT_REPORT_NOT_FOUND));
	}

	private void ensureNotDuplicated(ContentReportTargetType targetType, Long targetId, Long reporterMemberId) {
		if (contentReportRepository.existsByTargetTypeAndTargetIdAndReporterMemberId(
			targetType,
			targetId,
			reporterMemberId
		)) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_DUPLICATED);
		}
	}

	private void ensureAdmin(Long requesterMemberId) {
		boolean isAdmin = memberRepository.findById(requesterMemberId)
			.map(member -> member.getRoles().contains(MemberRole.ADMIN))
			.orElse(false);
		if (!isAdmin) {
			throw new BusinessException(ContentReportErrorCode.CONTENT_REPORT_ADMIN_REQUIRED);
		}
	}

	private List<Long> findActiveAdminIds() {
		return memberRepository.findAllByRoleAndStatus(MemberRole.ADMIN, MemberStatus.ACTIVE)
			.stream()
			.map(Member::getId)
			.toList();
	}

	private ContentReportResult toResult(ContentReport report) {
		return ContentReportResult.from(report, findReportMembers(List.of(report)));
	}

	private List<ContentReportResult> toResults(List<ContentReport> reports) {
		Map<Long, Member> members = findReportMembers(reports);
		return reports.stream()
			.map(report -> ContentReportResult.from(report, members))
			.toList();
	}

	private Map<Long, Member> findReportMembers(List<ContentReport> reports) {
		Set<Long> memberIds = new LinkedHashSet<>();
		for (ContentReport report : reports) {
			memberIds.add(report.getReporterMemberId());
			memberIds.add(report.getReportedMemberId());
			if (report.getAssignedAdminMemberId() != null) {
				memberIds.add(report.getAssignedAdminMemberId());
			}
			if (report.getHandlerMemberId() != null) {
				memberIds.add(report.getHandlerMemberId());
			}
		}
		return memberRepository.findAllById(memberIds)
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
	}
}

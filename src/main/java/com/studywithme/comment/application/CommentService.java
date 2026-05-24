package com.studywithme.comment.application;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.comment.exception.CommentErrorCode;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.mention.application.MentionExtractor;
import com.studywithme.mention.application.MentionTargetResolver;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostStatus;
import com.studywithme.post.exception.PostErrorCode;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final OutboxEventPublisher outboxEventPublisher;
	private final MentionExtractor mentionExtractor;
	private final MentionTargetResolver mentionTargetResolver;
	private final MemberRepository memberRepository;

	public CommentService(
		CommentRepository commentRepository,
		PostRepository postRepository,
		OutboxEventPublisher outboxEventPublisher,
		MentionExtractor mentionExtractor,
		MentionTargetResolver mentionTargetResolver,
		MemberRepository memberRepository
	) {
		this.commentRepository = commentRepository;
		this.postRepository = postRepository;
		this.outboxEventPublisher = outboxEventPublisher;
		this.mentionExtractor = mentionExtractor;
		this.mentionTargetResolver = mentionTargetResolver;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public CommentResult create(Long postId, Long requesterMemberId, CommentCreateCommand command) {
		Post post = getPublishedPost(postId);
		Comment comment = commentRepository.save(Comment.create(
			postId,
			requesterMemberId,
			null,
			command.content()
		));
		outboxEventPublisher.publishCommentCreated(
			postId,
			comment.getId(),
			post.getAuthorMemberId(),
			requesterMemberId
		);
		publishMentionedEvent(
			postId,
			comment.getId(),
			requesterMemberId,
			command.content(),
			List.of(post.getAuthorMemberId())
		);
		return toResult(comment, requesterMemberId);
	}

	@Transactional
	public CommentResult reply(Long parentCommentId, Long requesterMemberId, CommentCreateCommand command) {
		Comment parent = getPublishedComment(parentCommentId);
		if (parent.isReply()) {
			throw new BusinessException(CommentErrorCode.NESTED_REPLY_NOT_ALLOWED);
		}
		ensurePublishedPost(parent.getPostId());
		Comment reply = commentRepository.save(Comment.create(
			parent.getPostId(),
			requesterMemberId,
			parent.getId(),
			command.content()
		));
		outboxEventPublisher.publishReplyCreated(
			parent.getPostId(),
			parent.getId(),
			reply.getId(),
			parent.getAuthorMemberId(),
			requesterMemberId
		);
		publishMentionedEvent(
			parent.getPostId(),
			reply.getId(),
			requesterMemberId,
			command.content(),
			List.of(parent.getAuthorMemberId())
		);
		return toResult(reply, requesterMemberId);
	}

	@Transactional(readOnly = true)
	public List<CommentResult> findAllByPostId(Long postId) {
		return findAllByPostId(postId, null);
	}

	@Transactional(readOnly = true)
	public List<CommentResult> findAllByPostId(Long postId, Long requesterMemberId) {
		ensurePublishedPost(postId);
		List<Comment> publishedComments = commentRepository.findAllByPostIdAndStatusOrderByCreatedAtAsc(
			postId,
			CommentStatus.PUBLISHED
		);
		Map<Long, Member> authors = findAuthors(publishedComments);
		Set<Long> visibleCommentIds = publishedComments.stream()
			.map(Comment::getId)
			.collect(Collectors.toSet());

		return publishedComments.stream()
			.filter(comment -> comment.getParentCommentId() == null || visibleCommentIds.contains(comment.getParentCommentId()))
			.map(comment -> toResult(comment, authors.get(comment.getAuthorMemberId()), requesterMemberId))
			.toList();
	}

	@Transactional
	public CommentResult update(Long commentId, Long requesterMemberId, CommentUpdateCommand command) {
		Comment comment = getPublishedComment(commentId);
		comment.update(requesterMemberId, command.content());
		return toResult(comment, requesterMemberId);
	}

	@Transactional
	public CommentResult delete(Long commentId, Long requesterMemberId) {
		Comment comment = getPublishedComment(commentId);
		comment.delete(requesterMemberId);
		return toResult(comment, requesterMemberId);
	}

	private Post getPublishedPost(Long postId) {
		return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
	}

	private void ensurePublishedPost(Long postId) {
		getPublishedPost(postId);
	}

	private Comment getPublishedComment(Long commentId) {
		return commentRepository.findByIdAndStatus(commentId, CommentStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
	}

	private void publishMentionedEvent(
		Long postId,
		Long commentId,
		Long actorMemberId,
		String content,
		List<Long> ordinaryNotificationReceiverMemberIds
	) {
		List<Long> mentionedMemberIds = mentionTargetResolver.resolve(mentionExtractor.extract(content))
			.stream()
			.map(Member::getId)
			.filter(memberId -> !memberId.equals(actorMemberId))
			.toList();

		List<Long> replacedReceiverMemberIds = ordinaryNotificationReceiverMemberIds.stream()
			.filter(mentionedMemberIds::contains)
			.toList();

		outboxEventPublisher.publishCommentMentioned(
			postId,
			commentId,
			actorMemberId,
			mentionedMemberIds,
			replacedReceiverMemberIds
		);
	}

	private Map<Long, Member> findAuthors(List<Comment> comments) {
		List<Long> authorIds = comments.stream()
			.map(Comment::getAuthorMemberId)
			.distinct()
			.toList();
		return memberRepository.findAllById(authorIds)
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
	}

	private CommentResult toResult(Comment comment, Long requesterMemberId) {
		Member author = memberRepository.findById(comment.getAuthorMemberId()).orElse(null);
		return toResult(comment, author, requesterMemberId);
	}

	private CommentResult toResult(Comment comment, Member author, Long requesterMemberId) {
		return CommentResult.from(comment, author, requesterMemberId);
	}
}

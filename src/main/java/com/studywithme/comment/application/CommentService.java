package com.studywithme.comment.application;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.comment.exception.CommentErrorCode;
import com.studywithme.comment.repository.CommentRepository;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.outbox.application.OutboxEventPublisher;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostStatus;
import com.studywithme.post.exception.PostErrorCode;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final OutboxEventPublisher outboxEventPublisher;

	public CommentService(
		CommentRepository commentRepository,
		PostRepository postRepository,
		OutboxEventPublisher outboxEventPublisher
	) {
		this.commentRepository = commentRepository;
		this.postRepository = postRepository;
		this.outboxEventPublisher = outboxEventPublisher;
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
		return CommentResult.from(comment);
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
		return CommentResult.from(reply);
	}

	@Transactional(readOnly = true)
	public List<CommentResult> findAllByPostId(Long postId) {
		ensurePublishedPost(postId);
		List<Comment> publishedComments = commentRepository.findAllByPostIdAndStatusOrderByCreatedAtAsc(
			postId,
			CommentStatus.PUBLISHED
		);
		Set<Long> visibleCommentIds = publishedComments.stream()
			.map(Comment::getId)
			.collect(Collectors.toSet());

		return publishedComments.stream()
			.filter(comment -> comment.getParentCommentId() == null || visibleCommentIds.contains(comment.getParentCommentId()))
			.map(CommentResult::from)
			.toList();
	}

	@Transactional
	public CommentResult update(Long commentId, Long requesterMemberId, CommentUpdateCommand command) {
		Comment comment = getPublishedComment(commentId);
		comment.update(requesterMemberId, command.content());
		return CommentResult.from(comment);
	}

	@Transactional
	public CommentResult delete(Long commentId, Long requesterMemberId) {
		Comment comment = getPublishedComment(commentId);
		comment.delete(requesterMemberId);
		return CommentResult.from(comment);
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
}

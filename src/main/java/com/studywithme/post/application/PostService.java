package com.studywithme.post.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostBoardType;
import com.studywithme.post.domain.PostStatus;
import com.studywithme.post.exception.PostErrorCode;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final int POST_LIST_LIMIT = 50;

	private final PostRepository postRepository;
	private final MemberRepository memberRepository;

	public PostService(PostRepository postRepository, MemberRepository memberRepository) {
		this.postRepository = postRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public PostResult create(Long requesterMemberId, PostCreateCommand command) {
		Post post = postRepository.save(Post.create(
			command.boardType(),
			command.title(),
			command.content(),
			requesterMemberId
		));
		return toResult(post, requesterMemberId);
	}

	@Transactional(readOnly = true)
	public List<PostResult> findAll() {
		return findAll(null);
	}

	@Transactional(readOnly = true)
	public List<PostResult> findAll(Long requesterMemberId) {
		return findAll(null, requesterMemberId);
	}

	@Transactional(readOnly = true)
	public List<PostResult> findAll(PostBoardType boardType, Long requesterMemberId) {
		PageRequest pageable = PageRequest.of(0, POST_LIST_LIMIT);
		List<Post> posts = boardType == null
			? postRepository.findAllByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED, pageable)
			: postRepository.findAllByBoardTypeAndStatusOrderByCreatedAtDesc(boardType, PostStatus.PUBLISHED, pageable);
		Map<Long, Member> authors = findAuthors(posts);
		return posts.stream()
			.map(post -> toResult(post, authors.get(post.getAuthorMemberId()), requesterMemberId))
			.toList();
	}

	@Transactional(readOnly = true)
	public PostResult findById(Long postId) {
		return findById(postId, null);
	}

	@Transactional(readOnly = true)
	public PostResult findById(Long postId, Long requesterMemberId) {
		return toResult(getPublishedPost(postId), requesterMemberId);
	}

	@Transactional
	public PostResult update(Long postId, Long requesterMemberId, PostUpdateCommand command) {
		Post post = getPublishedPost(postId);
		post.update(requesterMemberId, command.title(), command.content());
		return toResult(post, requesterMemberId);
	}

	@Transactional
	public PostResult delete(Long postId, Long requesterMemberId) {
		Post post = getPublishedPost(postId);
		post.delete(requesterMemberId);
		return toResult(post, requesterMemberId);
	}

	private Post getPublishedPost(Long postId) {
		return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
	}

	private Map<Long, Member> findAuthors(List<Post> posts) {
		List<Long> authorIds = posts.stream()
			.map(Post::getAuthorMemberId)
			.distinct()
			.toList();
		return memberRepository.findAllById(authorIds)
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
	}

	private PostResult toResult(Post post, Long requesterMemberId) {
		Member author = memberRepository.findById(post.getAuthorMemberId()).orElse(null);
		return toResult(post, author, requesterMemberId);
	}

	private PostResult toResult(Post post, Member author, Long requesterMemberId) {
		return PostResult.from(post, author, requesterMemberId);
	}
}

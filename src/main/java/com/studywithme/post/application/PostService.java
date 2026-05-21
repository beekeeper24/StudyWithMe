package com.studywithme.post.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostStatus;
import com.studywithme.post.exception.PostErrorCode;
import com.studywithme.post.repository.PostRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final int POST_LIST_LIMIT = 50;

	private final PostRepository postRepository;

	public PostService(PostRepository postRepository) {
		this.postRepository = postRepository;
	}

	@Transactional
	public PostResult create(Long requesterMemberId, PostCreateCommand command) {
		Post post = postRepository.save(Post.create(
			command.title(),
			command.content(),
			requesterMemberId
		));
		return PostResult.from(post);
	}

	@Transactional(readOnly = true)
	public List<PostResult> findAll() {
		return postRepository.findAllByStatusOrderByCreatedAtDesc(
				PostStatus.PUBLISHED,
				PageRequest.of(0, POST_LIST_LIMIT)
			)
			.stream()
			.map(PostResult::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public PostResult findById(Long postId) {
		return PostResult.from(getPublishedPost(postId));
	}

	@Transactional
	public PostResult update(Long postId, Long requesterMemberId, PostUpdateCommand command) {
		Post post = getPublishedPost(postId);
		post.update(requesterMemberId, command.title(), command.content());
		return PostResult.from(post);
	}

	@Transactional
	public PostResult delete(Long postId, Long requesterMemberId) {
		Post post = getPublishedPost(postId);
		post.delete(requesterMemberId);
		return PostResult.from(post);
	}

	private Post getPublishedPost(Long postId) {
		return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
	}
}

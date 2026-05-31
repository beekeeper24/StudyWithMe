package com.studywithme.post.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final int POST_LIST_LIMIT = 50;
	private static final int POST_LIST_DEFAULT_PAGE = 0;

	private final PostRepository postRepository;
	private final MemberRepository memberRepository;

	public PostService(PostRepository postRepository, MemberRepository memberRepository) {
		this.postRepository = postRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public PostResult create(Long requesterMemberId, PostCreateCommand command) {
		ensureCanCreate(command.boardType(), requesterMemberId);
		Post post = postRepository.save(Post.create(
			command.boardType(),
			command.title(),
			command.content(),
			requesterMemberId
		));
		return toResult(post, requesterMemberId);
	}

	private void ensureCanCreate(PostBoardType boardType, Long requesterMemberId) {
		if (boardType != PostBoardType.NOTICE) {
			return;
		}
		ensureAdmin(requesterMemberId);
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
		return findAll(boardType, requesterMemberId, POST_LIST_DEFAULT_PAGE, POST_LIST_LIMIT);
	}

	@Transactional(readOnly = true)
	public List<PostResult> findAll(PostBoardType boardType, Long requesterMemberId, int page, int size) {
		return findPage(boardType, requesterMemberId, page, size).content();
	}

	@Transactional(readOnly = true)
	public PostPageResult findPage(PostBoardType boardType, Long requesterMemberId, int page, int size) {
		return findPage(boardType, null, requesterMemberId, page, size);
	}

	@Transactional(readOnly = true)
	public PostPageResult findPage(
		PostBoardType boardType,
		String keyword,
		Long requesterMemberId,
		int page,
		int size
	) {
		PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
		Page<Post> postPage = postRepository.searchPublishedPosts(
			boardType,
			PostStatus.PUBLISHED,
			normalizeKeyword(keyword),
			pageable
		);
		List<Post> posts = postPage.getContent();
		Map<Long, Member> authors = findAuthors(posts);
		List<PostResult> content = posts.stream()
			.map(post -> toResult(post, authors.get(post.getAuthorMemberId()), requesterMemberId))
			.toList();
		return new PostPageResult(
			content,
			postPage.getNumber(),
			postPage.getSize(),
			postPage.getTotalElements(),
			postPage.getTotalPages(),
			postPage.hasNext(),
			postPage.hasPrevious()
		);
	}

	private int normalizePage(int page) {
		return Math.max(page, POST_LIST_DEFAULT_PAGE);
	}

	private int normalizeSize(int size) {
		if (size <= 0) {
			return POST_LIST_LIMIT;
		}
		return Math.min(size, POST_LIST_LIMIT);
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword.trim();
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
		if (post.getBoardType() == PostBoardType.NOTICE) {
			ensureAdmin(requesterMemberId);
			post.updateContent(command.title(), command.content());
		} else {
			post.update(requesterMemberId, command.title(), command.content());
		}
		return toResult(post, requesterMemberId);
	}

	@Transactional
	public PostResult delete(Long postId, Long requesterMemberId) {
		Post post = getPublishedPost(postId);
		if (post.getBoardType() == PostBoardType.NOTICE) {
			ensureAdmin(requesterMemberId);
			post.delete();
		} else {
			post.delete(requesterMemberId);
		}
		return toResult(post, requesterMemberId);
	}

	private void ensureAdmin(Long requesterMemberId) {
		boolean isAdmin = memberRepository.findById(requesterMemberId)
			.map(member -> member.getRoles().contains(MemberRole.ADMIN))
			.orElse(false);
		if (!isAdmin) {
			throw new BusinessException(PostErrorCode.NOTICE_ADMIN_REQUIRED);
		}
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

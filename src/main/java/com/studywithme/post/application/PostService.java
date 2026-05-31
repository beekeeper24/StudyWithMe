package com.studywithme.post.application;

import com.studywithme.comment.domain.CommentStatus;
import com.studywithme.comment.repository.CommentCountView;
import com.studywithme.comment.repository.CommentRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final int POST_LIST_LIMIT = 50;
	private static final int POST_LIST_DEFAULT_PAGE = 0;

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;

	public PostService(
		PostRepository postRepository,
		CommentRepository commentRepository,
		MemberRepository memberRepository
	) {
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
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
		return findPage(boardType, null, PostSearchScope.ALL, PostSortOrder.LATEST, requesterMemberId, page, size);
	}

	@Transactional(readOnly = true)
	public PostPageResult findPage(
		PostBoardType boardType,
		String keyword,
		PostSearchScope searchScope,
		PostSortOrder sortOrder,
		Long requesterMemberId,
		int page,
		int size
	) {
		PostSortOrder normalizedSortOrder = normalizeSortOrder(sortOrder);
		PageRequest pageable = PageRequest.of(
			normalizePage(page),
			normalizeSize(size),
			Sort.by(normalizedSortOrder.direction(), "createdAt")
				.and(Sort.by(normalizedSortOrder.direction(), "id"))
		);
		String normalizedKeyword = normalizeKeyword(keyword);
		PostSearchScope normalizedSearchScope = normalizeSearchScope(searchScope);
		Page<Post> postPage = normalizedKeyword == null
			? findPublishedPosts(boardType, pageable)
			: postRepository.searchPublishedPosts(
				boardType,
				PostStatus.PUBLISHED,
				normalizedKeyword,
				normalizedSearchScope.includesTitle(),
				normalizedSearchScope.includesContent(),
				normalizedSearchScope.includesAuthor(),
				pageable
		);
		List<Post> posts = postPage.getContent();
		Map<Long, Member> authors = findAuthors(posts);
		Map<Long, Long> commentCounts = findVisibleCommentCounts(posts);
		List<PostResult> content = posts.stream()
			.map(post -> toResult(
				post,
				authors.get(post.getAuthorMemberId()),
				requesterMemberId,
				commentCounts.getOrDefault(post.getId(), 0L)
			))
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

	private Page<Post> findPublishedPosts(PostBoardType boardType, PageRequest pageable) {
		if (boardType == null) {
			return postRepository.findAllByStatus(PostStatus.PUBLISHED, pageable);
		}
		return postRepository.findAllByBoardTypeAndStatus(
			boardType,
			PostStatus.PUBLISHED,
			pageable
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

	private PostSearchScope normalizeSearchScope(PostSearchScope searchScope) {
		return searchScope == null ? PostSearchScope.ALL : searchScope;
	}

	private PostSortOrder normalizeSortOrder(PostSortOrder sortOrder) {
		return sortOrder == null ? PostSortOrder.LATEST : sortOrder;
	}

	@Transactional(readOnly = true)
	public PostResult findById(Long postId) {
		return findById(postId, null);
	}

	@Transactional(readOnly = true)
	public PostResult findById(Long postId, Long requesterMemberId) {
		Post post = getPublishedPost(postId);
		Member author = memberRepository.findById(post.getAuthorMemberId()).orElse(null);
		return toResult(post, author, requesterMemberId, findVisibleCommentCount(post.getId()));
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

	private Map<Long, Long> findVisibleCommentCounts(List<Post> posts) {
		List<Long> postIds = posts.stream()
			.map(Post::getId)
			.toList();
		if (postIds.isEmpty()) {
			return Map.of();
		}
		return commentRepository.countVisibleCommentsByPostIds(postIds, CommentStatus.PUBLISHED)
			.stream()
			.collect(Collectors.toMap(CommentCountView::getPostId, CommentCountView::getCommentCount));
	}

	private long findVisibleCommentCount(Long postId) {
		return commentRepository.countVisibleCommentsByPostIds(List.of(postId), CommentStatus.PUBLISHED)
			.stream()
			.findFirst()
			.map(CommentCountView::getCommentCount)
			.orElse(0L);
	}

	private PostResult toResult(Post post, Long requesterMemberId) {
		Member author = memberRepository.findById(post.getAuthorMemberId()).orElse(null);
		return toResult(post, author, requesterMemberId, findVisibleCommentCount(post.getId()));
	}

	private PostResult toResult(Post post, Member author, Long requesterMemberId, long commentCount) {
		return PostResult.from(post, author, requesterMemberId, commentCount);
	}
}

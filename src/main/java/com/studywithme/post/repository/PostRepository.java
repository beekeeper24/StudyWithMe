package com.studywithme.post.repository;

import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostBoardType;
import com.studywithme.post.domain.PostStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findAllByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

	Page<Post> findAllByBoardTypeAndStatusOrderByCreatedAtDesc(
		PostBoardType boardType,
		PostStatus status,
		Pageable pageable
	);

	@Query("""
		SELECT p
		FROM Post p
		WHERE p.status = :status
			AND (:boardType IS NULL OR p.boardType = :boardType)
			AND (
				:keyword IS NULL
				OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
			)
		ORDER BY p.createdAt DESC
		""")
	Page<Post> searchPublishedPosts(
		@Param("boardType") PostBoardType boardType,
		@Param("status") PostStatus status,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	Optional<Post> findByIdAndStatus(Long id, PostStatus status);
}

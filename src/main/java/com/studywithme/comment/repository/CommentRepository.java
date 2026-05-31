package com.studywithme.comment.repository;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findAllByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommentStatus status);

	@Query("""
		SELECT c.postId AS postId, COUNT(c.id) AS commentCount
		FROM Comment c
		LEFT JOIN Comment parent ON parent.id = c.parentCommentId
		WHERE c.postId IN :postIds
			AND c.status = :status
			AND (c.parentCommentId IS NULL OR parent.status = :status)
		GROUP BY c.postId
		""")
	List<CommentCountView> countVisibleCommentsByPostIds(
		@Param("postIds") List<Long> postIds,
		@Param("status") CommentStatus status
	);

	Optional<Comment> findByIdAndStatus(Long id, CommentStatus status);
}

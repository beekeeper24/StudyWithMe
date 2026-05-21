package com.studywithme.comment.repository;

import com.studywithme.comment.domain.Comment;
import com.studywithme.comment.domain.CommentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findAllByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommentStatus status);

	Optional<Comment> findByIdAndStatus(Long id, CommentStatus status);
}

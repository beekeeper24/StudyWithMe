package com.studywithme.post.repository;

import com.studywithme.post.domain.Post;
import com.studywithme.post.domain.PostBoardType;
import com.studywithme.post.domain.PostStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

	List<Post> findAllByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

	List<Post> findAllByBoardTypeAndStatusOrderByCreatedAtDesc(
		PostBoardType boardType,
		PostStatus status,
		Pageable pageable
	);

	Optional<Post> findByIdAndStatus(Long id, PostStatus status);
}

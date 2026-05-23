package com.studywithme.study.repository;

import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyRepository extends JpaRepository<Study, Long> {

	List<Study> findAllByOrderByCreatedAtDesc(Pageable pageable);

	List<Study> findAllByStatusOrderByCreatedAtDesc(StudyStatus status, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Study s where s.id = :id")
	Optional<Study> findByIdForUpdate(@Param("id") Long id);
}

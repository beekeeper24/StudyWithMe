package com.studywithme.study.repository;

import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMemberStatus;
import com.studywithme.study.domain.StudyStatus;
import com.studywithme.member.domain.MemberStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyRepository extends JpaRepository<Study, Long> {

	List<Study> findAllByOrderByCreatedAtDesc(Pageable pageable);

	List<Study> findAllByStatusOrderByCreatedAtDesc(StudyStatus status, Pageable pageable);

	@Query("""
		SELECT s
		FROM Study s
		JOIN Member m ON m.id = s.ownerMemberId
		WHERE s.status = :status
			AND m.status = :ownerStatus
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	Page<Study> findVisibleStudiesByStatus(
		@Param("status") StudyStatus status,
		@Param("ownerStatus") MemberStatus ownerStatus,
		Pageable pageable
	);

	@Query("""
		SELECT s
		FROM Study s
		JOIN Member m ON m.id = s.ownerMemberId
		WHERE s.status = :status
			AND m.status = :ownerStatus
			AND (
				LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR (s.progressMethod IS NOT NULL AND LOWER(s.progressMethod) LIKE LOWER(CONCAT('%', :keyword, '%')))
				OR (s.targetAudience IS NOT NULL AND LOWER(s.targetAudience) LIKE LOWER(CONCAT('%', :keyword, '%')))
				OR (s.schedule IS NOT NULL AND LOWER(s.schedule) LIKE LOWER(CONCAT('%', :keyword, '%')))
			)
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	Page<Study> searchVisibleStudiesByStatus(
		@Param("status") StudyStatus status,
		@Param("ownerStatus") MemberStatus ownerStatus,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	@Query("""
		SELECT s
		FROM StudyMember sm
		JOIN Study s ON s.id = sm.studyId
		WHERE sm.memberId = :memberId
			AND sm.historyHiddenAt IS NULL
			AND sm.status = :memberStatus
			AND s.status NOT IN :excludedStatuses
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	Page<Study> findActiveMemberStudies(
		@Param("memberId") Long memberId,
		@Param("memberStatus") StudyMemberStatus memberStatus,
		@Param("excludedStatuses") Collection<StudyStatus> excludedStatuses,
		Pageable pageable
	);

	@Query("""
		SELECT s
		FROM StudyMember sm
		JOIN Study s ON s.id = sm.studyId
		WHERE sm.memberId = :memberId
			AND sm.historyHiddenAt IS NULL
			AND sm.status = :memberStatus
			AND s.status NOT IN :excludedStatuses
			AND (
				LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR (s.progressMethod IS NOT NULL AND LOWER(s.progressMethod) LIKE LOWER(CONCAT('%', :keyword, '%')))
				OR (s.targetAudience IS NOT NULL AND LOWER(s.targetAudience) LIKE LOWER(CONCAT('%', :keyword, '%')))
				OR (s.schedule IS NOT NULL AND LOWER(s.schedule) LIKE LOWER(CONCAT('%', :keyword, '%')))
			)
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	Page<Study> searchActiveMemberStudies(
		@Param("memberId") Long memberId,
		@Param("memberStatus") StudyMemberStatus memberStatus,
		@Param("excludedStatuses") Collection<StudyStatus> excludedStatuses,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	@Query("""
		SELECT s
		FROM StudyMember sm
		JOIN Study s ON s.id = sm.studyId
		WHERE sm.memberId = :memberId
			AND sm.historyHiddenAt IS NULL
			AND (sm.status = :leftStatus OR s.status IN :pastStatuses)
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	Page<Study> findPastMemberStudies(
		@Param("memberId") Long memberId,
		@Param("leftStatus") StudyMemberStatus leftStatus,
		@Param("pastStatuses") Collection<StudyStatus> pastStatuses,
		Pageable pageable
	);

	@Query("""
		SELECT s
		FROM StudyMember sm
		JOIN Study s ON s.id = sm.studyId
		WHERE sm.memberId = :memberId
			AND sm.historyHiddenAt IS NULL
			AND (sm.status = :leftStatus OR s.status IN :pastStatuses)
			AND (
				LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR (s.progressMethod IS NOT NULL AND LOWER(s.progressMethod) LIKE LOWER(CONCAT('%', :keyword, '%')))
				OR (s.targetAudience IS NOT NULL AND LOWER(s.targetAudience) LIKE LOWER(CONCAT('%', :keyword, '%')))
				OR (s.schedule IS NOT NULL AND LOWER(s.schedule) LIKE LOWER(CONCAT('%', :keyword, '%')))
			)
		ORDER BY s.createdAt DESC, s.id DESC
		""")
	Page<Study> searchPastMemberStudies(
		@Param("memberId") Long memberId,
		@Param("leftStatus") StudyMemberStatus leftStatus,
		@Param("pastStatuses") Collection<StudyStatus> pastStatuses,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	List<Study> findAllByOwnerMemberIdAndStatusIn(Long ownerMemberId, List<StudyStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Study s where s.id = :id")
	Optional<Study> findByIdForUpdate(@Param("id") Long id);
}

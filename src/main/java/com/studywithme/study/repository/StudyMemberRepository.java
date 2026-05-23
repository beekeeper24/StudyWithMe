package com.studywithme.study.repository;

import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

	boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

	boolean existsByStudyIdAndMemberIdAndStatus(Long studyId, Long memberId, StudyMemberStatus status);

	Optional<StudyMember> findByStudyIdAndMemberId(Long studyId, Long memberId);

	Optional<StudyMember> findByStudyIdAndMemberIdAndStatus(
		Long studyId,
		Long memberId,
		StudyMemberStatus status
	);

	List<StudyMember> findAllByStudyId(Long studyId);

	List<StudyMember> findAllByStudyIdAndStatus(Long studyId, StudyMemberStatus status);

	List<StudyMember> findAllByMemberId(Long memberId);

	void deleteByStudyIdAndMemberId(Long studyId, Long memberId);
}

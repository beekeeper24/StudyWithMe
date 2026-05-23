package com.studywithme.study.repository;

import com.studywithme.study.domain.StudyMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

	boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

	Optional<StudyMember> findByStudyIdAndMemberId(Long studyId, Long memberId);

	List<StudyMember> findAllByStudyId(Long studyId);

	List<StudyMember> findAllByMemberId(Long memberId);

	void deleteByStudyIdAndMemberId(Long studyId, Long memberId);
}

package com.studywithme.study.repository;

import com.studywithme.study.domain.StudyMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

	boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

	Optional<StudyMember> findByStudyIdAndMemberId(Long studyId, Long memberId);

	void deleteByStudyIdAndMemberId(Long studyId, Long memberId);
}

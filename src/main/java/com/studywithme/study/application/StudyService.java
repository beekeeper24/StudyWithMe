package com.studywithme.study.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyStatus;
import com.studywithme.study.exception.StudyErrorCode;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyService {

	private static final int STUDY_LIST_LIMIT = 50;

	private final StudyRepository studyRepository;
	private final StudyMemberRepository studyMemberRepository;

	public StudyService(
		StudyRepository studyRepository,
		StudyMemberRepository studyMemberRepository
	) {
		this.studyRepository = studyRepository;
		this.studyMemberRepository = studyMemberRepository;
	}

	@Transactional
	public StudyResult create(Long requesterMemberId, StudyCreateCommand command) {
		Study study = studyRepository.save(Study.create(
			command.title(),
			command.description(),
			requesterMemberId
		));
		studyMemberRepository.save(StudyMember.owner(study.getId(), requesterMemberId));
		return StudyResult.from(study);
	}

	@Transactional(readOnly = true)
	public List<StudyResult> findAll() {
		return studyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, STUDY_LIST_LIMIT))
			.stream()
			.map(StudyResult::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public StudyResult findById(Long studyId) {
		return StudyResult.from(getStudy(studyId));
	}

	@Transactional
	public StudyResult join(Long studyId, Long requesterMemberId) {
		Study study = getStudyForUpdate(studyId);
		if (study.getStatus() == StudyStatus.CLOSED) {
			throw new BusinessException(StudyErrorCode.STUDY_ALREADY_CLOSED);
		}
		if (studyMemberRepository.existsByStudyIdAndMemberId(studyId, requesterMemberId)) {
			throw new BusinessException(StudyErrorCode.ALREADY_JOINED);
		}

		studyMemberRepository.save(StudyMember.member(studyId, requesterMemberId));
		return StudyResult.from(study);
	}

	@Transactional
	public StudyResult leave(Long studyId, Long requesterMemberId) {
		Study study = getStudyForUpdate(studyId);
		if (study.getOwnerMemberId().equals(requesterMemberId)) {
			throw new BusinessException(StudyErrorCode.OWNER_CANNOT_LEAVE);
		}

		StudyMember studyMember = studyMemberRepository.findByStudyIdAndMemberId(studyId, requesterMemberId)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.NOT_STUDY_MEMBER));
		studyMemberRepository.delete(studyMember);
		return StudyResult.from(study);
	}

	@Transactional
	public StudyResult close(Long studyId, Long requesterMemberId) {
		Study study = getStudyForUpdate(studyId);
		study.close(requesterMemberId);
		return StudyResult.from(study);
	}

	private Study getStudy(Long studyId) {
		return studyRepository.findById(studyId)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
	}

	private Study getStudyForUpdate(Long studyId) {
		return studyRepository.findByIdForUpdate(studyId)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
	}
}

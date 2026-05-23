package com.studywithme.study.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyStatus;
import com.studywithme.study.exception.StudyErrorCode;
import com.studywithme.study.repository.StudyMemberRepository;
import com.studywithme.study.repository.StudyRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyService {

	private static final int STUDY_LIST_LIMIT = 50;

	private final StudyRepository studyRepository;
	private final StudyMemberRepository studyMemberRepository;
	private final MemberRepository memberRepository;

	public StudyService(
		StudyRepository studyRepository,
		StudyMemberRepository studyMemberRepository,
		MemberRepository memberRepository
	) {
		this.studyRepository = studyRepository;
		this.studyMemberRepository = studyMemberRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public StudyResult create(Long requesterMemberId, StudyCreateCommand command) {
		Study study = studyRepository.save(Study.create(
			command.title(),
			command.description(),
			requesterMemberId
		));
		studyMemberRepository.save(StudyMember.owner(study.getId(), requesterMemberId));
		return toResult(study, requesterMemberId);
	}

	@Transactional(readOnly = true)
	public List<StudyResult> findAll() {
		return findAll(null);
	}

	@Transactional(readOnly = true)
	public List<StudyResult> findAll(Long requesterMemberId) {
		List<Study> studies = studyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, STUDY_LIST_LIMIT));
		Map<Long, Member> owners = findOwners(studies);
		Set<Long> joinedStudyIds = findJoinedStudyIds(requesterMemberId);

		return studies
			.stream()
			.map(study -> toResult(
				study,
				owners.get(study.getOwnerMemberId()),
				requesterMemberId,
				joinedStudyIds.contains(study.getId())
			))
			.toList();
	}

	@Transactional(readOnly = true)
	public StudyResult findById(Long studyId) {
		return findById(studyId, null);
	}

	@Transactional(readOnly = true)
	public StudyResult findById(Long studyId, Long requesterMemberId) {
		Study study = getStudy(studyId);
		Member owner = memberRepository.findById(study.getOwnerMemberId()).orElse(null);
		boolean joinedByRequester = requesterMemberId != null
			&& studyMemberRepository.existsByStudyIdAndMemberId(studyId, requesterMemberId);
		return toResult(study, owner, requesterMemberId, joinedByRequester);
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
		return toResult(study, requesterMemberId);
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
		return toResult(study, requesterMemberId, false);
	}

	@Transactional
	public StudyResult close(Long studyId, Long requesterMemberId) {
		Study study = getStudyForUpdate(studyId);
		study.close(requesterMemberId);
		return toResult(study, requesterMemberId);
	}

	private Study getStudy(Long studyId) {
		return studyRepository.findById(studyId)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
	}

	private Study getStudyForUpdate(Long studyId) {
		return studyRepository.findByIdForUpdate(studyId)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
	}

	private Map<Long, Member> findOwners(List<Study> studies) {
		List<Long> ownerIds = studies.stream()
			.map(Study::getOwnerMemberId)
			.distinct()
			.toList();
		return memberRepository.findAllById(ownerIds)
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
	}

	private Set<Long> findJoinedStudyIds(Long requesterMemberId) {
		if (requesterMemberId == null) {
			return Set.of();
		}
		return studyMemberRepository.findAllByMemberId(requesterMemberId)
			.stream()
			.map(StudyMember::getStudyId)
			.collect(Collectors.toUnmodifiableSet());
	}

	private StudyResult toResult(Study study, Long requesterMemberId) {
		boolean joinedByRequester = requesterMemberId != null
			&& studyMemberRepository.existsByStudyIdAndMemberId(study.getId(), requesterMemberId);
		return toResult(study, requesterMemberId, joinedByRequester);
	}

	private StudyResult toResult(Study study, Long requesterMemberId, boolean joinedByRequester) {
		Member owner = memberRepository.findById(study.getOwnerMemberId()).orElse(null);
		return toResult(study, owner, requesterMemberId, joinedByRequester);
	}

	private StudyResult toResult(
		Study study,
		Member owner,
		Long requesterMemberId,
		boolean joinedByRequester
	) {
		String ownerNickname = owner == null ? null : owner.getNickname();
		String ownerProfileImageUrl = owner == null ? null : owner.getProfileImageUrl();
		return StudyResult.from(
			study,
			ownerNickname,
			ownerProfileImageUrl,
			joinedByRequester,
			requesterMemberId != null && study.getOwnerMemberId().equals(requesterMemberId)
		);
	}
}

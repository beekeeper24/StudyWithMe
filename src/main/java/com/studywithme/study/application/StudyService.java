package com.studywithme.study.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.study.domain.Study;
import com.studywithme.study.domain.StudyMember;
import com.studywithme.study.domain.StudyMemberStatus;
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
			descriptionFor(command),
			command.progressMethod(),
			command.targetAudience(),
			command.rules(),
			command.capacity(),
			command.schedule(),
			requesterMemberId
		));
		studyMemberRepository.save(StudyMember.owner(study.getId(), requesterMemberId));
		return toResult(study, requesterMemberId);
	}

	@Transactional
	public StudyResult update(Long studyId, Long requesterMemberId, StudyUpdateCommand command) {
		Study study = getStudyForUpdate(studyId);
		study.update(
			requesterMemberId,
			command.title(),
			command.progressMethod(),
			command.progressMethod(),
			command.targetAudience(),
			command.rules(),
			command.capacity(),
			command.schedule()
		);
		closeIfCapacityFull(study);
		return toResult(study, requesterMemberId);
	}

	@Transactional(readOnly = true)
	public List<StudyResult> findAll() {
		return findAll(null);
	}

	@Transactional(readOnly = true)
	public List<StudyResult> findAll(Long requesterMemberId) {
		List<Study> studies = studyRepository.findAllByStatusOrderByCreatedAtDesc(
			StudyStatus.RECRUITING,
			PageRequest.of(0, STUDY_LIST_LIMIT)
		);
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
			&& studyMemberRepository.existsByStudyIdAndMemberIdAndStatus(
				studyId,
				requesterMemberId,
				StudyMemberStatus.JOINED
			);
		return toResult(study, owner, requesterMemberId, joinedByRequester);
	}

	@Transactional(readOnly = true)
	public MyStudyHistoryResult findMyStudies(Long requesterMemberId) {
		List<StudyMember> memberships = studyMemberRepository.findAllByMemberId(requesterMemberId);
		List<Long> studyIds = memberships.stream()
			.map(StudyMember::getStudyId)
			.distinct()
			.toList();
		Map<Long, Study> studies = studyRepository.findAllById(studyIds)
			.stream()
			.collect(Collectors.toMap(Study::getId, Function.identity()));
		Map<Long, Member> owners = findOwners(studies.values().stream().toList());

		List<StudyResult> activeStudies = memberships.stream()
			.filter(StudyMember::isJoined)
			.map(StudyMember::getStudyId)
			.map(studies::get)
			.filter(study -> study != null && study.getStatus() == StudyStatus.RECRUITING)
			.map(study -> toResult(study, owners.get(study.getOwnerMemberId()), requesterMemberId, true))
			.toList();
		List<StudyResult> pastStudies = memberships.stream()
			.filter(membership -> !membership.isJoined()
				|| isClosedStudy(studies.get(membership.getStudyId())))
			.map(StudyMember::getStudyId)
			.map(studies::get)
			.filter(study -> study != null)
			.map(study -> toResult(
				study,
				owners.get(study.getOwnerMemberId()),
				requesterMemberId,
				studyMemberRepository.existsByStudyIdAndMemberIdAndStatus(
					study.getId(),
					requesterMemberId,
					StudyMemberStatus.JOINED
				)
			))
			.toList();

		return new MyStudyHistoryResult(activeStudies, pastStudies);
	}

	@Transactional
	public StudyResult join(Long studyId, Long requesterMemberId) {
		Study study = getStudyForUpdate(studyId);
		if (study.getStatus() == StudyStatus.CLOSED && isCapacityFull(study)) {
			throw new BusinessException(StudyErrorCode.STUDY_CAPACITY_FULL);
		}
		if (study.getStatus() == StudyStatus.CLOSED) {
			throw new BusinessException(StudyErrorCode.STUDY_ALREADY_CLOSED);
		}
		if (isCapacityFull(study)) {
			throw new BusinessException(StudyErrorCode.STUDY_CAPACITY_FULL);
		}
		StudyMember existingMember = studyMemberRepository.findByStudyIdAndMemberId(studyId, requesterMemberId)
			.orElse(null);
		if (existingMember != null && existingMember.isJoined()) {
			throw new BusinessException(StudyErrorCode.ALREADY_JOINED);
		}
		if (existingMember != null) {
			existingMember.rejoin();
			closeIfCapacityFull(study);
			return toResult(study, requesterMemberId);
		}

		studyMemberRepository.save(StudyMember.member(studyId, requesterMemberId));
		closeIfCapacityFull(study);
		return toResult(study, requesterMemberId);
	}

	@Transactional
	public StudyResult leave(Long studyId, Long requesterMemberId) {
		Study study = getStudyForUpdate(studyId);
		if (study.getOwnerMemberId().equals(requesterMemberId)) {
			throw new BusinessException(StudyErrorCode.OWNER_CANNOT_LEAVE);
		}

		StudyMember studyMember = studyMemberRepository.findByStudyIdAndMemberIdAndStatus(
				studyId,
				requesterMemberId,
				StudyMemberStatus.JOINED
			)
			.orElseThrow(() -> new BusinessException(StudyErrorCode.NOT_STUDY_MEMBER));
		studyMember.leave();
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
			.filter(StudyMember::isJoined)
			.map(StudyMember::getStudyId)
			.collect(Collectors.toUnmodifiableSet());
	}

	private StudyResult toResult(Study study, Long requesterMemberId) {
		boolean joinedByRequester = requesterMemberId != null
			&& studyMemberRepository.existsByStudyIdAndMemberIdAndStatus(
				study.getId(),
				requesterMemberId,
				StudyMemberStatus.JOINED
			);
		return toResult(study, requesterMemberId, joinedByRequester);
	}

	private boolean isClosedStudy(Study study) {
		return study != null && study.getStatus() == StudyStatus.CLOSED;
	}

	private void closeIfCapacityFull(Study study) {
		if (isCapacityFull(study)) {
			study.closeWhenCapacityFull();
		}
	}

	private boolean isCapacityFull(Study study) {
		Integer capacity = study.getCapacity();
		if (capacity == null) {
			return false;
		}
		long joinedCount = studyMemberRepository.countByStudyIdAndStatus(
			study.getId(),
			StudyMemberStatus.JOINED
		);
		return joinedCount >= capacity;
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

	private String descriptionFor(StudyCreateCommand command) {
		if (hasText(command.description())) {
			return command.description().trim();
		}
		return command.progressMethod().trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

package com.studywithme.report.application;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.report.domain.MemberSanction;
import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.exception.MemberSanctionErrorCode;
import com.studywithme.report.repository.MemberSanctionRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberSanctionService {

	private final MemberSanctionRepository memberSanctionRepository;
	private final MemberRepository memberRepository;

	public MemberSanctionService(
		MemberSanctionRepository memberSanctionRepository,
		MemberRepository memberRepository
	) {
		this.memberSanctionRepository = memberSanctionRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public MemberSanctionResult createSanction(Long requesterMemberId, MemberSanctionCreateCommand command) {
		ensureAdmin(requesterMemberId);
		Member target = memberRepository.findById(command.targetMemberId())
			.filter(member -> member.getStatus() == MemberStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(MemberSanctionErrorCode.MEMBER_SANCTION_TARGET_NOT_FOUND));
		MemberSanction sanction = memberSanctionRepository.save(MemberSanction.create(
			target.getId(),
			requesterMemberId,
			command.type(),
			command.reason(),
			normalizeSourceType(command.sourceType()),
			command.sourceId()
		));
		return toResult(sanction);
	}

	public List<MemberSanctionResult> findSanctions(Long requesterMemberId, Long targetMemberId) {
		ensureAdmin(requesterMemberId);
		memberRepository.findById(targetMemberId)
			.orElseThrow(() -> new BusinessException(MemberSanctionErrorCode.MEMBER_SANCTION_TARGET_NOT_FOUND));
		return toResults(memberSanctionRepository.findAllByTargetMemberIdOrderByCreatedAtDescIdDesc(targetMemberId));
	}

	private void ensureAdmin(Long requesterMemberId) {
		boolean isAdmin = memberRepository.findById(requesterMemberId)
			.map(member -> member.getRoles().contains(MemberRole.ADMIN))
			.orElse(false);
		if (!isAdmin) {
			throw new BusinessException(MemberSanctionErrorCode.MEMBER_SANCTION_ADMIN_REQUIRED);
		}
	}

	private MemberSanctionSourceType normalizeSourceType(MemberSanctionSourceType sourceType) {
		return sourceType == null ? MemberSanctionSourceType.MANUAL : sourceType;
	}

	private MemberSanctionResult toResult(MemberSanction sanction) {
		return MemberSanctionResult.from(sanction, findMembers(List.of(sanction)));
	}

	private List<MemberSanctionResult> toResults(List<MemberSanction> sanctions) {
		Map<Long, Member> members = findMembers(sanctions);
		return sanctions.stream()
			.map(sanction -> MemberSanctionResult.from(sanction, members))
			.toList();
	}

	private Map<Long, Member> findMembers(List<MemberSanction> sanctions) {
		Set<Long> memberIds = new LinkedHashSet<>();
		for (MemberSanction sanction : sanctions) {
			memberIds.add(sanction.getTargetMemberId());
			memberIds.add(sanction.getAdminMemberId());
		}
		return memberRepository.findAllById(memberIds)
			.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
	}
}

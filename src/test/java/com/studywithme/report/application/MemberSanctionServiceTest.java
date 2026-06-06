package com.studywithme.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.report.domain.MemberSanctionSourceType;
import com.studywithme.report.domain.MemberSanctionType;
import com.studywithme.report.exception.MemberSanctionErrorCode;
import com.studywithme.report.repository.MemberSanctionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MemberSanctionService.class)
class MemberSanctionServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberSanctionRepository memberSanctionRepository;

	@Autowired
	private MemberSanctionService memberSanctionService;

	@Test
	@DisplayName("관리자는 활성 회원에게 제재 이력을 기록할 수 있다")
	void createSanctionByAdmin() {
		Member target = saveMember("sanction-target");
		Member admin = saveAdmin("sanction-admin");

		MemberSanctionResult result = memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(
				target.getId(),
				MemberSanctionType.WARNING,
				"부적절한 메시지 반복",
				MemberSanctionSourceType.CHAT_MESSAGE_REPORT,
				10L
			)
		);

		assertThat(result.id()).isNotNull();
		assertThat(result.targetMemberId()).isEqualTo(target.getId());
		assertThat(result.targetNickname()).isEqualTo("sanction-target");
		assertThat(result.adminMemberId()).isEqualTo(admin.getId());
		assertThat(result.adminNickname()).isEqualTo("sanction-admin");
		assertThat(result.type()).isEqualTo(MemberSanctionType.WARNING);
		assertThat(result.reason()).isEqualTo("부적절한 메시지 반복");
		assertThat(result.sourceType()).isEqualTo(MemberSanctionSourceType.CHAT_MESSAGE_REPORT);
		assertThat(result.sourceId()).isEqualTo(10L);
		assertThat(result.createdAt()).isNotNull();
		assertThat(memberSanctionRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("일반 회원은 제재 이력을 기록할 수 없다")
	void rejectCreateSanctionByNonAdmin() {
		Member target = saveMember("non-admin-sanction-target");
		Member requester = saveMember("non-admin-sanction-requester");

		assertThatThrownBy(() -> memberSanctionService.createSanction(
				requester.getId(),
				new MemberSanctionCreateCommand(
					target.getId(),
					MemberSanctionType.WARNING,
					"권한 없음",
					MemberSanctionSourceType.MANUAL,
					null
				)
			))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberSanctionErrorCode.MEMBER_SANCTION_ADMIN_REQUIRED);
	}

	@Test
	@DisplayName("탈퇴 회원에게는 새 제재 이력을 기록할 수 없다")
	void rejectCreateSanctionForWithdrawnMember() {
		Member target = saveMember("withdrawn-sanction-target");
		target.withdraw("withdrawn-sanction-target@example.com", "withdrawn:sanction-target");
		memberRepository.saveAndFlush(target);
		Member admin = saveAdmin("withdrawn-sanction-admin");

		assertThatThrownBy(() -> memberSanctionService.createSanction(
				admin.getId(),
				new MemberSanctionCreateCommand(
					target.getId(),
					MemberSanctionType.WARNING,
					"탈퇴 회원 제재",
					MemberSanctionSourceType.MANUAL,
					null
				)
			))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberSanctionErrorCode.MEMBER_SANCTION_TARGET_NOT_FOUND);
	}

	@Test
	@DisplayName("관리자는 특정 회원의 제재 이력을 최신순으로 조회할 수 있다")
	void findSanctionsByTargetMember() {
		Member target = saveMember("history-sanction-target");
		Member otherTarget = saveMember("history-sanction-other-target");
		Member admin = saveAdmin("history-sanction-admin");
		MemberSanctionResult first = memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(target.getId(), MemberSanctionType.WARNING, "첫 경고", MemberSanctionSourceType.MANUAL, null)
		);
		MemberSanctionResult second = memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(target.getId(), MemberSanctionType.WARNING, "두 번째 경고", MemberSanctionSourceType.CONTENT_REPORT, 20L)
		);
		memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(otherTarget.getId(), MemberSanctionType.WARNING, "다른 회원", MemberSanctionSourceType.MANUAL, null)
		);

		assertThat(memberSanctionService.findSanctions(admin.getId(), target.getId()))
			.extracting(MemberSanctionResult::id)
			.containsExactly(second.id(), first.id());
	}

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}

	private Member saveAdmin(String name) {
		Member member = Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		);
		member.grantRole(MemberRole.ADMIN);
		return memberRepository.saveAndFlush(member);
	}
}

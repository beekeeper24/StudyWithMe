package com.studywithme.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.global.exception.BusinessException;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
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
	@DisplayName("정지 제재를 기록하면 대상 회원 상태를 정지로 변경한다")
	void suspendMemberBySanction() {
		Member target = saveMember("suspended-sanction-target");
		Member admin = saveAdmin("suspended-sanction-admin");

		MemberSanctionResult result = memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(
				target.getId(),
				MemberSanctionType.SUSPENSION,
				"반복적인 부적절한 채팅",
				MemberSanctionSourceType.CHAT_MESSAGE_REPORT,
				11L
			)
		);

		assertThat(result.type()).isEqualTo(MemberSanctionType.SUSPENSION);
		assertThat(memberRepository.findById(target.getId()).orElseThrow().getStatus())
			.isEqualTo(MemberStatus.SUSPENDED);
	}

	@Test
	@DisplayName("차단 제재를 기록하면 대상 회원 상태를 차단으로 변경한다")
	void banMemberBySanction() {
		Member target = saveMember("banned-sanction-target");
		Member admin = saveAdmin("banned-sanction-admin");

		MemberSanctionResult result = memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(
				target.getId(),
				MemberSanctionType.BAN,
				"커뮤니티 악용",
				MemberSanctionSourceType.CONTENT_REPORT,
				12L
			)
		);

		assertThat(result.type()).isEqualTo(MemberSanctionType.BAN);
		assertThat(memberRepository.findById(target.getId()).orElseThrow().getStatus())
			.isEqualTo(MemberStatus.BANNED);
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
	@DisplayName("복구 이력은 일반 제재 생성 흐름으로 기록할 수 없다")
	void rejectCreateRestoreSanction() {
		Member target = saveMember("create-restore-target");
		Member admin = saveAdmin("create-restore-admin");

		assertThatThrownBy(() -> memberSanctionService.createSanction(
				admin.getId(),
				new MemberSanctionCreateCommand(
					target.getId(),
					MemberSanctionType.RESTORE,
					"전용 복구 API를 사용해야 함",
					MemberSanctionSourceType.MANUAL,
					null
				)
			))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberSanctionErrorCode.MEMBER_SANCTION_RESTORE_TYPE_NOT_ALLOWED);
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

	@Test
	@DisplayName("관리자는 정지 회원을 활성 상태로 복구하고 복구 이력을 남길 수 있다")
	void restoreSuspendedMemberByAdmin() {
		Member target = saveMember("restore-suspended-target");
		Member admin = saveAdmin("restore-suspended-admin");
		memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(
				target.getId(),
				MemberSanctionType.SUSPENSION,
				"정지 처리",
				MemberSanctionSourceType.MANUAL,
				null
			)
		);

		MemberSanctionResult result = memberSanctionService.restoreMember(
			admin.getId(),
			new MemberSanctionRestoreCommand(
				target.getId(),
				"소명 확인 후 복구",
				MemberSanctionSourceType.MANUAL,
				null
			)
		);

		assertThat(result.type()).isEqualTo(MemberSanctionType.RESTORE);
		assertThat(result.reason()).isEqualTo("소명 확인 후 복구");
		assertThat(memberRepository.findById(target.getId()).orElseThrow().getStatus())
			.isEqualTo(MemberStatus.ACTIVE);
		assertThat(memberSanctionService.findSanctions(admin.getId(), target.getId()))
			.extracting(MemberSanctionResult::type)
			.containsExactly(MemberSanctionType.RESTORE, MemberSanctionType.SUSPENSION);
	}

	@Test
	@DisplayName("관리자는 차단 회원을 활성 상태로 복구할 수 있다")
	void restoreBannedMemberByAdmin() {
		Member target = saveMember("restore-banned-target");
		Member admin = saveAdmin("restore-banned-admin");
		memberSanctionService.createSanction(
			admin.getId(),
			new MemberSanctionCreateCommand(target.getId(), MemberSanctionType.BAN, "차단 처리", MemberSanctionSourceType.MANUAL, null)
		);

		memberSanctionService.restoreMember(
			admin.getId(),
			new MemberSanctionRestoreCommand(target.getId(), "오인 차단 복구", MemberSanctionSourceType.MANUAL, null)
		);

		assertThat(memberRepository.findById(target.getId()).orElseThrow().getStatus())
			.isEqualTo(MemberStatus.ACTIVE);
	}

	@Test
	@DisplayName("활성 회원은 복구 대상으로 처리하지 않는다")
	void rejectRestoreActiveMember() {
		Member target = saveMember("restore-active-target");
		Member admin = saveAdmin("restore-active-admin");

		assertThatThrownBy(() -> memberSanctionService.restoreMember(
				admin.getId(),
				new MemberSanctionRestoreCommand(target.getId(), "이미 활성", MemberSanctionSourceType.MANUAL, null)
			))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberSanctionErrorCode.MEMBER_SANCTION_RESTORE_TARGET_NOT_RESTRICTED);
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

package com.studywithme.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.studywithme.auth.oauth.OAuth2UserProfile;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OAuthLoginServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("처음 로그인한 OAuth 회원은 별명 설정이 필요한 새 회원으로 저장한다")
	void signUpOAuthMember() {
		OAuthLoginService oauthLoginService = new OAuthLoginService(memberRepository);
		OAuth2UserProfile profile = new OAuth2UserProfile(
			OAuthProvider.GOOGLE,
			"google-123",
			"bee@example.com",
			"beekeeper",
			"https://example.com/profile.png"
		);

		Member member = oauthLoginService.loginOrSignUp(profile);

		assertThat(member.getId()).isNotNull();
		assertThat(member.getOauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
		assertThat(member.getOauthSubject()).isEqualTo("google-123");
		assertThat(member.getNickname()).isNull();
		assertThat(member.isNicknameRequired()).isTrue();
		assertThat(memberRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("이미 가입한 OAuth 회원은 새로 만들지 않고 기존 회원을 반환한다")
	void loginExistingOAuthMember() {
		OAuthLoginService oauthLoginService = new OAuthLoginService(memberRepository);
		OAuth2UserProfile firstProfile = new OAuth2UserProfile(
			OAuthProvider.KAKAO,
			"kakao-123",
			"first@kakao.com",
			"beekeeper",
			"https://example.com/first.png"
		);
		Member firstMember = oauthLoginService.loginOrSignUp(firstProfile);

		OAuth2UserProfile secondProfile = new OAuth2UserProfile(
			OAuthProvider.KAKAO,
			"kakao-123",
			"second@kakao.com",
			"beekeeper",
			"https://example.com/second.png"
		);
		Member secondMember = oauthLoginService.loginOrSignUp(secondProfile);

		assertThat(secondMember.getId()).isEqualTo(firstMember.getId());
		assertThat(memberRepository.count()).isEqualTo(1);
		assertThat(secondMember.getEmail()).isEqualTo("second@kakao.com");
		assertThat(secondMember.getProfileImageUrl()).isEqualTo("https://example.com/second.png");
	}

	@Test
	@DisplayName("이미 가입한 OAuth 회원은 설정한 별명을 유지한다")
	void keepConfiguredNicknameForExistingOAuthMember() {
		OAuthLoginService oauthLoginService = new OAuthLoginService(memberRepository);
		Member firstMember = oauthLoginService.loginOrSignUp(new OAuth2UserProfile(
			OAuthProvider.GOOGLE,
			"google-123",
			"google@example.com",
			"beekeeper",
			null
		));
		firstMember.updateNickname("beekeeper");

		Member existingMember = oauthLoginService.loginOrSignUp(new OAuth2UserProfile(
			OAuthProvider.GOOGLE,
			"google-123",
			"updated@example.com",
			"oauth-name",
			"https://example.com/updated.png"
		));

		assertThat(existingMember.getId()).isEqualTo(firstMember.getId());
		assertThat(existingMember.getNickname()).isEqualTo("beekeeper");
		assertThat(existingMember.isNicknameRequired()).isFalse();
		assertThat(existingMember.getEmail()).isEqualTo("updated@example.com");
		assertThat(existingMember.getProfileImageUrl()).isEqualTo("https://example.com/updated.png");
	}
}

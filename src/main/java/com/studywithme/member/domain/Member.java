package com.studywithme.member.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
	name = "members",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_members_oauth_provider_subject",
			columnNames = {"oauth_provider", "oauth_subject"}
		),
		@UniqueConstraint(name = "uk_members_nickname", columnNames = "nickname")
	}
)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(length = 50)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "oauth_provider", nullable = false, length = 20)
	private OAuthProvider oauthProvider;

	@Column(name = "oauth_subject", nullable = false, length = 100)
	private String oauthSubject;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Column(name = "terms_agreed_at")
	private LocalDateTime termsAgreedAt;

	@Column(name = "terms_version", length = 20)
	private String termsVersion;

	@Column(name = "privacy_policy_version", length = 20)
	private String privacyPolicyVersion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private Set<MemberRole> roles = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Member() {
	}

	private Member(
		String email,
		String nickname,
		OAuthProvider oauthProvider,
		String oauthSubject,
		String profileImageUrl
	) {
		this.email = email;
		this.nickname = nickname;
		this.oauthProvider = oauthProvider;
		this.oauthSubject = oauthSubject;
		this.profileImageUrl = profileImageUrl;
		if (nickname != null && !nickname.isBlank()) {
			this.termsAgreedAt = LocalDateTime.now();
			this.termsVersion = "LEGACY";
			this.privacyPolicyVersion = "LEGACY";
		}
		this.status = MemberStatus.ACTIVE;
		this.roles.add(MemberRole.USER);
	}

	public static Member createOAuthMember(
		String email,
		String nickname,
		OAuthProvider oauthProvider,
		String oauthSubject,
		String profileImageUrl
	) {
		return new Member(email, nickname, oauthProvider, oauthSubject, profileImageUrl);
	}

	public void updateOAuthProfile(String email, String profileImageUrl) {
		this.email = email;
		this.profileImageUrl = profileImageUrl;
	}

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void completeSignup(
		String nickname,
		String termsVersion,
		String privacyPolicyVersion,
		LocalDateTime agreedAt
	) {
		this.nickname = nickname;
		this.termsVersion = termsVersion;
		this.privacyPolicyVersion = privacyPolicyVersion;
		this.termsAgreedAt = agreedAt;
	}

	public boolean isNicknameRequired() {
		return nickname == null || nickname.isBlank();
	}

	public boolean isTermsAgreementRequired() {
		return termsAgreedAt == null;
	}

	public boolean isSignupRequired() {
		return isNicknameRequired() || isTermsAgreementRequired();
	}

	public void withdraw(String anonymizedEmail, String anonymizedOauthSubject) {
		this.email = anonymizedEmail;
		this.nickname = null;
		this.oauthSubject = anonymizedOauthSubject;
		this.profileImageUrl = null;
		this.status = MemberStatus.WITHDRAWN;
	}

	public void withdraw() {
		this.status = MemberStatus.WITHDRAWN;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public OAuthProvider getOauthProvider() {
		return oauthProvider;
	}

	public String getOauthSubject() {
		return oauthSubject;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public LocalDateTime getTermsAgreedAt() {
		return termsAgreedAt;
	}

	public String getTermsVersion() {
		return termsVersion;
	}

	public String getPrivacyPolicyVersion() {
		return privacyPolicyVersion;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public Set<MemberRole> getRoles() {
		return Collections.unmodifiableSet(roles);
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}

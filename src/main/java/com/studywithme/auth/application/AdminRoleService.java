package com.studywithme.auth.application;

import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminRoleService {

	private final Set<String> adminEmails;

	public AdminRoleService(AdminRoleProperties properties) {
		this.adminEmails = properties.getEmails().stream()
			.map(AdminRoleService::normalizeEmail)
			.filter(email -> !email.isBlank())
			.collect(Collectors.toUnmodifiableSet());
	}

	public boolean grantIfConfiguredAdmin(Member member) {
		if (member.getStatus() != MemberStatus.ACTIVE || !isConfiguredAdmin(member.getEmail())) {
			return false;
		}

		member.grantRole(MemberRole.ADMIN);
		return true;
	}

	public Set<String> getAdminEmails() {
		return adminEmails;
	}

	private boolean isConfiguredAdmin(String email) {
		return adminEmails.contains(normalizeEmail(email));
	}

	static String normalizeEmail(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}

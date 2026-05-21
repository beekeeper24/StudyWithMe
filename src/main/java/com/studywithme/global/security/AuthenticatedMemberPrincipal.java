package com.studywithme.global.security;

import java.security.Principal;
import java.util.Set;

public record AuthenticatedMemberPrincipal(
	Long memberId,
	Set<String> roles
) implements Principal {

	public AuthenticatedMemberPrincipal {
		roles = Set.copyOf(roles);
	}

	@Override
	public String getName() {
		return memberId.toString();
	}
}

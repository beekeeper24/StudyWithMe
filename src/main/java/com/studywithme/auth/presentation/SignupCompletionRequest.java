package com.studywithme.auth.presentation;

public record SignupCompletionRequest(
	String nickname,
	boolean termsAgreed,
	boolean privacyPolicyAgreed
) {
}

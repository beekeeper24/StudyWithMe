package com.studywithme.auth.token;

import com.studywithme.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "token_hash", nullable = false, unique = true, length = 100)
	private String tokenHash;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "rotated_at")
	private Instant rotatedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected RefreshToken() {
	}

	private RefreshToken(Member member, String tokenHash, Instant issuedAt, Instant expiresAt) {
		this.member = member;
		this.tokenHash = tokenHash;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken issue(Member member, String rawToken, Instant issuedAt, Instant expiresAt) {
		return new RefreshToken(member, RefreshTokenHash.sha256(rawToken), issuedAt, expiresAt);
	}

	public boolean matches(String rawToken) {
		return tokenHash.equals(RefreshTokenHash.sha256(rawToken));
	}

	public boolean isUsable(Instant now) {
		return !isRotated() && !isRevoked() && expiresAt.isAfter(now);
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public void rotate(Instant rotatedAt) {
		this.rotatedAt = rotatedAt;
	}

	public void revoke(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isRotated() {
		return rotatedAt != null;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}
}

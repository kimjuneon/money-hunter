package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_sessions")
public class AdminSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 96)
	private String tokenHash;

	@Column(nullable = false, length = 80)
	private String username;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected AdminSession() {
	}

	public AdminSession(String tokenHash, String username, Instant createdAt, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.username = username;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public String getUsername() {
		return username;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}
}

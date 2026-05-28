package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_sessions")
public class LoginSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 96)
	private String tokenHash;

	@Column(name = "user_key", nullable = false, length = 120)
	private String userKey;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected LoginSession() {
	}

	public LoginSession(String tokenHash, String userKey, Instant createdAt, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.userKey = userKey;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public String getUserKey() {
		return userKey;
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}
}

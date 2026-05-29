package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_reward_sessions")
public class AdRewardSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdEventType type;

	@Column(name = "session_token", nullable = false, unique = true, length = 120)
	private String sessionToken;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant completedAt;

	protected AdRewardSession() {
	}

	public AdRewardSession(Player player, AdEventType type, String sessionToken, Instant createdAt, Instant expiresAt) {
		this.player = player;
		this.type = type;
		this.sessionToken = sessionToken;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public Player getPlayer() {
		return player;
	}

	public AdEventType getType() {
		return type;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public boolean isCompleted() {
		return completedAt != null;
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public void complete(Instant completedAt) {
		this.completedAt = completedAt;
	}
}

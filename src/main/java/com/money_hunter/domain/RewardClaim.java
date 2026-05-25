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
@Table(name = "reward_claims")
public class RewardClaim {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Column(nullable = false)
	private long goldSpent;

	@Column(nullable = false)
	private int pointAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RewardClaimStatus status;

	@Column(nullable = false, unique = true, length = 120)
	private String idempotencyKey;

	@Column(nullable = false)
	private Instant createdAt;

	protected RewardClaim() {
	}

	public RewardClaim(Player player, long goldSpent, int pointAmount, String idempotencyKey, Instant now) {
		this.player = player;
		this.goldSpent = goldSpent;
		this.pointAmount = pointAmount;
		this.idempotencyKey = idempotencyKey;
		this.status = RewardClaimStatus.PENDING_PROMOTION_GRANT;
		this.createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public int getPointAmount() {
		return pointAmount;
	}

	public RewardClaimStatus getStatus() {
		return status;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}
}

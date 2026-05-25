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
@Table(name = "ad_events")
public class AdEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdEventType type;

	@Column(nullable = false)
	private int rewardValue;

	@Column(nullable = false)
	private Instant occurredAt;

	protected AdEvent() {
	}

	public AdEvent(Player player, AdEventType type, int rewardValue, Instant occurredAt) {
		this.player = player;
		this.type = type;
		this.rewardValue = rewardValue;
		this.occurredAt = occurredAt;
	}
}

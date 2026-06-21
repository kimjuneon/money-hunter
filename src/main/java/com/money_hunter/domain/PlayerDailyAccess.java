package com.money_hunter.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "player_daily_accesses",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_player_daily_accesses_player_date",
				columnNames = {"player_id", "access_date"}
		)
)
public class PlayerDailyAccess {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Column(name = "access_date", nullable = false)
	private LocalDate accessDate;

	@Column(nullable = false)
	private Instant firstAccessedAt;

	@Column(nullable = false)
	private Instant lastAccessedAt;

	protected PlayerDailyAccess() {
	}

	public PlayerDailyAccess(Player player, LocalDate accessDate, Instant now) {
		this.player = player;
		this.accessDate = accessDate;
		this.firstAccessedAt = now;
		this.lastAccessedAt = now;
	}
}

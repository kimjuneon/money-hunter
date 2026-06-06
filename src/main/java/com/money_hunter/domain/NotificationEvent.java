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
@Table(name = "notification_events")
public class NotificationEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NotificationType type;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 500)
	private String body;

	@Column(nullable = false)
	private Instant sentAt;

	private Long settledGold;

	@Column(nullable = false)
	private int levelGain = 0;

	@Column(nullable = false)
	private int skillPointGain = 0;

	@Column(nullable = false)
	private long combatPowerGain = 0;

	private Instant readAt;

	@Column(nullable = false)
	private Instant createdAt;

	protected NotificationEvent() {
	}

	public NotificationEvent(Player player, NotificationType type, String title, String body, Instant sentAt) {
		this(player, type, title, body, sentAt, null);
	}

	public NotificationEvent(Player player, NotificationType type, String title, String body, Instant sentAt, Long settledGold) {
		this(player, type, title, body, sentAt, settledGold, 0, 0, 0);
	}

	public NotificationEvent(
			Player player,
			NotificationType type,
			String title,
			String body,
			Instant sentAt,
			Long settledGold,
			int levelGain,
			int skillPointGain,
			long combatPowerGain
	) {
		this.player = player;
		this.type = type;
		this.title = title;
		this.body = body;
		this.sentAt = sentAt;
		this.settledGold = settledGold;
		this.levelGain = Math.max(0, levelGain);
		this.skillPointGain = Math.max(0, skillPointGain);
		this.combatPowerGain = Math.max(0, combatPowerGain);
		this.createdAt = sentAt;
	}

	public Long getId() {
		return id;
	}

	public NotificationType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public Long getSettledGold() {
		return settledGold;
	}

	public int getLevelGain() {
		return levelGain;
	}

	public int getSkillPointGain() {
		return skillPointGain;
	}

	public long getCombatPowerGain() {
		return combatPowerGain;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public void markRead(Instant readAt) {
		this.readAt = readAt;
	}
}

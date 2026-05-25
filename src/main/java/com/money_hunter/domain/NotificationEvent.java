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

	private Instant readAt;

	@Column(nullable = false)
	private Instant createdAt;

	protected NotificationEvent() {
	}

	public NotificationEvent(Player player, NotificationType type, String title, String body, Instant sentAt) {
		this.player = player;
		this.type = type;
		this.title = title;
		this.body = body;
		this.sentAt = sentAt;
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

	public Instant getReadAt() {
		return readAt;
	}

	public void markRead(Instant readAt) {
		this.readAt = readAt;
	}
}

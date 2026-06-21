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
@Table(name = "ad_client_events")
public class AdClientEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdEventType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdClientEventType eventType;

	@Column(length = 60)
	private String adGroupKey;

	@Column(length = 120)
	private String adGroupId;

	@Column(length = 120)
	private String sessionToken;

	@Column(length = 500)
	private String errorMessage;

	@Column(nullable = false)
	private Instant occurredAt;

	protected AdClientEvent() {
	}

	public AdClientEvent(
			Player player,
			AdEventType type,
			AdClientEventType eventType,
			String adGroupKey,
			String adGroupId,
			String sessionToken,
			String errorMessage,
			Instant occurredAt
	) {
		this.player = player;
		this.type = type;
		this.eventType = eventType;
		this.adGroupKey = normalize(adGroupKey, 60);
		this.adGroupId = normalize(adGroupId, 120);
		this.sessionToken = normalize(sessionToken, 120);
		this.errorMessage = normalize(errorMessage, 500);
		this.occurredAt = occurredAt;
	}

	private String normalize(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
	}
}

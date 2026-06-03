package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "rookie_event_settings")
public class RookieEventSettings {
	public static final long SINGLETON_ID = 1L;

	@Id
	private Long id;

	@Column(nullable = false)
	private boolean enabled;

	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	protected RookieEventSettings() {
	}

	public RookieEventSettings(Long id, boolean enabled, Instant now) {
		this.id = id;
		this.enabled = enabled;
		this.updatedAt = now;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setEnabled(boolean enabled, Instant now) {
		this.enabled = enabled;
		this.updatedAt = now;
	}
}

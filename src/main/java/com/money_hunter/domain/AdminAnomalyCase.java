package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_anomaly_cases")
public class AdminAnomalyCase {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "anomaly_key", nullable = false, unique = true, length = 220)
	private String anomalyKey;

	@Column(nullable = false, length = 80)
	private String category;

	@Column(name = "user_key", nullable = false, length = 180)
	private String userKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AdminAnomalyStatus status;

	@Column(length = 1000)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	protected AdminAnomalyCase() {
	}

	public AdminAnomalyCase(String anomalyKey, String category, String userKey, Instant now) {
		this.anomalyKey = anomalyKey;
		this.category = category;
		this.userKey = userKey;
		this.status = AdminAnomalyStatus.OPEN;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getAnomalyKey() {
		return anomalyKey;
	}

	public String getCategory() {
		return category;
	}

	public String getUserKey() {
		return userKey;
	}

	public AdminAnomalyStatus getStatus() {
		return status;
	}

	public String getNote() {
		return note;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getResolvedAt() {
		return resolvedAt;
	}

	public void update(AdminAnomalyStatus status, String note, Instant now) {
		this.status = status;
		this.note = note;
		this.updatedAt = now;
		this.resolvedAt = status == AdminAnomalyStatus.RESOLVED ? now : null;
	}
}

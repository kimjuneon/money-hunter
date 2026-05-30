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
@Table(name = "admin_anomaly_actions")
public class AdminAnomalyAction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "anomaly_case_id", nullable = false)
	private AdminAnomalyCase anomalyCase;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AdminAnomalyStatus status;

	@Column(length = 1000)
	private String note;

	@Column(name = "actor_fingerprint", nullable = false, length = 24)
	private String actorFingerprint;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AdminAnomalyAction() {
	}

	public AdminAnomalyAction(
			AdminAnomalyCase anomalyCase,
			AdminAnomalyStatus status,
			String note,
			String actorFingerprint,
			Instant createdAt
	) {
		this.anomalyCase = anomalyCase;
		this.status = status;
		this.note = note;
		this.actorFingerprint = actorFingerprint;
		this.createdAt = createdAt;
	}

	public AdminAnomalyStatus getStatus() {
		return status;
	}

	public String getNote() {
		return note;
	}

	public String getActorFingerprint() {
		return actorFingerprint;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

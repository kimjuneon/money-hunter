package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 40)
	private String action;

	@Column(nullable = false, length = 80)
	private String target;

	@Column(name = "actor_fingerprint", nullable = false, length = 24)
	private String actorFingerprint;

	@Column(name = "before_value", length = 4000)
	private String beforeValue;

	@Column(name = "after_value", length = 4000)
	private String afterValue;

	@Column(length = 500)
	private String reason;

	@Column(name = "client_ip", length = 80)
	private String clientIp;

	@Column(name = "user_agent", length = 300)
	private String userAgent;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AdminAuditLog() {
	}

	public AdminAuditLog(
			String action,
			String target,
			String actorFingerprint,
			String beforeValue,
			String afterValue,
			String reason,
			String clientIp,
			String userAgent,
			Instant createdAt
	) {
		this.action = action;
		this.target = target;
		this.actorFingerprint = actorFingerprint;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
		this.reason = reason;
		this.clientIp = clientIp;
		this.userAgent = userAgent;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getAction() {
		return action;
	}

	public String getTarget() {
		return target;
	}

	public String getActorFingerprint() {
		return actorFingerprint;
	}

	public String getBeforeValue() {
		return beforeValue;
	}

	public String getAfterValue() {
		return afterValue;
	}

	public String getReason() {
		return reason;
	}

	public String getClientIp() {
		return clientIp;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

package com.money_hunter.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_in_toss_ad_daily_metrics")
public class AppInTossAdDailyMetric {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "metric_date", nullable = false, unique = true)
	private LocalDate metricDate;

	@Column(name = "ad_impressions", nullable = false)
	private long adImpressions;

	@Column(name = "ad_watch_rate_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal adWatchRatePercent;

	@Column(name = "ecpm_won", nullable = false, precision = 12, scale = 2)
	private BigDecimal ecpmWon;

	@Column(length = 500)
	private String note;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected AppInTossAdDailyMetric() {
	}

	public AppInTossAdDailyMetric(LocalDate metricDate, Instant now) {
		this.metricDate = metricDate;
		this.createdAt = now;
		this.updatedAt = now;
		this.adWatchRatePercent = BigDecimal.ZERO;
		this.ecpmWon = BigDecimal.ZERO;
	}

	public LocalDate getMetricDate() {
		return metricDate;
	}

	public long getAdImpressions() {
		return adImpressions;
	}

	public BigDecimal getAdWatchRatePercent() {
		return adWatchRatePercent;
	}

	public BigDecimal getEcpmWon() {
		return ecpmWon;
	}

	public String getNote() {
		return note;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(long adImpressions, BigDecimal adWatchRatePercent, BigDecimal ecpmWon, String note, Instant now) {
		this.adImpressions = adImpressions;
		this.adWatchRatePercent = adWatchRatePercent;
		this.ecpmWon = ecpmWon;
		this.note = note == null || note.isBlank() ? null : note.trim();
		this.updatedAt = now;
	}
}

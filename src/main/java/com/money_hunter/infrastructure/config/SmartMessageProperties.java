package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.smart-message")
public record SmartMessageProperties(
		String autoHuntEndedTemplateSetCode,
		String autoHuntEndedAgreementTemplateCode,
		String landingUrl,
		boolean rookieEventMissionArrivedEnabled,
		String rookieEventMissionArrivedTemplateSetCode,
		String rookieEventMissionAgreementTemplateCode,
		int rookieEventMissionBatchSize
) {
	public String normalizedAutoHuntEndedTemplateSetCode() {
		return value(autoHuntEndedTemplateSetCode);
	}

	public String normalizedAutoHuntEndedAgreementTemplateCode() {
		return value(autoHuntEndedAgreementTemplateCode);
	}

	public Map<String, String> autoHuntEndedContext() {
		return Map.of(
				"title", "사냥 종료",
				"message", "자동사냥이 종료됐어요.",
				"landingUrl", value(landingUrl)
		);
	}

	public String normalizedRookieEventMissionArrivedTemplateSetCode() {
		return value(rookieEventMissionArrivedTemplateSetCode);
	}

	public String normalizedRookieEventMissionAgreementTemplateCode() {
		return value(rookieEventMissionAgreementTemplateCode);
	}

	public int safeRookieEventMissionBatchSize() {
		return Math.max(1, Math.min(500, rookieEventMissionBatchSize));
	}

	public Map<String, String> rookieEventMissionArrivedContext(int day) {
		return Map.of("day", String.valueOf(Math.max(1, day)));
	}

	private String value(String raw) {
		return raw == null ? "" : raw.trim();
	}
}

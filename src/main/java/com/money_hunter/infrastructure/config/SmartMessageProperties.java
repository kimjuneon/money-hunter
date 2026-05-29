package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.smart-message")
public record SmartMessageProperties(
		String autoHuntEndedTemplateSetCode,
		String landingUrl
) {
	public String normalizedAutoHuntEndedTemplateSetCode() {
		return value(autoHuntEndedTemplateSetCode);
	}

	public Map<String, String> autoHuntEndedContext() {
		return Map.of(
				"title", "사냥 종료",
				"message", "자동사냥이 종료됐어요.",
				"landingUrl", value(landingUrl)
		);
	}

	private String value(String raw) {
		return raw == null ? "" : raw.trim();
	}
}

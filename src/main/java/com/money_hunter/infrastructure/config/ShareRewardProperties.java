package com.money_hunter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.share-reward")
public record ShareRewardProperties(
		String moduleId,
		String message
) {
	public String normalizedModuleId() {
		return value(moduleId);
	}

	public String normalizedMessage() {
		String normalized = value(message);
		return normalized.isBlank() ? "친구에게 공유하고 SP 5개를 받아요" : normalized;
	}

	private String value(String raw) {
		return raw == null ? "" : raw.trim();
	}
}

package com.money_hunter.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.security.cors")
public record CorsProperties(
		List<String> allowedOriginPatterns
) {
	public List<String> allowedOriginPatterns() {
		if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
			return List.of();
		}
		return List.copyOf(allowedOriginPatterns);
	}
}

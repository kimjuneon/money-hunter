package com.money_hunter.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.toss-api")
public record TossApiProperties(
		String baseUrl,
		String mtlsCertificatePem,
		String mtlsPrivateKeyPem,
		String mtlsCertificatePath,
		String mtlsPrivateKeyPath,
		Duration connectTimeout,
		Duration requestTimeout
) {
	public String normalizedBaseUrl() {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "https://apps-in-toss-api.toss.im";
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	public Duration normalizedConnectTimeout() {
		return connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
	}

	public Duration normalizedRequestTimeout() {
		return requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
	}
}

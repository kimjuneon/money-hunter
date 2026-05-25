package com.money_hunter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.app")
public record AppProperties(
		boolean reviewToolsEnabled,
		boolean guestUserEnabled
) {
}

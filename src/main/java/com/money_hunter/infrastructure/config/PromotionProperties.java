package com.money_hunter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.promotion")
public record PromotionProperties(
		String rewardClaimCode
) {
	public String normalizedRewardClaimCode() {
		return rewardClaimCode == null ? "" : rewardClaimCode.trim();
	}
}

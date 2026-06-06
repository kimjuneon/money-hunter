package com.money_hunter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.promotion")
public record PromotionProperties(
		String rewardClaimCode,
		String benefitTabNewUserCode,
		int benefitTabNewUserAmount
) {
	public String normalizedRewardClaimCode() {
		return rewardClaimCode == null ? "" : rewardClaimCode.trim();
	}

	public String normalizedBenefitTabNewUserCode() {
		return benefitTabNewUserCode == null ? "" : benefitTabNewUserCode.trim();
	}

	public int normalizedBenefitTabNewUserAmount() {
		return Math.max(0, benefitTabNewUserAmount);
	}
}

package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.ads")
public record AdProperties(
		String autoHuntRewardAdGroupId,
		String boostRewardAdGroupId,
		String skillPointRewardAdGroupId,
		String rewardClaimRewardAdGroupId,
		String jobChangeInterstitialAdGroupId,
		String bannerAdGroupId
) {
	public Map<String, String> clientAdGroupIds() {
		return Map.of(
				"autoHunt", value(autoHuntRewardAdGroupId),
				"boost", value(boostRewardAdGroupId),
				"skillPoint", value(skillPointRewardAdGroupId),
				"rewardClaim", value(rewardClaimRewardAdGroupId),
				"jobChange", value(jobChangeInterstitialAdGroupId),
				"banner", value(bannerAdGroupId)
		);
	}

	private String value(String input) {
		return input == null ? "" : input.trim();
	}
}

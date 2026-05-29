package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.ads")
public record AdProperties(
		String mode,
		String autoHuntRewardAdGroupId,
		String boostRewardAdGroupId,
		String skillPointRewardAdGroupId,
		String rewardClaimRewardAdGroupId,
		String jobChangeInterstitialAdGroupId,
		String bannerAdGroupId
) {
	private static final String TEST_REWARDED_AD_GROUP_ID = "ait-ad-test-rewarded-id";
	private static final String TEST_INTERSTITIAL_AD_GROUP_ID = "ait-ad-test-interstitial-id";
	private static final String TEST_BANNER_AD_GROUP_ID = "ait-ad-test-banner-id";

	public String normalizedMode() {
		return "live".equalsIgnoreCase(value(mode)) ? "live" : "test";
	}

	public Map<String, String> clientAdGroupIds() {
		if ("test".equals(normalizedMode())) {
			return Map.of(
					"autoHunt", TEST_REWARDED_AD_GROUP_ID,
					"boost", TEST_REWARDED_AD_GROUP_ID,
					"skillPoint", TEST_REWARDED_AD_GROUP_ID,
					"rewardClaim", TEST_REWARDED_AD_GROUP_ID,
					"jobChange", TEST_INTERSTITIAL_AD_GROUP_ID,
					"banner", TEST_BANNER_AD_GROUP_ID
			);
		}
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

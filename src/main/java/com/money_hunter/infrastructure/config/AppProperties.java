package com.money_hunter.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.app")
public record AppProperties(
		boolean reviewToolsEnabled,
		boolean guestUserEnabled,
		boolean mockMonetizationEnabled,
		String appsInTossAppName,
		String integrationMode,
		String distributionTarget,
		boolean tossLoginEnabled,
		boolean tossUserKeyEnabled,
		boolean realRewardAdsEnabled,
		boolean realBannerAdsEnabled,
		boolean realPaymentsEnabled,
		boolean realIapOrderVerificationEnabled,
		boolean realTossPointRewardsEnabled,
		boolean realSmartMessageEnabled,
		boolean realShareRewardEnabled,
		boolean dungeonCouponEnabled
) {
	public boolean oneStoreTarget() {
		return "ONESTORE".equalsIgnoreCase(distributionTarget);
	}

	public boolean tossReleaseReady() {
		return releaseBlockers().isEmpty();
	}

	public List<String> releaseBlockers() {
		List<String> blockers = new ArrayList<>();
		if (reviewToolsEnabled) {
			blockers.add("review-tools-enabled");
		}
		if (guestUserEnabled) {
			blockers.add("guest-user-enabled");
		}
		if (mockMonetizationEnabled) {
			blockers.add("mock-monetization-enabled");
		}
		if (!tossLoginEnabled && !tossUserKeyEnabled) {
			blockers.add("toss-identity-disabled");
		}
		if (!tossUserKeyEnabled) {
			blockers.add("toss-user-key-disabled");
		}
		if (!realRewardAdsEnabled) {
			blockers.add("real-reward-ads-disabled");
		}
		if (!realPaymentsEnabled) {
			blockers.add("real-payments-disabled");
		}
		if (!realTossPointRewardsEnabled) {
			blockers.add("real-toss-point-rewards-disabled");
		}
		if (!realSmartMessageEnabled) {
			blockers.add("real-smart-message-disabled");
		}
		if (!realShareRewardEnabled) {
			blockers.add("real-share-reward-disabled");
		}
		return List.copyOf(blockers);
	}
}

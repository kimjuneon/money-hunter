package com.money_hunter.presentation.dto.response;

import java.util.List;

public record AppConfigResponse(
		boolean reviewToolsEnabled,
		boolean guestUserEnabled,
		boolean mockMonetizationEnabled,
		String environment,
		String appsInTossAppName,
		String integrationMode,
		String distributionTarget,
		boolean tossReleaseReady,
		List<String> releaseBlockers,
		boolean tossLoginEnabled,
		boolean tossUserKeyEnabled,
		boolean realRewardAdsEnabled,
		boolean realBannerAdsEnabled,
		boolean realPaymentsEnabled,
		boolean realTossPointRewardsEnabled,
		boolean realSmartMessageEnabled,
		boolean realShareRewardEnabled
) {
}

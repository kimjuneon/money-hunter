package com.money_hunter.application.dto.response;

import java.util.List;
import java.util.Map;

public record AppConfigResponse(
			boolean reviewToolsEnabled,
			boolean guestUserEnabled,
			boolean mockMonetizationEnabled,
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
		boolean realShareRewardEnabled,
		String adMode,
		Map<String, String> adGroupIds,
		Map<String, String> iapProductIds,
		String shareRewardModuleId,
		String shareRewardMessage,
		String autoHuntEndedNotificationAgreementTemplateCode
) {
}

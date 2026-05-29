package com.money_hunter.presentation.controller;

import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.AdProperties;
import com.money_hunter.application.dto.response.AppConfigResponse;
import com.money_hunter.infrastructure.config.IapProperties;
import com.money_hunter.infrastructure.config.ShareRewardProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppConfigController {
	private final AppProperties appProperties;
	private final AdProperties adProperties;
	private final IapProperties iapProperties;
	private final ShareRewardProperties shareRewardProperties;

	public AppConfigController(
			AppProperties appProperties,
			AdProperties adProperties,
			IapProperties iapProperties,
			ShareRewardProperties shareRewardProperties
	) {
		this.appProperties = appProperties;
		this.adProperties = adProperties;
		this.iapProperties = iapProperties;
		this.shareRewardProperties = shareRewardProperties;
	}

	@GetMapping("/config")
	public AppConfigResponse getConfig() {
		return new AppConfigResponse(
					appProperties.reviewToolsEnabled(),
					appProperties.guestUserEnabled(),
					appProperties.mockMonetizationEnabled(),
					appProperties.appsInTossAppName(),
					appProperties.integrationMode(),
					appProperties.distributionTarget(),
				appProperties.tossReleaseReady(),
				appProperties.releaseBlockers(),
				appProperties.tossLoginEnabled(),
				appProperties.tossUserKeyEnabled(),
				appProperties.realRewardAdsEnabled(),
				appProperties.realBannerAdsEnabled(),
				appProperties.realPaymentsEnabled(),
				appProperties.realTossPointRewardsEnabled(),
				appProperties.realSmartMessageEnabled(),
				appProperties.realShareRewardEnabled(),
				adProperties.normalizedMode(),
				adProperties.clientAdGroupIds(),
				iapProperties.clientProductIds(),
				shareRewardProperties.normalizedModuleId(),
				shareRewardProperties.normalizedMessage()
		);
	}
}

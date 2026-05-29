package com.money_hunter.presentation.controller;

import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.AdProperties;
import com.money_hunter.application.dto.response.AppConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppConfigController {
	private final AppProperties appProperties;
	private final AdProperties adProperties;

	public AppConfigController(AppProperties appProperties, AdProperties adProperties) {
		this.appProperties = appProperties;
		this.adProperties = adProperties;
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
				adProperties.clientAdGroupIds()
		);
	}
}

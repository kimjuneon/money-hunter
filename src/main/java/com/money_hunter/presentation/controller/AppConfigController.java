package com.money_hunter.presentation.controller;

import java.util.Arrays;

import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.presentation.dto.response.AppConfigResponse;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppConfigController {
	private final AppProperties appProperties;
	private final Environment environment;

	public AppConfigController(AppProperties appProperties, Environment environment) {
		this.appProperties = appProperties;
		this.environment = environment;
	}

	@GetMapping("/config")
	public AppConfigResponse getConfig() {
		return new AppConfigResponse(
				appProperties.reviewToolsEnabled(),
				appProperties.guestUserEnabled(),
				appProperties.mockMonetizationEnabled(),
				String.join(",", Arrays.asList(environment.getActiveProfiles())),
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
				appProperties.realShareRewardEnabled()
		);
	}
}

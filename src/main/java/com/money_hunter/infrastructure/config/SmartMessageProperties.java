package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.smart-message")
public record SmartMessageProperties(
		String autoHuntEndedTemplateSetCode,
		String autoHuntEndedAgreementTemplateCode,
		boolean autoHuntEndingSoonEnabled,
		String autoHuntEndingSoonTemplateSetCode,
		int autoHuntEndingSoonBatchSize,
		String landingUrl,
		boolean rookieEventMissionArrivedEnabled,
		String rookieEventMissionArrivedTemplateSetCode,
		String rookieEventMissionAgreementTemplateCode,
		int rookieEventMissionBatchSize,
		boolean dormantSpRewardEnabled,
		String dormantSpRewardTemplateSetCode,
		int dormantSpRewardBatchSize,
		boolean dungeonExploreAvailableEnabled,
		String dungeonExploreAvailableTemplateSetCode,
		int dungeonExploreAvailableBatchSize,
		boolean battleReadyDailyEnabled,
		String battleReadyDailyTemplateSetCode,
		int battleReadyDailyBatchSize
) {
	public String normalizedAutoHuntEndedTemplateSetCode() {
		return value(autoHuntEndedTemplateSetCode);
	}

	public String normalizedAutoHuntEndedAgreementTemplateCode() {
		return value(autoHuntEndedAgreementTemplateCode);
	}

	public Map<String, String> autoHuntEndedContext() {
		return Map.of(
				"title", "사냥 종료",
				"message", "자동사냥이 종료됐어요.",
				"landingUrl", value(landingUrl)
		);
	}

	public String normalizedAutoHuntEndingSoonTemplateSetCode() {
		return value(autoHuntEndingSoonTemplateSetCode);
	}

	public int safeAutoHuntEndingSoonBatchSize() {
		return Math.max(1, Math.min(500, autoHuntEndingSoonBatchSize));
	}

	public Map<String, String> autoHuntEndingSoonContext() {
		return Map.of(
				"title", "사냥종료 임박",
				"message", "자동 사냥이 30분 남았어요.",
				"landingUrl", value(landingUrl)
		);
	}

	public String normalizedRookieEventMissionArrivedTemplateSetCode() {
		return value(rookieEventMissionArrivedTemplateSetCode);
	}

	public String normalizedRookieEventMissionAgreementTemplateCode() {
		return value(rookieEventMissionAgreementTemplateCode);
	}

	public int safeRookieEventMissionBatchSize() {
		return Math.max(1, Math.min(500, rookieEventMissionBatchSize));
	}

	public Map<String, String> rookieEventMissionArrivedContext(int day) {
		return Map.of("day", String.valueOf(Math.max(1, day)));
	}

	public String normalizedDormantSpRewardTemplateSetCode() {
		return value(dormantSpRewardTemplateSetCode);
	}

	public int safeDormantSpRewardBatchSize() {
		return Math.max(1, Math.min(500, dormantSpRewardBatchSize));
	}

	public Map<String, String> dormantSpRewardContext() {
		return Map.of();
	}

	public String normalizedDungeonExploreAvailableTemplateSetCode() {
		return value(dungeonExploreAvailableTemplateSetCode);
	}

	public int safeDungeonExploreAvailableBatchSize() {
		return Math.max(1, Math.min(500, dungeonExploreAvailableBatchSize));
	}

	public Map<String, String> dungeonExploreAvailableContext() {
		return Map.of(
				"title", "던전 탐험 가능",
				"message", "던전 탐험 준비가 완료됐어요.",
				"landingUrl", value(landingUrl)
		);
	}

	public String normalizedBattleReadyDailyTemplateSetCode() {
		return value(battleReadyDailyTemplateSetCode);
	}

	public int safeBattleReadyDailyBatchSize() {
		return Math.max(1, Math.min(500, battleReadyDailyBatchSize));
	}

	public Map<String, String> battleReadyDailyContext(int inactivityDays) {
		int day = Math.max(1, Math.min(5, inactivityDays));
		return Map.of(
				"title", "전투 준비 완료",
				"message", "자동 전투 보상이 기다리고 있어요.",
				"landingUrl", value(landingUrl),
				"day", String.valueOf(day)
		);
	}

	private String value(String raw) {
		return raw == null ? "" : raw.trim();
	}
}

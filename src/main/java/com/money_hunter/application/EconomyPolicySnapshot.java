package com.money_hunter.application;

public record EconomyPolicySnapshot(
		int adRevenuePerRewardAdWon,
		int goldPerTossPoint,
		int companionPriceWon,
		int skillPointPackPriceWon,
		int skillPointPackAmount,
		int friendInviteRewardSkillPoints,
			int friendInviteLimit,
			int maxCharacterSlots,
			long autoHuntAdSeconds,
			long autoHuntAdCooldownSeconds,
			long maxAdSeconds,
		int dungeonFreeDailyLimit,
		int dungeonAdditionalDailyLimit,
		long dungeonReentryCooldownSeconds,
		long skillPointAdCooldownSeconds,
		long rewardGoldThreshold,
		int rewardPointAmount,
		int anomalyLimitPerRule,
		long anomalyAdEventsPerHourWarning,
		long anomalyRewardClaimsPerDayWarning,
		long anomalyGoldThresholdMultiplier,
		int anomalySkillPointsWarning,
		long anomalyTimerGraceSeconds
) {
	public void validate() {
		range(adRevenuePerRewardAdWon, 1, 10_000, "리워드 광고 1회 매출");
		range(goldPerTossPoint, 1, 1_000_000, "토스포인트 1P당 골드");
		range(companionPriceWon, 0, 1_000_000, "동료 펫 가격");
		range(skillPointPackPriceWon, 0, 1_000_000, "스킬 포인트 팩 가격");
		range(skillPointPackAmount, 1, 1_000, "스킬 포인트 팩 지급량");
		range(friendInviteRewardSkillPoints, 0, 1_000, "친구 초대 SP 보상");
			range(friendInviteLimit, 0, 100, "친구 초대 보상 제한");
			range(maxCharacterSlots, 1, 3, "최대 캐릭터 슬롯");
			range(autoHuntAdSeconds, 60, 86_400, "자동사냥 광고 보상 시간");
			range(autoHuntAdCooldownSeconds, 0, 86_400, "자동사냥 광고 보상 쿨타임");
			range(maxAdSeconds, 3_600, 86_400, "광고 보상 최대 누적 시간");
		range(dungeonFreeDailyLimit, 0, 20, "던전 기본 제공 횟수");
		range(dungeonAdditionalDailyLimit, 0, 20, "던전 광고 추가 횟수");
		range(dungeonReentryCooldownSeconds, 0, 86_400, "던전 재입장 대기 시간");
		range(skillPointAdCooldownSeconds, 0, 86_400, "SP 광고 보상 쿨타임");
		range(rewardGoldThreshold, 1, 1_000_000_000_000L, "보상 수령 환산 골드");
		range(rewardPointAmount, 1, 1_000_000, "보상 수령 포인트 기준");
		range(anomalyLimitPerRule, 1, 200, "이상징후 룰별 표시 수");
		range(anomalyAdEventsPerHourWarning, 1, 10_000, "1시간 광고 이벤트 이상징후 기준");
		range(anomalyRewardClaimsPerDayWarning, 1, 1_000, "일일 보상 수령 이상징후 기준");
		range(anomalyGoldThresholdMultiplier, 1, 1_000, "보유 골드 이상징후 배수");
		range(anomalySkillPointsWarning, 1, 100_000, "미사용 SP 이상징후 기준");
		range(anomalyTimerGraceSeconds, 0, 86_400, "자동사냥 시간 이상징후 유예");
		if (maxAdSeconds < autoHuntAdSeconds) {
			throw new IllegalArgumentException("광고 보상 최대 누적 시간은 개별 보상 시간보다 작을 수 없어요.");
		}
	}

	private static void range(long value, long min, long max, String label) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(label + " 값은 " + min + " 이상 " + max + " 이하여야 해요.");
		}
	}
}

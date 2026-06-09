package com.money_hunter.application.dto.response;

import java.time.LocalDate;

public record WeeklyPunchKingResponse(
		boolean visible,
		LocalDate weekStartDate,
		long bestScore,
		long rewardedGold,
		int rewardedSkillPoints,
		int durationSeconds,
		int ultimateCooldownSeconds,
		long goldRewardDivisor,
		long nextGoldRewardScore,
		int nextSkillPointRewardScore
) {
}

package com.money_hunter.application.dto.response;

import java.time.LocalDate;

public record DailyMissionEventResponse(
		boolean visible,
		boolean completedToday,
		int cycle,
		int completedDays,
		int currentDay,
		LocalDate currentDate,
		long autoHuntProgressSeconds,
		long autoHuntRequiredSeconds,
		int dungeonRuns,
		int dungeonRunsRequired,
		boolean rewardPending,
		boolean finalRewardPending
) {
}

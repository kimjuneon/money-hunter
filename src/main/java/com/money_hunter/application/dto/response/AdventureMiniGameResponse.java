package com.money_hunter.application.dto.response;

import java.time.LocalDate;

public record AdventureMiniGameResponse(
		boolean visible,
		boolean completedToday,
		boolean canEnter,
		int entryCostGold,
		int clearRewardSkillPoints,
		int clearSeconds,
		LocalDate completedDate
) {
}

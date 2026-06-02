package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.util.List;

public record RookieEventResponse(
		boolean visible,
		boolean active,
		boolean expired,
		boolean completed,
		boolean rewardClaimed,
		boolean lockedUntilTomorrow,
		Instant startedAt,
		Instant endsAt,
		int daysRemaining,
		int completedDays,
		int currentDay,
		String rewardName,
		String rewardDescription,
		int eventPetSkillLevel,
		List<RookieEventDayResponse> days
) {
}

package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.util.List;

public record RookieEventResponse(
		boolean visible,
		boolean started,
		boolean startable,
		boolean active,
		boolean expired,
		boolean completed,
		boolean rewardClaimed,
		boolean rewardActive,
		boolean lockedUntilTomorrow,
		Instant startedAt,
		Instant endsAt,
		Instant rewardExpiresAt,
		int daysRemaining,
		int rewardDaysRemaining,
		int completedDays,
		int currentDay,
		String rewardName,
		String rewardDescription,
		int eventPetSkillLevel,
		List<RookieEventDayResponse> days
) {
}

package com.money_hunter.application.dto.response;

import java.time.Instant;

public record AdminRookieEventSettingsResponse(
		boolean enabled,
		boolean startWindowUnlimited,
		int eventDays,
		int playerProgressDays,
		int eventPetDurationDays,
		int eventPetSkillLevel,
		long startedPlayers,
		long completedPlayers,
		long rewardClaimedPlayers,
		long eligibleUnstartedPlayers,
		Instant updatedAt
) {
}

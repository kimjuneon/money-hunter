package com.money_hunter.application.dto.response;

import java.time.Instant;

public record EventRewardResponse(
		Long id,
		String rewardKey,
		String sourceEventKey,
		String sourceEventName,
		String title,
		String description,
		String rewardLabel,
		boolean claimable,
		boolean claimed,
		Instant createdAt,
		Instant expiresAt,
		Instant claimedAt,
		int daysRemaining
) {
}

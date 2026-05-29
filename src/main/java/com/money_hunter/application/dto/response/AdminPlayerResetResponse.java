package com.money_hunter.application.dto.response;

public record AdminPlayerResetResponse(
		String userKey,
		boolean playerDeleted,
		long loginSessionsDeleted,
		long adRewardSessionsDeleted,
		long notificationsDeleted,
		long rewardClaimsDeleted,
		long adEventsDeleted
) {
}

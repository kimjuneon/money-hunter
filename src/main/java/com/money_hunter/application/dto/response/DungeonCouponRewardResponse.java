package com.money_hunter.application.dto.response;

public record DungeonCouponRewardResponse(
		String rewardType,
		String rewardLabel,
		long amount,
		String tierName,
		boolean bossTicketGranted,
		PlayerStateResponse state
) {
}

package com.money_hunter.application.dto.response;

import com.money_hunter.domain.RewardClaimStatus;

public record RewardClaimResponse(
		long claimId,
		int pointAmount,
		RewardClaimStatus status,
		String idempotencyKey,
		PlayerStateResponse state
) {
}

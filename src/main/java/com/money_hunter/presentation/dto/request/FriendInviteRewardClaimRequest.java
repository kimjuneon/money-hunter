package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FriendInviteRewardClaimRequest(
		@Min(1)
		@Max(100)
		Integer completedInvites
) {
	public int normalizedCompletedInvites() {
		return completedInvites == null ? 1 : completedInvites;
	}
}

package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminRookieEventTestRequest(
		@Min(0) @Max(7) Integer completedDays,
		@Min(0) @Max(7) Integer rewardedDays,
		Boolean finalRewardClaimed,
		@Size(max = 500) String reason
) {
}

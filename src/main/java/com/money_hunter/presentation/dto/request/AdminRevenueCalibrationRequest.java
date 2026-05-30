package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminRevenueCalibrationRequest(
		@NotNull
		@Min(1)
		@Max(10_000)
		Long adRevenuePerRewardAdWon
) {
}

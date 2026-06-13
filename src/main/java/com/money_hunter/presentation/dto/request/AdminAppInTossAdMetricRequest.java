package com.money_hunter.presentation.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAppInTossAdMetricRequest(
		@NotNull LocalDate date,
		@NotNull @Min(0) Long adImpressions,
		@NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal adWatchRatePercent,
		@NotNull @DecimalMin("0.00") BigDecimal ecpmWon,
		@Size(max = 500) String note
) {
}

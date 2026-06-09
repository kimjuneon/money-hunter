package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Min;

public record PunchKingScoreRequest(
		@Min(0)
		long score
) {
}

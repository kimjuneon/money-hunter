package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPlayerResourceAdjustRequest(
		@NotBlank(message = "조정 모드가 필요해요.")
		String mode,
		@NotNull(message = "조정값이 필요해요.")
		Long amount,
		String reason
) {
}

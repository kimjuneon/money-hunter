package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimRewardRequest(
		@NotBlank @Size(max = 120) String idempotencyKey
) {
}

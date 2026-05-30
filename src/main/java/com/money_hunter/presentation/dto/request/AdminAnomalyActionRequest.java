package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAnomalyActionRequest(
		@NotBlank String anomalyKey,
		@NotBlank String category,
		@NotBlank String userKey,
		@NotBlank String status,
		@Size(max = 1000) String note
) {
}

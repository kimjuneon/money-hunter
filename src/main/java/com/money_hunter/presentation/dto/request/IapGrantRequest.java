package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record IapGrantRequest(
		@NotBlank String orderId,
		@NotBlank String productId
) {
}

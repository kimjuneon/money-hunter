package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record AdminPlayerActionRequest(
		@Size(max = 500) String reason
) {
}

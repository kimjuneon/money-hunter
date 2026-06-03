package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminRookieEventSettingsRequest(
		@NotNull Boolean enabled,
		String reason
) {
}

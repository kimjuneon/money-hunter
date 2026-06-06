package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TossLoginRequest(
		@NotBlank String authorizationCode,
		String referrer,
		String entryPath
) {
}

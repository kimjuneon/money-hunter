package com.money_hunter.presentation.dto.response;

import java.time.Instant;

public record LoginSessionResponse(
		String accessToken,
		String tokenType,
		String userKey,
		Instant expiresAt
) {
}

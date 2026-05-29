package com.money_hunter.application.dto.response;

import java.time.Instant;

import com.money_hunter.domain.AdEventType;

public record AdRewardSessionResponse(
		AdEventType type,
		String sessionToken,
		Instant expiresAt
) {
}

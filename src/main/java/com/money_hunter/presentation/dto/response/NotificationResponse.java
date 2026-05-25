package com.money_hunter.presentation.dto.response;

import java.time.Instant;

public record NotificationResponse(
		Long id,
		String type,
		String title,
		String body,
		Instant sentAt
) {
}

package com.money_hunter.application.dto.response;

import java.time.Instant;

public record NotificationResponse(
		Long id,
		String type,
		String title,
		String body,
		Instant sentAt,
		Long settledGold,
		int levelGained,
		int skillPointsGained,
		long combatPowerGained
) {
}

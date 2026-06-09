package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record VipMembershipResponse(
		boolean active,
		Instant expiresAt,
		LocalDate lastDailyRewardDate,
		boolean dailyRewardAvailable,
		int dailyMissionSkipTickets,
		int priceWon
) {
}

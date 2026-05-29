package com.money_hunter.application.dto.response;

import java.time.Instant;

import com.money_hunter.domain.JobType;
import com.money_hunter.domain.Player;

public record AdminPlayerResponse(
		String userKey,
		JobType job,
		boolean onboardingRequired,
		long gold,
		int skillPoints,
		int level,
		int characterSlots,
		Instant autoHuntEndsAt,
		Instant boostEndsAt,
		boolean suspended,
		Instant suspendedAt,
		String suspensionReason,
		Instant createdAt,
		Instant updatedAt
) {
	public static AdminPlayerResponse from(Player player) {
		return new AdminPlayerResponse(
				player.getUserKey(),
				player.getJob(),
				!player.hasChosenJob(),
				player.getGold(),
				player.getSkillPoints(),
				player.getLevel(),
				player.getCharacterSlots(),
				player.getAutoHuntEndsAt(),
				player.getBoostEndsAt(),
				player.isSuspended(),
				player.getSuspendedAt(),
				player.getSuspensionReason(),
				player.getCreatedAt(),
				player.getUpdatedAt());
	}
}

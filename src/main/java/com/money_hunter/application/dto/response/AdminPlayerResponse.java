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
		long experience,
		int nextLevelExperience,
		int characterSlots,
		int friendInviteRewardCount,
		int defeatedMonsters,
		String currentMonsterKey,
		int currentMonsterHp,
		Instant lastSettledAt,
		Instant lastSkillPointAdClaimedAt,
		boolean tutorialRewardClaimed,
		boolean featureTutorialCompleted,
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
				player.getExperience(),
				player.getNextLevelExperience(),
				player.getCharacterSlots(),
				player.getFriendInviteRewardCount(),
				player.getDefeatedMonsters(),
				player.getCurrentMonsterKey(),
				player.getCurrentMonsterHp(),
				player.getLastSettledAt(),
				player.getLastSkillPointAdClaimedAt(),
				player.hasClaimedTutorialReward(),
				player.hasCompletedFeatureTutorial(),
				player.getAutoHuntEndsAt(),
				player.getBoostEndsAt(),
				player.isSuspended(),
				player.getSuspendedAt(),
				player.getSuspensionReason(),
				player.getCreatedAt(),
				player.getUpdatedAt());
	}
}

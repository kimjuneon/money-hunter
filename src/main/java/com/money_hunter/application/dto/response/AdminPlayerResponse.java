package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.util.Set;

import com.money_hunter.domain.JobType;
import com.money_hunter.domain.Player;

public record AdminPlayerResponse(
		String userKey,
		String gameProfileNickname,
		String adminNickname,
		boolean adminFavorite,
		boolean hiddenPetSkinsUnlocked,
		JobType job,
		boolean onboardingRequired,
		long gold,
		long cumulativeGoldEarned,
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
		Instant gameProfileUpdatedAt,
		boolean tutorialRewardClaimed,
		boolean featureTutorialCompleted,
			Instant autoHuntEndsAt,
			Instant boostEndsAt,
			boolean suspended,
			Instant suspendedAt,
			String suspensionReason,
			Instant createdAt,
			Instant lastAccessedAt,
			Instant updatedAt
	) {
	private static final Set<String> EASTER_EGG_PET_SKINS = Set.of(
			"EASTER_EGG_JUNEON",
			"EASTER_EGG_EULGIN",
			"EASTER_EGG_GYUDONG",
			"EASTER_EGG_MINGYU",
			"EASTER_EGG_JAESEO"
	);

	public static AdminPlayerResponse from(Player player) {
		return new AdminPlayerResponse(
				player.getUserKey(),
				player.getGameProfileNickname(),
				player.getAdminNickname(),
				player.isAdminFavorite(),
				hasHiddenPetSkinsUnlocked(player),
				player.getJob(),
				!player.hasChosenJob(),
				player.getGold(),
				player.getCumulativeGoldEarned(),
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
				player.getGameProfileUpdatedAt(),
				player.hasClaimedTutorialReward(),
				player.hasCompletedFeatureTutorial(),
					player.getAutoHuntEndsAt(),
					player.getBoostEndsAt(),
					player.isSuspended(),
					player.getSuspendedAt(),
					player.getSuspensionReason(),
					player.getCreatedAt(),
					player.getLastAccessedAt(),
					player.getUpdatedAt());
	}

	private static boolean hasHiddenPetSkinsUnlocked(Player player) {
		return player.ownedPetSkinKeyList().stream().anyMatch(EASTER_EGG_PET_SKINS::contains);
	}
}

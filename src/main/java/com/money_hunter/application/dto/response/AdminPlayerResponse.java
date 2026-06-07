package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.money_hunter.domain.JobType;
import com.money_hunter.domain.Player;
import com.money_hunter.domain.PlayerSkill;
import com.money_hunter.domain.SkillType;

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
		long combatPower,
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
			long totalSettledGold,
			long totalSettledWon,
			Instant lastSkillPointAdClaimedAt,
			Instant gameProfileUpdatedAt,
			boolean tutorialRewardClaimed,
			boolean featureTutorialCompleted,
				Instant autoHuntEndsAt,
				Instant benefitTabNewUserEnteredAt,
				boolean benefitTabNewUserPromotionEligible,
				boolean benefitTabNewUserPromotionRequested,
				Instant benefitTabNewUserPromotionResultCheckedAt,
				Instant benefitTabNewUserPromotionGrantedAt,
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
	private static final long MAX_COMBAT_POWER = 99_999_999L;
	private static final double COMBAT_POWER_LEVEL_SCALE = 40.0;
	private static final double COMBAT_POWER_SKILL_SCALE = 180.0;
	private static final int ROOKIE_EVENT_PET_SKILL_LEVEL = 15;
	private static final long ROOKIE_EVENT_REWARD_DURATION_DAYS = 30;
	private static final Set<SkillType> STAT_SKILLS = EnumSet.of(
			SkillType.STRENGTH,
			SkillType.DEXTERITY,
			SkillType.INTELLIGENCE,
			SkillType.LUCK
	);

	public static AdminPlayerResponse from(Player player, int goldPerTossPoint) {
		return from(player, goldPerTossPoint, Instant.now());
	}

	public static AdminPlayerResponse from(Player player, int goldPerTossPoint, Instant now) {
		long totalSettledGold = Math.max(0, player.getCumulativeGoldEarned());
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
				combatPower(player, now),
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
				totalSettledGold,
				totalSettledWon(totalSettledGold, goldPerTossPoint),
				player.getLastSkillPointAdClaimedAt(),
				player.getGameProfileUpdatedAt(),
				player.hasClaimedTutorialReward(),
				player.hasCompletedFeatureTutorial(),
					player.getAutoHuntEndsAt(),
					player.getBenefitTabNewUserEnteredAt(),
					player.isBenefitTabNewUserPromotionEligible(),
					player.hasBenefitTabNewUserPromotionExecutionKey(),
					player.getBenefitTabNewUserPromotionResultCheckedAt(),
					player.getBenefitTabNewUserPromotionGrantedAt(),
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

	private static long totalSettledWon(long totalSettledGold, int goldPerTossPoint) {
		if (goldPerTossPoint <= 0) {
			return 0;
		}
		return Math.max(0, totalSettledGold / goldPerTossPoint);
	}

	private static long combatPower(Player player, Instant now) {
		long totalSkillLevels = combatPowerSkillLevelTotal(player);
		totalSkillLevels += rookieEventPetCombatPowerSkillBonus(player, now);
		double levelRatio = Math.min(1.0, Math.max(0.0, player.getLevel() / COMBAT_POWER_LEVEL_SCALE));
		double spRatio = Math.min(1.0, Math.max(0.0, totalSkillLevels / COMBAT_POWER_SKILL_SCALE));
		double power = MAX_COMBAT_POWER
				* Math.pow(levelRatio, 0.5)
				* Math.pow(spRatio, 1.5);
		return Math.min(MAX_COMBAT_POWER, Math.max(0L, (long) Math.floor(power)));
	}

	private static long combatPowerSkillLevelTotal(Player player) {
		long sharedStatSkillLevel = player.getSkills().stream()
				.filter(skill -> STAT_SKILLS.contains(skill.getType()))
				.mapToLong(PlayerSkill::getLevel)
				.max()
				.orElse(0L);
		long nonStatSkillLevels = player.getSkills().stream()
				.filter(skill -> !STAT_SKILLS.contains(skill.getType()))
				.mapToLong(PlayerSkill::getLevel)
				.sum();
		return Math.max(0L, sharedStatSkillLevel + nonStatSkillLevels);
	}

	private static int rookieEventPetCombatPowerSkillBonus(Player player, Instant now) {
		Instant claimedAt = player.getRookieEventRewardClaimedAt();
		if (claimedAt == null || now == null) {
			return 0;
		}
		return now.isBefore(claimedAt.plusSeconds(ROOKIE_EVENT_REWARD_DURATION_DAYS * 86_400L))
				? ROOKIE_EVENT_PET_SKILL_LEVEL
				: 0;
	}
}

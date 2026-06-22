package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "game_economy_policy")
public class GameEconomyPolicy {
	public static final long SINGLETON_ID = 1L;

	@Id
	private Long id;

	@Column(name = "ad_revenue_per_reward_ad_won")
	private Integer adRevenuePerRewardAdWon;

	@Column(name = "gold_per_toss_point")
	private Integer goldPerTossPoint;

	@Column(name = "companion_price_won")
	private Integer companionPriceWon;

	@Column(name = "skill_point_pack_price_won")
	private Integer skillPointPackPriceWon;

	@Column(name = "skill_point_pack_amount")
	private Integer skillPointPackAmount;

	@Column(name = "friend_invite_reward_skill_points")
	private Integer friendInviteRewardSkillPoints;

	@Column(name = "friend_invite_limit")
	private Integer friendInviteLimit;

	@Column(name = "max_character_slots")
	private Integer maxCharacterSlots;

	@Column(name = "auto_hunt_ad_seconds")
	private Long autoHuntAdSeconds;

	@Column(name = "auto_hunt_ad_cooldown_seconds")
	private Long autoHuntAdCooldownSeconds;

	@Column(name = "max_ad_seconds")
	private Long maxAdSeconds;

	@Column(name = "dungeon_free_daily_limit")
	private Integer dungeonFreeDailyLimit;

	@Column(name = "dungeon_additional_daily_limit")
	private Integer dungeonAdditionalDailyLimit;

	@Column(name = "dungeon_reentry_cooldown_seconds")
	private Long dungeonReentryCooldownSeconds;

	@Column(name = "skill_point_ad_cooldown_seconds")
	private Long skillPointAdCooldownSeconds;

	@Column(name = "weekly_punch_king_max_gold_reward")
	private Long weeklyPunchKingMaxGoldReward;

	@Column(name = "weekly_punch_king_gold_reward_score_scale")
	private Long weeklyPunchKingGoldRewardScoreScale;

	@Column(name = "weekly_punch_king_base_skill_points")
	private Integer weeklyPunchKingBaseSkillPoints;

	@Column(name = "weekly_punch_king_skill_point_tier2_score")
	private Long weeklyPunchKingSkillPointTier2Score;

	@Column(name = "weekly_punch_king_skill_point_tier2_reward")
	private Integer weeklyPunchKingSkillPointTier2Reward;

	@Column(name = "weekly_punch_king_skill_point_tier3_score")
	private Long weeklyPunchKingSkillPointTier3Score;

	@Column(name = "weekly_punch_king_skill_point_tier3_reward")
	private Integer weeklyPunchKingSkillPointTier3Reward;

	@Column(name = "weekly_punch_king_skill_point_tier4_score")
	private Long weeklyPunchKingSkillPointTier4Score;

	@Column(name = "weekly_punch_king_skill_point_tier4_reward")
	private Integer weeklyPunchKingSkillPointTier4Reward;

	@Column(name = "reward_gold_threshold")
	private Long rewardGoldThreshold;

	@Column(name = "reward_point_amount")
	private Integer rewardPointAmount;

	@Column(name = "anomaly_limit_per_rule")
	private Integer anomalyLimitPerRule;

	@Column(name = "anomaly_ad_events_per_hour_warning")
	private Long anomalyAdEventsPerHourWarning;

	@Column(name = "anomaly_reward_claims_per_day_warning")
	private Long anomalyRewardClaimsPerDayWarning;

	@Column(name = "anomaly_gold_threshold_multiplier")
	private Long anomalyGoldThresholdMultiplier;

	@Column(name = "anomaly_skill_points_warning")
	private Integer anomalySkillPointsWarning;

	@Column(name = "anomaly_timer_grace_seconds")
	private Long anomalyTimerGraceSeconds;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	protected GameEconomyPolicy() {
	}

	public GameEconomyPolicy(Long id, Instant now) {
		this.id = id;
		this.updatedAt = now;
	}

	public Integer getAdRevenuePerRewardAdWon() {
		return adRevenuePerRewardAdWon;
	}

	public Integer getGoldPerTossPoint() {
		return goldPerTossPoint;
	}

	public Integer getCompanionPriceWon() {
		return companionPriceWon;
	}

	public Integer getSkillPointPackPriceWon() {
		return skillPointPackPriceWon;
	}

	public Integer getSkillPointPackAmount() {
		return skillPointPackAmount;
	}

	public Integer getFriendInviteRewardSkillPoints() {
		return friendInviteRewardSkillPoints;
	}

	public Integer getFriendInviteLimit() {
		return friendInviteLimit;
	}

	public Integer getMaxCharacterSlots() {
		return maxCharacterSlots;
	}

	public Long getAutoHuntAdSeconds() {
		return autoHuntAdSeconds;
	}

	public Long getAutoHuntAdCooldownSeconds() {
		return autoHuntAdCooldownSeconds;
	}

	public Long getMaxAdSeconds() {
		return maxAdSeconds;
	}

	public Integer getDungeonFreeDailyLimit() {
		return dungeonFreeDailyLimit;
	}

	public Integer getDungeonAdditionalDailyLimit() {
		return dungeonAdditionalDailyLimit;
	}

	public Long getDungeonReentryCooldownSeconds() {
		return dungeonReentryCooldownSeconds;
	}

	public Long getSkillPointAdCooldownSeconds() {
		return skillPointAdCooldownSeconds;
	}

	public Long getWeeklyPunchKingMaxGoldReward() {
		return weeklyPunchKingMaxGoldReward;
	}

	public Long getWeeklyPunchKingGoldRewardScoreScale() {
		return weeklyPunchKingGoldRewardScoreScale;
	}

	public Integer getWeeklyPunchKingBaseSkillPoints() {
		return weeklyPunchKingBaseSkillPoints;
	}

	public Long getWeeklyPunchKingSkillPointTier2Score() {
		return weeklyPunchKingSkillPointTier2Score;
	}

	public Integer getWeeklyPunchKingSkillPointTier2Reward() {
		return weeklyPunchKingSkillPointTier2Reward;
	}

	public Long getWeeklyPunchKingSkillPointTier3Score() {
		return weeklyPunchKingSkillPointTier3Score;
	}

	public Integer getWeeklyPunchKingSkillPointTier3Reward() {
		return weeklyPunchKingSkillPointTier3Reward;
	}

	public Long getWeeklyPunchKingSkillPointTier4Score() {
		return weeklyPunchKingSkillPointTier4Score;
	}

	public Integer getWeeklyPunchKingSkillPointTier4Reward() {
		return weeklyPunchKingSkillPointTier4Reward;
	}

	public Long getRewardGoldThreshold() {
		return rewardGoldThreshold;
	}

	public Integer getRewardPointAmount() {
		return rewardPointAmount;
	}

	public Integer getAnomalyLimitPerRule() {
		return anomalyLimitPerRule;
	}

	public Long getAnomalyAdEventsPerHourWarning() {
		return anomalyAdEventsPerHourWarning;
	}

	public Long getAnomalyRewardClaimsPerDayWarning() {
		return anomalyRewardClaimsPerDayWarning;
	}

	public Long getAnomalyGoldThresholdMultiplier() {
		return anomalyGoldThresholdMultiplier;
	}

	public Integer getAnomalySkillPointsWarning() {
		return anomalySkillPointsWarning;
	}

	public Long getAnomalyTimerGraceSeconds() {
		return anomalyTimerGraceSeconds;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(String key, Number value, Instant now) {
		switch (key) {
			case "adRevenuePerRewardAdWon" -> this.adRevenuePerRewardAdWon = value.intValue();
			case "goldPerTossPoint" -> this.goldPerTossPoint = value.intValue();
			case "companionPriceWon" -> this.companionPriceWon = value.intValue();
			case "skillPointPackPriceWon" -> this.skillPointPackPriceWon = value.intValue();
			case "skillPointPackAmount" -> this.skillPointPackAmount = value.intValue();
			case "friendInviteRewardSkillPoints" -> this.friendInviteRewardSkillPoints = value.intValue();
			case "friendInviteLimit" -> this.friendInviteLimit = value.intValue();
				case "maxCharacterSlots" -> this.maxCharacterSlots = value.intValue();
				case "autoHuntAdSeconds" -> this.autoHuntAdSeconds = value.longValue();
				case "autoHuntAdCooldownSeconds" -> this.autoHuntAdCooldownSeconds = value.longValue();
				case "maxAdSeconds" -> this.maxAdSeconds = value.longValue();
			case "dungeonFreeDailyLimit" -> this.dungeonFreeDailyLimit = value.intValue();
			case "dungeonAdditionalDailyLimit" -> this.dungeonAdditionalDailyLimit = value.intValue();
			case "dungeonReentryCooldownSeconds" -> this.dungeonReentryCooldownSeconds = value.longValue();
			case "skillPointAdCooldownSeconds" -> this.skillPointAdCooldownSeconds = value.longValue();
			case "weeklyPunchKingMaxGoldReward" -> this.weeklyPunchKingMaxGoldReward = value.longValue();
			case "weeklyPunchKingGoldRewardScoreScale" -> this.weeklyPunchKingGoldRewardScoreScale = value.longValue();
			case "weeklyPunchKingBaseSkillPoints" -> this.weeklyPunchKingBaseSkillPoints = value.intValue();
			case "weeklyPunchKingSkillPointTier2Score" -> this.weeklyPunchKingSkillPointTier2Score = value.longValue();
			case "weeklyPunchKingSkillPointTier2Reward" -> this.weeklyPunchKingSkillPointTier2Reward = value.intValue();
			case "weeklyPunchKingSkillPointTier3Score" -> this.weeklyPunchKingSkillPointTier3Score = value.longValue();
			case "weeklyPunchKingSkillPointTier3Reward" -> this.weeklyPunchKingSkillPointTier3Reward = value.intValue();
			case "weeklyPunchKingSkillPointTier4Score" -> this.weeklyPunchKingSkillPointTier4Score = value.longValue();
			case "weeklyPunchKingSkillPointTier4Reward" -> this.weeklyPunchKingSkillPointTier4Reward = value.intValue();
			case "rewardGoldThreshold" -> this.rewardGoldThreshold = value.longValue();
			case "rewardPointAmount" -> this.rewardPointAmount = value.intValue();
			case "anomalyLimitPerRule" -> this.anomalyLimitPerRule = value.intValue();
			case "anomalyAdEventsPerHourWarning" -> this.anomalyAdEventsPerHourWarning = value.longValue();
			case "anomalyRewardClaimsPerDayWarning" -> this.anomalyRewardClaimsPerDayWarning = value.longValue();
			case "anomalyGoldThresholdMultiplier" -> this.anomalyGoldThresholdMultiplier = value.longValue();
			case "anomalySkillPointsWarning" -> this.anomalySkillPointsWarning = value.intValue();
			case "anomalyTimerGraceSeconds" -> this.anomalyTimerGraceSeconds = value.longValue();
			default -> throw new IllegalArgumentException("Unknown policy key.");
		}
		this.updatedAt = now;
	}

	public void reset(String key, Instant now) {
		switch (key) {
			case "adRevenuePerRewardAdWon" -> this.adRevenuePerRewardAdWon = null;
			case "goldPerTossPoint" -> this.goldPerTossPoint = null;
			case "companionPriceWon" -> this.companionPriceWon = null;
			case "skillPointPackPriceWon" -> this.skillPointPackPriceWon = null;
			case "skillPointPackAmount" -> this.skillPointPackAmount = null;
			case "friendInviteRewardSkillPoints" -> this.friendInviteRewardSkillPoints = null;
			case "friendInviteLimit" -> this.friendInviteLimit = null;
				case "maxCharacterSlots" -> this.maxCharacterSlots = null;
				case "autoHuntAdSeconds" -> this.autoHuntAdSeconds = null;
				case "autoHuntAdCooldownSeconds" -> this.autoHuntAdCooldownSeconds = null;
				case "maxAdSeconds" -> this.maxAdSeconds = null;
			case "dungeonFreeDailyLimit" -> this.dungeonFreeDailyLimit = null;
			case "dungeonAdditionalDailyLimit" -> this.dungeonAdditionalDailyLimit = null;
			case "dungeonReentryCooldownSeconds" -> this.dungeonReentryCooldownSeconds = null;
			case "skillPointAdCooldownSeconds" -> this.skillPointAdCooldownSeconds = null;
			case "weeklyPunchKingMaxGoldReward" -> this.weeklyPunchKingMaxGoldReward = null;
			case "weeklyPunchKingGoldRewardScoreScale" -> this.weeklyPunchKingGoldRewardScoreScale = null;
			case "weeklyPunchKingBaseSkillPoints" -> this.weeklyPunchKingBaseSkillPoints = null;
			case "weeklyPunchKingSkillPointTier2Score" -> this.weeklyPunchKingSkillPointTier2Score = null;
			case "weeklyPunchKingSkillPointTier2Reward" -> this.weeklyPunchKingSkillPointTier2Reward = null;
			case "weeklyPunchKingSkillPointTier3Score" -> this.weeklyPunchKingSkillPointTier3Score = null;
			case "weeklyPunchKingSkillPointTier3Reward" -> this.weeklyPunchKingSkillPointTier3Reward = null;
			case "weeklyPunchKingSkillPointTier4Score" -> this.weeklyPunchKingSkillPointTier4Score = null;
			case "weeklyPunchKingSkillPointTier4Reward" -> this.weeklyPunchKingSkillPointTier4Reward = null;
			case "rewardGoldThreshold" -> this.rewardGoldThreshold = null;
			case "rewardPointAmount" -> this.rewardPointAmount = null;
			case "anomalyLimitPerRule" -> this.anomalyLimitPerRule = null;
			case "anomalyAdEventsPerHourWarning" -> this.anomalyAdEventsPerHourWarning = null;
			case "anomalyRewardClaimsPerDayWarning" -> this.anomalyRewardClaimsPerDayWarning = null;
			case "anomalyGoldThresholdMultiplier" -> this.anomalyGoldThresholdMultiplier = null;
			case "anomalySkillPointsWarning" -> this.anomalySkillPointsWarning = null;
			case "anomalyTimerGraceSeconds" -> this.anomalyTimerGraceSeconds = null;
			default -> throw new IllegalArgumentException("Unknown policy key.");
		}
		this.updatedAt = now;
	}
}

package com.money_hunter.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "players")
public class Player {

	private static final long GOLD_MICROS = 1_000_000L;

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(nullable = false, unique = true, length = 120)
	private String userKey;

    @Enumerated(EnumType.STRING)
	private JobType job;

    @Column(nullable = false)
	private int characterSlots = 1;

    @Column(nullable = false)
	private long gold = 0;

	@Column(nullable = false)
	private long cumulativeGoldEarned = 0;

    @Column(nullable = false)
	private int skillPoints = 0;

	@Column(nullable = false)
	private int friendInviteRewardCount = 0;

    @Column(nullable = false)
	private int level = 1;

    @Column(nullable = false)
	private long experience = 0;

    @Column(nullable = false, length = 40)
	private String currentMonsterKey = "BOSS_ROCK";

    @Column(nullable = false)
	private int currentMonsterHp = 120;

    @Column(nullable = false)
	private int defeatedMonsters = 0;

	@Column(nullable = false)
	private long hitGoldRemainderMicros = 0;

	@Column(nullable = false)
	private long defeatGoldRemainderMicros = 0;

    private Instant autoHuntEndsAt;

    private Instant autoHuntEndNotifiedAt;

	private Instant autoHuntEndSmartMessageAttemptedAt;

	private Long autoHuntEndSettledGold;

	@Column(nullable = false)
	private int autoHuntEndLevelGain = 0;

	@Column(nullable = false)
	private int autoHuntEndSkillPointGain = 0;

	@Column(nullable = false)
	private long autoHuntEndCombatPowerGain = 0;

	private Instant lastAutoHuntAdClaimedAt;

	private Instant lastSkillPointAdClaimedAt;

	private Instant tutorialRewardClaimedAt;

	private Instant featureTutorialCompletedAt;

	private Instant suspendedAt;

	@Column(length = 500)
	private String suspensionReason;

	@Column(length = 80)
	private String gameProfileNickname;

	private Instant gameProfileUpdatedAt;

	@Column(length = 80)
	private String adminNickname;

	@Column(nullable = false)
	private boolean adminFavorite = false;

	private Instant benefitTabNewUserEnteredAt;

	@Column(length = 220)
	private String benefitTabNewUserPromotionExecutionKey;

	private Instant benefitTabNewUserPromotionResultCheckedAt;

	private Instant benefitTabNewUserPromotionGrantedAt;

	@Column(nullable = false, length = 600)
	private String ownedPetSkinKeys = "FIRE_FOX,ICE";

	@Column(nullable = false, length = 40)
	private String petOneSkinKey = "FIRE_FOX";

	@Column(nullable = false, length = 40)
	private String petTwoSkinKey = "ICE";

	private Instant rookieEventStartedAt;

	private Instant rookieEventCompletedAt;

	private Instant rookieEventRewardClaimedAt;

	@Column(nullable = false)
	private int rookieEventCompletedDays = 0;

	@Column(nullable = false)
	private int rookieEventRewardedDays = 0;

	private LocalDate rookieEventCurrentDate;

	private LocalDate rookieEventLastCompletedDate;

	@Column(nullable = false)
	private long rookieEventDailyHuntMillis = 0;

	@Column(nullable = false)
	private int rookieEventDailyMonsters = 0;

	@Column(nullable = false)
	private long rookieEventDailyGold = 0;

	@Column(nullable = false)
	private int rookieEventDailySettlements = 0;

	@Column(nullable = false)
	private int rookieEventDailySkillPointsSpent = 0;

	@Column(nullable = false)
	private boolean rookieEventDailySkillPointHelpClaimed = false;

	@Column(nullable = false)
	private boolean rookieEventDailyHomeShortcutReturned = false;

	private Instant rookieEventMissionNotificationAgreedAt;

	private LocalDate rookieEventMissionMessageSentDate;

	@Column(nullable = false)
	private int rookieEventMissionMessageSentDay = 0;

	private Instant dormantSpRewardStreakAccessedAt;

	@Column(nullable = false)
	private int dormantSpRewardSentStage = 0;

	private Instant dormantSpRewardLastSentAt;

	private LocalDate dungeonExploreAvailableNotificationDate;

	private Integer dungeonExploreAvailableNotificationRunCount;

	private Instant dungeonExploreAvailableNotificationSentAt;

	private Instant battleReadyDailyStreakAccessedAt;

	@Column(nullable = false)
	private int battleReadyDailySentStage = 0;

	private Instant battleReadyDailyLastSentAt;

	@Column(nullable = false)
	private int dungeonCouponCount = 0;

	@Column(nullable = false)
	private long dungeonCouponHuntMillis = 0;

	private LocalDate dungeonRunCountDate;

	@Column(nullable = false)
	private int dungeonRunCount = 0;

	private Instant dungeonNextAvailableAt;

	@Column(nullable = false)
	private int bossRaidTicketCount = 0;

	@Column(nullable = false)
	private int dailyMissionCycle = 1;

	private LocalDate dailyMissionCurrentDate;

	private LocalDate dailyMissionLastCompletedDate;

	@Column(nullable = false)
	private int dailyMissionCompletedDays = 0;

	@Column(nullable = false)
	private long dailyMissionDailyHuntMillis = 0;

	@Column(nullable = false)
	private int dailyMissionDailyDungeonRuns = 0;

	private Instant vipExpiresAt;

	private LocalDate vipLastDailyRewardDate;

	private Instant adventureMiniGameEntryStartedAt;

	private LocalDate adventureMiniGameCompletedDate;

	private LocalDate weeklyPunchKingWeekStartDate;

	@Column(nullable = false)
	private long weeklyPunchKingBestScore = 0;

	@Column(nullable = false)
	private long weeklyPunchKingRewardedGold = 0;

	@Column(nullable = false)
	private int weeklyPunchKingRewardedSkillPoints = 0;

	@Column(nullable = false)
	private Instant lastSettledAt;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant lastAccessedAt;

    @Column(nullable = false)
	private Instant updatedAt;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PlayerSkill> skills = new ArrayList<>();

	protected Player() {
	}

	public Player(String userKey, Instant now) {
		this.userKey = userKey;
		this.lastSettledAt = now;
		this.createdAt = now;
		this.lastAccessedAt = now;
		this.updatedAt = now;
	}

    public void chooseJob(JobType job) {
		this.job = job;
		touch();
	}

    public void purchaseCharacterSlot(int maxCharacterSlots) {
		if (characterSlots >= maxCharacterSlots) {
			throw new IllegalStateException("동료 펫을 모두 데려왔어요.");
		}
		this.characterSlots += 1;
		touch();
	}

	public int removePetAndRefundSkillPoints() {
		if (characterSlots <= 1) {
			throw new IllegalStateException("제거할 동료 펫이 없어요.");
		}
		SkillType removedSkillType = characterSlots >= 3
				? SkillType.PET_AQUA_ATTACK
				: SkillType.PET_FLARE_ATTACK;
		PlayerSkill removedPetSkill = getOrCreateSkill(removedSkillType);
		int refundSkillPoints = removedPetSkill.getLevel();
		if (refundSkillPoints > 0) {
			this.skillPoints += refundSkillPoints;
			removedPetSkill.resetLevel();
		}
		this.characterSlots -= 1;
		touch();
		return refundSkillPoints;
	}

	public void addGold(long amount) {
		this.gold += amount;
		touch();
	}

	public void adjustGold(long amount) {
		this.gold = Math.max(0, this.gold + amount);
		touch();
	}

	public void setGold(long amount) {
		this.gold = Math.max(0, amount);
		this.hitGoldRemainderMicros = 0;
		this.defeatGoldRemainderMicros = 0;
		touch();
	}

	public long collectHitGold(long rewardMicros) {
		this.hitGoldRemainderMicros += rewardMicros;
		long wholeGold = collectWholeHitGold();
		if (wholeGold == 0) {
			touch();
		}
		return wholeGold;
	}

	public void reserveDefeatGold(long rewardMicros) {
		this.defeatGoldRemainderMicros += rewardMicros;
		touch();
	}

	public long collectDefeatGold() {
		long wholeGold = defeatGoldRemainderMicros / GOLD_MICROS;
		if (wholeGold > 0) {
			this.defeatGoldRemainderMicros -= wholeGold * GOLD_MICROS;
			this.gold += wholeGold;
			this.cumulativeGoldEarned += wholeGold;
			touch();
		}
		return wholeGold;
	}

	public long collectCombatGold(long hitRewardMicros, long defeatRewardMicros) {
		this.hitGoldRemainderMicros += hitRewardMicros;
		this.defeatGoldRemainderMicros += defeatRewardMicros;
		long hitGold = collectWholeHitGold();
		long defeatGold = collectDefeatGold();
		if (hitGold + defeatGold == 0) {
			touch();
		}
		return hitGold + defeatGold;
	}

	private long collectWholeHitGold() {
		long wholeGold = hitGoldRemainderMicros / GOLD_MICROS;
		if (wholeGold > 0) {
			this.hitGoldRemainderMicros -= wholeGold * GOLD_MICROS;
			this.gold += wholeGold;
			this.cumulativeGoldEarned += wholeGold;
			touch();
		}
		return wholeGold;
	}

	public void spendGold(long amount) {
		if (gold < amount) {
			throw new IllegalStateException("Not enough gold.");
		}
		this.gold -= amount;
		touch();
	}

    public void addSkillPoint() {
		this.skillPoints += 1;
		touch();
	}

	public void markSkillPointAdClaimed(Instant now) {
		this.lastSkillPointAdClaimedAt = now;
		touch();
	}

	public void addSkillPoints(int amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("Skill point amount must be positive.");
		}
		this.skillPoints += amount;
		touch();
	}

	public void adjustSkillPoints(int amount) {
		this.skillPoints = Math.max(0, this.skillPoints + amount);
		touch();
	}

	public void setSkillPoints(int amount) {
		this.skillPoints = Math.max(0, amount);
		touch();
	}

	public void claimFriendInviteReward(int inviteLimit, int rewardSkillPoints) {
		claimFriendInviteReward(inviteLimit, rewardSkillPoints, 1);
	}

	public int claimFriendInviteReward(int inviteLimit, int rewardSkillPoints, int completedInvites) {
		if (friendInviteRewardCount >= inviteLimit) {
			throw new IllegalStateException("친구 초대 보상을 모두 받았어요.");
		}
		if (rewardSkillPoints < 1) {
			throw new IllegalArgumentException("Friend invite reward must be positive.");
		}
		if (completedInvites < 1) {
			throw new IllegalArgumentException("Friend invite count must be positive.");
		}
		int claimCount = Math.min(completedInvites, inviteLimit - friendInviteRewardCount);
		this.friendInviteRewardCount += claimCount;
		this.skillPoints += rewardSkillPoints * claimCount;
		touch();
		return claimCount;
	}

	public void startRookieEvent(Instant now, LocalDate today) {
		if (rookieEventStartedAt != null) {
			return;
		}
		this.rookieEventStartedAt = now;
		resetRookieEventDailyProgress(today);
		touch();
	}

	public void ensureRookieEventDay(LocalDate today) {
		if (today == null) {
			return;
		}
		if (!today.equals(rookieEventCurrentDate)) {
			resetRookieEventDailyProgress(today);
		}
	}

	public boolean isRookieEventRewardClaimed() {
		return rookieEventRewardClaimedAt != null;
	}

	public boolean completedRookieEventDayToday(LocalDate today) {
		return today != null && today.equals(rookieEventLastCompletedDate);
	}

	public void addRookieEventCombatProgress(long huntMillis, long gold, int monsters) {
		this.rookieEventDailyHuntMillis += Math.max(0, huntMillis);
		this.rookieEventDailyGold += Math.max(0, gold);
		this.rookieEventDailyMonsters += Math.max(0, monsters);
		touch();
	}

	public void addRookieEventTossPointClaim() {
		this.rookieEventDailySettlements += 1;
		touch();
	}

	public void addRookieEventSkillPointSpent() {
		this.rookieEventDailySkillPointsSpent += 1;
		touch();
	}

	public void claimRookieEventDailySkillPointHelp() {
		if (rookieEventDailySkillPointHelpClaimed) {
			throw new IllegalStateException("이미 이벤트 SP를 받았어요.");
		}
		this.skillPoints += 1;
		this.rookieEventDailySkillPointHelpClaimed = true;
		touch();
	}

	public void markRookieEventHomeShortcutReturned() {
		if (!rookieEventDailyHomeShortcutReturned) {
			this.rookieEventDailyHomeShortcutReturned = true;
			touch();
		}
	}

	public void markBenefitTabNewUserEntry(Instant now) {
		if (benefitTabNewUserEnteredAt == null) {
			this.benefitTabNewUserEnteredAt = now;
			touch();
		}
	}

	public Instant getBenefitTabNewUserEnteredAt() {
		return benefitTabNewUserEnteredAt;
	}

	public String getBenefitTabNewUserPromotionExecutionKey() {
		return benefitTabNewUserPromotionExecutionKey;
	}

	public Instant getBenefitTabNewUserPromotionResultCheckedAt() {
		return benefitTabNewUserPromotionResultCheckedAt;
	}

	public Instant getBenefitTabNewUserPromotionGrantedAt() {
		return benefitTabNewUserPromotionGrantedAt;
	}

	public void resetBenefitTabNewUserPromotionForTest(Instant now) {
		this.benefitTabNewUserEnteredAt = now;
		this.benefitTabNewUserPromotionExecutionKey = null;
		this.benefitTabNewUserPromotionResultCheckedAt = null;
		this.benefitTabNewUserPromotionGrantedAt = null;
		touch();
	}

	public boolean isBenefitTabNewUserPromotionEligible() {
		return benefitTabNewUserEnteredAt != null && benefitTabNewUserPromotionGrantedAt == null;
	}

	public boolean hasBenefitTabNewUserPromotionExecutionKey() {
		return benefitTabNewUserPromotionExecutionKey != null && !benefitTabNewUserPromotionExecutionKey.isBlank();
	}

	public void markBenefitTabNewUserPromotionExecutionKey(String executionKey, Instant now) {
		this.benefitTabNewUserPromotionExecutionKey = executionKey;
		this.benefitTabNewUserPromotionResultCheckedAt = now;
		touch();
	}

	public void markBenefitTabNewUserPromotionResult(String result, Instant now) {
		String normalized = result == null ? "" : result.trim().toUpperCase();
		this.benefitTabNewUserPromotionResultCheckedAt = now;
		if ("SUCCESS".equals(normalized)) {
			this.benefitTabNewUserPromotionGrantedAt = now;
		}
		touch();
	}

	public void completeRookieEventDay(LocalDate today, Instant now, int maxEventDays) {
		if (today == null || completedRookieEventDayToday(today) || rookieEventCompletedDays >= maxEventDays) {
			return;
		}
		this.rookieEventCompletedDays += 1;
		this.rookieEventLastCompletedDate = today;
		if (rookieEventCompletedDays >= maxEventDays) {
			this.rookieEventCompletedAt = now;
		}
		touch();
	}

	public void markRookieEventDailyRewarded(int day) {
		this.rookieEventRewardedDays = Math.max(this.rookieEventRewardedDays, day);
		touch();
	}

	public void claimRookieEventReward(Instant now) {
		if (rookieEventRewardClaimedAt != null) {
			return;
		}
		this.rookieEventRewardClaimedAt = now;
		touch();
	}

	public boolean rookieEventMissionMessageSentToday(LocalDate today, int day) {
		return today != null
				&& today.equals(rookieEventMissionMessageSentDate)
				&& rookieEventMissionMessageSentDay == day;
	}

	public void markRookieEventMissionMessageSent(LocalDate today, int day) {
		this.rookieEventMissionMessageSentDate = today;
		this.rookieEventMissionMessageSentDay = Math.max(1, day);
		touch();
	}

	public void markRookieEventMissionNotificationAgreed(Instant agreedAt) {
		this.rookieEventMissionNotificationAgreedAt = agreedAt;
		touch();
	}

	public int dormantSpRewardSentStageForCurrentStreak() {
		if (dormantSpRewardStreakAccessedAt == null || !dormantSpRewardStreakAccessedAt.equals(lastAccessedAt)) {
			return 0;
		}
		return Math.max(0, dormantSpRewardSentStage);
	}

	public void markDormantSpRewardSent(Instant streakAccessedAt, int sentStage, Instant sentAt) {
		this.dormantSpRewardStreakAccessedAt = streakAccessedAt;
		this.dormantSpRewardSentStage = Math.max(0, sentStage);
		this.dormantSpRewardLastSentAt = sentAt;
		touch();
	}

	public boolean dungeonExploreAvailableNotificationSentForCurrentRun(LocalDate today) {
		return today != null
				&& today.equals(dungeonExploreAvailableNotificationDate)
				&& dungeonExploreAvailableNotificationRunCount != null
				&& dungeonExploreAvailableNotificationRunCount == Math.max(0, dungeonRunCount);
	}

	public void markDungeonExploreAvailableNotificationSent(LocalDate today, Instant sentAt) {
		this.dungeonExploreAvailableNotificationDate = today;
		this.dungeonExploreAvailableNotificationRunCount = Math.max(0, dungeonRunCount);
		this.dungeonExploreAvailableNotificationSentAt = sentAt;
		touch();
	}

	public int battleReadyDailySentStageForCurrentStreak() {
		if (battleReadyDailyStreakAccessedAt == null || !battleReadyDailyStreakAccessedAt.equals(lastAccessedAt)) {
			return 0;
		}
		return Math.max(0, battleReadyDailySentStage);
	}

	public void markBattleReadyDailySent(Instant streakAccessedAt, int sentStage, Instant sentAt) {
		this.battleReadyDailyStreakAccessedAt = streakAccessedAt;
		this.battleReadyDailySentStage = Math.max(0, sentStage);
		this.battleReadyDailyLastSentAt = sentAt;
		touch();
	}

	public int addDungeonCouponHuntMillis(long huntMillis, long requiredMillis) {
		if (huntMillis < 1 || requiredMillis < 1) {
			return 0;
		}
		long totalMillis = Math.max(0, this.dungeonCouponHuntMillis) + huntMillis;
		int earnedCoupons = (int) Math.min(Integer.MAX_VALUE, totalMillis / requiredMillis);
		this.dungeonCouponHuntMillis = totalMillis % requiredMillis;
		if (earnedCoupons > 0) {
			this.dungeonCouponCount = Math.max(0, this.dungeonCouponCount) + earnedCoupons;
		}
		touch();
		return earnedCoupons;
	}

	public void addDungeonEntryHuntProgress(long huntMillis, long requiredMillis) {
		if (huntMillis < 1 || requiredMillis < 1) {
			return;
		}
		this.dungeonCouponHuntMillis = Math.min(
				requiredMillis,
				Math.max(0, this.dungeonCouponHuntMillis) + huntMillis);
		touch();
	}

	public long dungeonEntryHuntProgressMillis(long requiredMillis) {
		if (requiredMillis < 1) {
			return 0;
		}
		return Math.min(requiredMillis, Math.max(0, this.dungeonCouponHuntMillis));
	}

	public boolean dungeonEntryHuntRequirementCompleted(long requiredMillis) {
		return dungeonEntryHuntProgressMillis(requiredMillis) >= requiredMillis;
	}

	public void addDungeonCoupons(int amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("Dungeon coupon amount must be positive.");
		}
		this.dungeonCouponCount = Math.max(0, this.dungeonCouponCount) + amount;
		touch();
	}

	public void spendDungeonCoupon() {
		if (dungeonCouponCount < 1) {
			throw new IllegalStateException("사용할 던전 쿠폰이 없어요.");
		}
		this.dungeonCouponCount -= 1;
		touch();
	}

	public void resetDungeonRunCountIfNewDay(LocalDate today) {
		if (today == null) {
			return;
		}
		if (!today.equals(dungeonRunCountDate)) {
			this.dungeonRunCountDate = today;
			this.dungeonRunCount = 0;
			this.dungeonCouponHuntMillis = 0;
			this.dungeonNextAvailableAt = null;
			this.dungeonExploreAvailableNotificationDate = null;
			this.dungeonExploreAvailableNotificationRunCount = null;
			this.dungeonExploreAvailableNotificationSentAt = null;
			touch();
		}
	}

	public void enterDungeon(Instant now, LocalDate today, int dailyLimit, Duration cooldown) {
		if (now == null || today == null || dailyLimit < 1 || cooldown == null || cooldown.isNegative()) {
			throw new IllegalArgumentException("Dungeon entry options are invalid.");
		}
		resetDungeonRunCountIfNewDay(today);
		if (dungeonRunCount >= dailyLimit) {
			throw new IllegalStateException("오늘 던전 입장 횟수를 모두 사용했어요.");
		}
		if (dungeonNextAvailableAt != null && dungeonNextAvailableAt.isAfter(now)) {
			throw new IllegalStateException("던전 재입장 시간이 아직 남았어요.");
		}
		this.dungeonRunCountDate = today;
		this.dungeonRunCount += 1;
		this.dungeonNextAvailableAt = dungeonRunCount >= dailyLimit ? null : now.plus(cooldown);
		touch();
	}

	public void addBossRaidTickets(int amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("Boss raid ticket amount must be positive.");
		}
		this.bossRaidTicketCount = Math.max(0, this.bossRaidTicketCount) + amount;
		touch();
	}

	public void ensureDailyMissionDay(LocalDate today) {
		if (today == null) {
			return;
		}
		if (!today.equals(dailyMissionCurrentDate)) {
			this.dailyMissionCurrentDate = today;
			this.dailyMissionDailyHuntMillis = 0;
			this.dailyMissionDailyDungeonRuns = 0;
			touch();
		}
	}

	public boolean completedDailyMissionToday(LocalDate today) {
		return today != null && today.equals(dailyMissionLastCompletedDate);
	}

	public void addDailyMissionHuntProgress(long huntMillis, long requiredMillis) {
		if (huntMillis < 1 || requiredMillis < 1) {
			return;
		}
		this.dailyMissionDailyHuntMillis = Math.min(
				requiredMillis,
				Math.max(0, this.dailyMissionDailyHuntMillis) + huntMillis);
		touch();
	}

	public void addDailyMissionDungeonRun(LocalDate today, int requiredRuns) {
		ensureDailyMissionDay(today);
		this.dailyMissionDailyDungeonRuns = Math.min(
				Math.max(1, requiredRuns),
				Math.max(0, this.dailyMissionDailyDungeonRuns) + 1);
		touch();
	}

	public boolean dailyMissionReady(long requiredMillis, int requiredDungeonRuns) {
		return dailyMissionDailyHuntMillis >= requiredMillis
				&& dailyMissionDailyDungeonRuns >= requiredDungeonRuns;
	}

	public int completeDailyMission(LocalDate today, int maxDays) {
		if (today == null || completedDailyMissionToday(today)) {
			return dailyMissionCompletedDays;
		}
		this.dailyMissionLastCompletedDate = today;
		this.dailyMissionCompletedDays = Math.min(maxDays, Math.max(0, dailyMissionCompletedDays) + 1);
		touch();
		return dailyMissionCompletedDays;
	}

	public void startNextDailyMissionCycle(LocalDate today) {
		this.dailyMissionCycle = Math.max(1, dailyMissionCycle) + 1;
		this.dailyMissionCompletedDays = 0;
		this.dailyMissionLastCompletedDate = null;
		this.dailyMissionCurrentDate = today;
		this.dailyMissionDailyHuntMillis = 0;
		this.dailyMissionDailyDungeonRuns = 0;
		touch();
	}

	public void activateVip(Instant now, Duration duration) {
		if (now == null || duration == null || duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException("VIP duration is invalid.");
		}
		Instant base = vipExpiresAt != null && vipExpiresAt.isAfter(now) ? vipExpiresAt : now;
		this.vipExpiresAt = base.plus(duration);
		touch();
	}

	public boolean isVipActive(Instant now) {
		return vipExpiresAt != null && now != null && vipExpiresAt.isAfter(now);
	}

	public boolean canClaimVipDailyReward(LocalDate today, Instant now) {
		return isVipActive(now)
				&& today != null
				&& (vipLastDailyRewardDate == null || vipLastDailyRewardDate.isBefore(today));
	}

	public void markVipDailyRewardClaimed(LocalDate today) {
		if (today != null && (vipLastDailyRewardDate == null || vipLastDailyRewardDate.isBefore(today))) {
			this.vipLastDailyRewardDate = today;
		}
		touch();
	}

	public boolean adventureMiniGameCompletedToday(LocalDate today) {
		return today != null && today.equals(adventureMiniGameCompletedDate);
	}

	public void startAdventureMiniGame(Instant now, LocalDate today, long entryCostGold) {
		if (now == null || today == null) {
			throw new IllegalArgumentException("Mini game entry options are invalid.");
		}
		if (adventureMiniGameCompletedToday(today)) {
			throw new IllegalStateException("오늘 미니게임 보상은 이미 받았어요.");
		}
		spendGold(entryCostGold);
		this.adventureMiniGameEntryStartedAt = now;
		touch();
	}

	public boolean adventureMiniGameEntryActive(LocalDate today, Instant now, Duration ttl) {
		if (today == null || now == null || ttl == null || ttl.isNegative() || adventureMiniGameEntryStartedAt == null) {
			return false;
		}
		return LocalDate.ofInstant(adventureMiniGameEntryStartedAt, java.time.ZoneId.of("Asia/Seoul")).equals(today)
				&& adventureMiniGameEntryStartedAt.plus(ttl).isAfter(now);
	}

	public void completeAdventureMiniGame(LocalDate today) {
		if (today == null) {
			return;
		}
		this.adventureMiniGameCompletedDate = today;
		this.adventureMiniGameEntryStartedAt = null;
		touch();
	}

	public void ensureWeeklyPunchKingWeek(LocalDate weekStartDate) {
		if (weekStartDate == null) {
			return;
		}
		if (!weekStartDate.equals(weeklyPunchKingWeekStartDate)) {
			this.weeklyPunchKingWeekStartDate = weekStartDate;
			this.weeklyPunchKingBestScore = 0;
			this.weeklyPunchKingRewardedGold = 0;
			this.weeklyPunchKingRewardedSkillPoints = 0;
			touch();
		}
	}

	public void recordWeeklyPunchKingReward(long score, long rewardedGold, int rewardedSkillPoints) {
		this.weeklyPunchKingBestScore = Math.max(Math.max(0, score), weeklyPunchKingBestScore);
		this.weeklyPunchKingRewardedGold = Math.max(0, rewardedGold);
		this.weeklyPunchKingRewardedSkillPoints = Math.max(0, rewardedSkillPoints);
		touch();
	}

	public void spendBossRaidTicket() {
		if (bossRaidTicketCount < 1) {
			throw new IllegalStateException("보스 입장권이 없어요.");
		}
		this.bossRaidTicketCount -= 1;
		touch();
	}

	public void resetDungeonReentryCooldownForTest() {
		this.dungeonNextAvailableAt = null;
		touch();
	}

	public void resetDungeonDailyLimitForTest(LocalDate today) {
		this.dungeonRunCountDate = today;
		this.dungeonRunCount = 0;
		this.dungeonCouponHuntMillis = 0;
		this.dungeonNextAvailableAt = null;
		this.dungeonExploreAvailableNotificationDate = null;
		this.dungeonExploreAvailableNotificationRunCount = null;
		this.dungeonExploreAvailableNotificationSentAt = null;
		touch();
	}

	public void resetRookieEventForTest(Instant now, LocalDate today) {
		this.rookieEventStartedAt = now;
		this.rookieEventCompletedAt = null;
		this.rookieEventRewardClaimedAt = null;
		this.rookieEventCompletedDays = 0;
		this.rookieEventRewardedDays = 0;
		this.rookieEventLastCompletedDate = null;
		this.rookieEventMissionMessageSentDate = null;
		this.rookieEventMissionMessageSentDay = 0;
		resetRookieEventDailyProgress(today);
		touch();
	}

	public void overrideRookieEventForTest(
			Instant now,
			LocalDate today,
			int completedDays,
			int rewardedDays,
			boolean rewardClaimed,
			int maxEventDays
	) {
		int safeCompletedDays = Math.max(0, Math.min(maxEventDays, completedDays));
		int safeRewardedDays = Math.max(0, Math.min(safeCompletedDays, rewardedDays));
		this.rookieEventStartedAt = rookieEventStartedAt == null ? now : rookieEventStartedAt;
		this.rookieEventCompletedDays = safeCompletedDays;
		this.rookieEventRewardedDays = safeRewardedDays;
		this.rookieEventCompletedAt = safeCompletedDays >= maxEventDays ? now : null;
		this.rookieEventRewardClaimedAt = rewardClaimed && safeCompletedDays >= maxEventDays ? now : null;
		this.rookieEventLastCompletedDate = safeCompletedDays > 0 && safeCompletedDays < maxEventDays ? today : null;
		resetRookieEventDailyProgress(today);
		touch();
	}

	public void advanceRookieEventDayForTest(Instant now, LocalDate today) {
		if (rookieEventStartedAt == null) {
			resetRookieEventForTest(now, today);
		}
		this.rookieEventStartedAt = oneDayEarlier(rookieEventStartedAt);
		this.rookieEventCompletedAt = oneDayEarlier(rookieEventCompletedAt);
		this.rookieEventRewardClaimedAt = oneDayEarlier(rookieEventRewardClaimedAt);
		if (today != null) {
			LocalDate previousDay = today.minusDays(1);
			this.rookieEventCurrentDate = previousDay;
			if (today.equals(rookieEventLastCompletedDate)) {
				this.rookieEventLastCompletedDate = previousDay;
			}
		}
		touch();
	}

	public void spendSkillPoint() {
		spendSkillPoints(1);
	}

	public void spendSkillPoints(int amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("Skill point amount must be positive.");
		}
		if (skillPoints < amount) {
			throw new IllegalStateException("Not enough skill points.");
		}
		this.skillPoints -= amount;
		touch();
	}

    public int getNextLevelExperience() {
		return nextLevelExperience(level);
	}

    public void damageMonster(int damage) {
		this.currentMonsterHp = Math.max(0, currentMonsterHp - damage);
		touch();
	}

	public void replaceMonster(String monsterKey, int maxHp) {
		this.currentMonsterKey = monsterKey;
		this.currentMonsterHp = maxHp;
		touch();
	}

	public void recordMonsterDefeat(long experienceGained) {
		this.defeatedMonsters += 1;
		this.experience += experienceGained;
		while (experience >= nextLevelExperience(level)) {
			this.experience -= nextLevelExperience(level);
			this.level += 1;
			this.skillPoints += 1;
		}
		touch();
	}

	public void levelUpForTest() {
		this.level += 1;
		this.skillPoints += 1;
		this.experience = Math.min(this.experience, nextLevelExperience(this.level) - 1);
		touch();
	}

	public void levelDownForTest() {
		if (this.level <= 1) {
			this.level = 1;
			this.experience = 0;
			touch();
			return;
		}
		this.level -= 1;
		this.experience = Math.min(this.experience, nextLevelExperience(this.level) - 1);
		touch();
	}

    public void setAutoHuntEndsAt(Instant autoHuntEndsAt) {
		this.autoHuntEndsAt = autoHuntEndsAt;
		touch();
	}

	public void markAutoHuntAdClaimed(Instant claimedAt) {
		this.lastAutoHuntAdClaimedAt = claimedAt;
		touch();
	}

	public void clearAutoHuntEndNotification() {
		this.autoHuntEndNotifiedAt = null;
		this.autoHuntEndSmartMessageAttemptedAt = null;
		this.autoHuntEndSettledGold = null;
		this.autoHuntEndLevelGain = 0;
		this.autoHuntEndSkillPointGain = 0;
		this.autoHuntEndCombatPowerGain = 0;
		touch();
	}

	public void markAutoHuntEndNotified(Instant notifiedAt) {
		this.autoHuntEndNotifiedAt = notifiedAt;
		touch();
	}

	public void markAutoHuntEndSmartMessageAttempted(Instant attemptedAt) {
		this.autoHuntEndSmartMessageAttemptedAt = attemptedAt;
		touch();
	}

	public void addAutoHuntEndSettledGold(long settledGold) {
		addAutoHuntEndSettlementSummary(settledGold, 0, 0, 0);
	}

	public void addAutoHuntEndSettlementSummary(long settledGold, int levelGain, int skillPointGain, long combatPowerGain) {
		if (settledGold < 1 && levelGain < 1 && skillPointGain < 1 && combatPowerGain < 1) {
			return;
		}
		if (settledGold > 0) {
			this.autoHuntEndSettledGold = Math.max(0, this.autoHuntEndSettledGold == null ? 0 : this.autoHuntEndSettledGold)
					+ settledGold;
		}
		this.autoHuntEndLevelGain += Math.max(0, levelGain);
		this.autoHuntEndSkillPointGain += Math.max(0, skillPointGain);
		this.autoHuntEndCombatPowerGain += Math.max(0, combatPowerGain);
		touch();
	}

	public boolean hasClaimedTutorialReward() {
		return tutorialRewardClaimedAt != null;
	}

	public void claimTutorialReward(Instant claimedAt) {
		this.tutorialRewardClaimedAt = claimedAt;
		touch();
	}

	public boolean hasCompletedFeatureTutorial() {
		return featureTutorialCompletedAt != null;
	}

	public void completeFeatureTutorial(Instant completedAt) {
		this.featureTutorialCompletedAt = completedAt;
		touch();
	}

	public boolean isSuspended() {
		return suspendedAt != null;
	}

	public void suspend(String reason, Instant suspendedAt) {
		this.suspendedAt = suspendedAt;
		this.suspensionReason = reason;
		touch();
	}

	public void resume() {
		this.suspendedAt = null;
		this.suspensionReason = null;
		touch();
	}

	public void updateGameProfile(String nickname, Instant updatedAt) {
		String normalized = nickname == null ? "" : nickname.trim();
		this.gameProfileNickname = normalized.isBlank() ? null : normalized;
		this.gameProfileUpdatedAt = this.gameProfileNickname == null ? null : updatedAt;
		touch();
	}

	public void updateAdminNickname(String nickname) {
		String normalized = nickname == null ? "" : nickname.trim();
		this.adminNickname = normalized.isBlank() ? null : normalized;
		touch();
	}

	public void markAccessed(Instant accessedAt) {
		this.lastAccessedAt = accessedAt;
	}

	public void setAdminFavorite(boolean adminFavorite) {
		this.adminFavorite = adminFavorite;
		touch();
	}

	public List<String> ownedPetSkinKeyList() {
		if (ownedPetSkinKeys == null || ownedPetSkinKeys.isBlank()) {
			return List.of();
		}
		return Arrays.stream(ownedPetSkinKeys.split(","))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.distinct()
				.toList();
	}

	public boolean ownsPetSkin(String skinKey) {
		return ownedPetSkinKeyList().contains(normalizePetSkinKey(skinKey));
	}

	public void purchasePetSkin(String skinKey, long priceGold) {
		String normalized = normalizePetSkinKey(skinKey);
		if (ownsPetSkin(normalized)) {
			throw new IllegalStateException("이미 보유한 펫 스킨이에요.");
		}
		spendGold(priceGold);
		addOwnedPetSkin(normalized);
	}

	public void unlockPetSkin(String skinKey) {
		String normalized = normalizePetSkinKey(skinKey);
		if (!ownsPetSkin(normalized)) {
			addOwnedPetSkin(normalized);
		}
	}

	public void equipPetSkin(int slot, String skinKey) {
		String normalized = normalizePetSkinKey(skinKey);
		if (!ownsPetSkin(normalized)) {
			throw new IllegalStateException("보유하지 않은 펫 스킨이에요.");
		}
		if (slot == 1) {
			if (characterSlots < 2) {
				throw new IllegalStateException("첫 번째 동료 펫을 먼저 데려와야 해요.");
			}
			this.petOneSkinKey = normalized;
		} else if (slot == 2) {
			if (characterSlots < 3) {
				throw new IllegalStateException("두 번째 동료 펫을 먼저 데려와야 해요.");
			}
			this.petTwoSkinKey = normalized;
		} else {
			throw new IllegalArgumentException("펫 슬롯은 1 또는 2만 사용할 수 있어요.");
		}
		touch();
	}

	public void unlockEasterEggPetSkins(Set<String> skinKeys) {
		for (String skinKey : skinKeys) {
			unlockPetSkin(skinKey);
		}
	}

    public void setLastSettledAt(Instant lastSettledAt) {
		this.lastSettledAt = lastSettledAt;
		touch();
	}

    public PlayerSkill getOrCreateSkill(SkillType type) {
		return skills.stream()
				.filter(skill -> skill.getType() == type)
				.findFirst()
				.orElseGet(() -> {
					PlayerSkill skill = new PlayerSkill(this, type);
					skills.add(skill);
					return skill;
				});
	}

    public boolean hasChosenJob() {
		return job != null;
	}

	public void resetForTest(Instant now) {
		this.job = null;
		this.characterSlots = 1;
		this.gold = 0;
		this.cumulativeGoldEarned = 0;
		this.skillPoints = 0;
		this.friendInviteRewardCount = 0;
		this.level = 1;
		this.experience = 0;
		this.currentMonsterKey = "BOSS_ROCK";
		this.currentMonsterHp = 120;
		this.defeatedMonsters = 0;
		this.hitGoldRemainderMicros = 0;
		this.defeatGoldRemainderMicros = 0;
		this.autoHuntEndsAt = null;
		this.autoHuntEndNotifiedAt = null;
		this.autoHuntEndSmartMessageAttemptedAt = null;
		this.autoHuntEndSettledGold = null;
		this.autoHuntEndLevelGain = 0;
		this.autoHuntEndSkillPointGain = 0;
		this.autoHuntEndCombatPowerGain = 0;
		this.lastAutoHuntAdClaimedAt = null;
		this.lastSkillPointAdClaimedAt = null;
		this.tutorialRewardClaimedAt = null;
		this.featureTutorialCompletedAt = null;
		this.gameProfileNickname = null;
		this.gameProfileUpdatedAt = null;
		this.adminFavorite = false;
		this.benefitTabNewUserEnteredAt = null;
		this.benefitTabNewUserPromotionExecutionKey = null;
		this.benefitTabNewUserPromotionResultCheckedAt = null;
		this.benefitTabNewUserPromotionGrantedAt = null;
		this.ownedPetSkinKeys = "FIRE_FOX,ICE";
		this.petOneSkinKey = "FIRE_FOX";
		this.petTwoSkinKey = "ICE";
		this.rookieEventStartedAt = null;
		this.rookieEventCompletedAt = null;
		this.rookieEventRewardClaimedAt = null;
		this.rookieEventCompletedDays = 0;
		this.rookieEventRewardedDays = 0;
		this.rookieEventCurrentDate = null;
		this.rookieEventLastCompletedDate = null;
		this.rookieEventDailyHuntMillis = 0;
		this.rookieEventDailyMonsters = 0;
		this.rookieEventDailyGold = 0;
		this.rookieEventDailySettlements = 0;
		this.rookieEventDailySkillPointsSpent = 0;
		this.rookieEventDailySkillPointHelpClaimed = false;
		this.rookieEventDailyHomeShortcutReturned = false;
		this.rookieEventMissionNotificationAgreedAt = null;
		this.rookieEventMissionMessageSentDate = null;
		this.rookieEventMissionMessageSentDay = 0;
			this.dormantSpRewardStreakAccessedAt = null;
			this.dormantSpRewardSentStage = 0;
			this.dormantSpRewardLastSentAt = null;
		this.dungeonExploreAvailableNotificationDate = null;
		this.dungeonExploreAvailableNotificationRunCount = null;
		this.dungeonExploreAvailableNotificationSentAt = null;
		this.battleReadyDailyStreakAccessedAt = null;
		this.battleReadyDailySentStage = 0;
		this.battleReadyDailyLastSentAt = null;
		this.dungeonCouponCount = 0;
		this.dungeonCouponHuntMillis = 0;
			this.dungeonRunCountDate = null;
			this.dungeonRunCount = 0;
			this.dungeonNextAvailableAt = null;
			this.bossRaidTicketCount = 0;
			this.dailyMissionCycle = 1;
			this.dailyMissionCurrentDate = null;
			this.dailyMissionLastCompletedDate = null;
			this.dailyMissionCompletedDays = 0;
			this.dailyMissionDailyHuntMillis = 0;
			this.dailyMissionDailyDungeonRuns = 0;
			this.vipExpiresAt = null;
			this.vipLastDailyRewardDate = null;
			this.adventureMiniGameEntryStartedAt = null;
			this.adventureMiniGameCompletedDate = null;
			this.weeklyPunchKingWeekStartDate = null;
			this.weeklyPunchKingBestScore = 0;
			this.weeklyPunchKingRewardedGold = 0;
			this.weeklyPunchKingRewardedSkillPoints = 0;
			this.lastSettledAt = now;
			this.lastAccessedAt = now;
			this.skills.forEach(PlayerSkill::resetLevel);
			touch();
		}

	private void addOwnedPetSkin(String skinKey) {
		LinkedHashSet<String> keys = new LinkedHashSet<>(ownedPetSkinKeyList());
		keys.add(normalizePetSkinKey(skinKey));
		this.ownedPetSkinKeys = String.join(",", keys);
		touch();
	}

	private String normalizePetSkinKey(String skinKey) {
		String normalized = skinKey == null ? "" : skinKey.trim().toUpperCase();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("펫 스킨을 선택해 주세요.");
		}
		return normalized;
	}

	private int nextLevelExperience(int level) {
		return 1000 + level * level * 500;
	}

	private void resetRookieEventDailyProgress(LocalDate today) {
		this.rookieEventCurrentDate = today;
		this.rookieEventDailyHuntMillis = 0;
		this.rookieEventDailyMonsters = 0;
		this.rookieEventDailyGold = 0;
		this.rookieEventDailySettlements = 0;
		this.rookieEventDailySkillPointsSpent = 0;
		this.rookieEventDailySkillPointHelpClaimed = false;
		this.rookieEventDailyHomeShortcutReturned = false;
		touch();
	}

	private Instant oneDayEarlier(Instant instant) {
		return instant == null ? null : instant.minusSeconds(86_400);
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}
}

package com.money_hunter.domain;

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

	private LocalDate friendInviteRewardDate;

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

	private Instant boostEndsAt;

	private Instant lastAutoHuntAdClaimedAt;

	private Instant lastBoostAdClaimedAt;

	private Instant lastSkillPointAdClaimedAt;

	private Instant tutorialRewardClaimedAt;

	private Instant featureTutorialCompletedAt;

	private Instant suspendedAt;

	@Column(length = 500)
	private String suspensionReason;

	@Column(length = 80)
	private String gameProfileNickname;

	private Instant gameProfileUpdatedAt;

	@Column(nullable = false)
	private boolean adminFavorite = false;

	@Column(nullable = false, length = 600)
	private String ownedPetSkinKeys = "FIRE_FOX,ICE";

	@Column(nullable = false, length = 40)
	private String petOneSkinKey = "FIRE_FOX";

	@Column(nullable = false, length = 40)
	private String petTwoSkinKey = "ICE";

    @Column(nullable = false)
	private Instant lastSettledAt;

    @Column(nullable = false)
	private Instant createdAt;

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

	public void claimFriendInviteReward(int inviteLimit, int rewardSkillPoints, LocalDate today) {
		claimFriendInviteReward(inviteLimit, rewardSkillPoints, 1, today);
	}

	public int claimFriendInviteReward(int inviteLimit, int rewardSkillPoints, int completedInvites, LocalDate today) {
		resetFriendInviteRewardIfNewDay(today);
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

	public void resetFriendInviteRewardIfNewDay(LocalDate today) {
		if (today == null) {
			return;
		}
		if (!today.equals(friendInviteRewardDate)) {
			this.friendInviteRewardDate = today;
			this.friendInviteRewardCount = 0;
			touch();
		}
	}

	public void spendSkillPoint() {
		if (skillPoints < 1) {
			throw new IllegalStateException("Not enough skill points.");
		}
		this.skillPoints -= 1;
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
		touch();
	}

	public void markAutoHuntEndNotified(Instant notifiedAt) {
		this.autoHuntEndNotifiedAt = notifiedAt;
		touch();
	}

    public void setBoostEndsAt(Instant boostEndsAt) {
		this.boostEndsAt = boostEndsAt;
		touch();
	}

	public void markBoostAdClaimed(Instant claimedAt) {
		this.lastBoostAdClaimedAt = claimedAt;
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
		this.friendInviteRewardDate = null;
		this.level = 1;
		this.experience = 0;
		this.currentMonsterKey = "BOSS_ROCK";
		this.currentMonsterHp = 120;
		this.defeatedMonsters = 0;
		this.hitGoldRemainderMicros = 0;
		this.defeatGoldRemainderMicros = 0;
		this.autoHuntEndsAt = null;
		this.autoHuntEndNotifiedAt = null;
		this.boostEndsAt = null;
		this.lastAutoHuntAdClaimedAt = null;
		this.lastBoostAdClaimedAt = null;
		this.lastSkillPointAdClaimedAt = null;
		this.tutorialRewardClaimedAt = null;
		this.featureTutorialCompletedAt = null;
		this.gameProfileNickname = null;
		this.gameProfileUpdatedAt = null;
		this.adminFavorite = false;
		this.ownedPetSkinKeys = "FIRE_FOX,ICE";
		this.petOneSkinKey = "FIRE_FOX";
		this.petTwoSkinKey = "ICE";
		this.lastSettledAt = now;
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

	private void touch() {
		this.updatedAt = Instant.now();
	}
}
